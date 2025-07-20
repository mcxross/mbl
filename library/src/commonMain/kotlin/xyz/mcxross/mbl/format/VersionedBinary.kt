/*
 * Copyright 2025 McXross
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package xyz.mcxross.mbl.format

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.runCatching
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlinx.io.readIntLe
import xyz.mcxross.mbl.model.ModuleHandleIndex
import xyz.mcxross.mbl.utils.TABLE_COUNT_MAX
import xyz.mcxross.mbl.utils.TABLE_OFFSET_MAX
import xyz.mcxross.mbl.utils.TABLE_SIZE_MAX
import xyz.mcxross.mbl.utils.VERSION_7
import xyz.mcxross.mbl.utils.VERSION_MAX
import xyz.mcxross.mbl.model.MagicError
import xyz.mcxross.mbl.model.MagicKind
import xyz.mcxross.mbl.model.StatusCode
import xyz.mcxross.mbl.model.Table
import xyz.mcxross.mbl.model.TableType
import xyz.mcxross.mbl.exception.PartialVMError
import xyz.mcxross.mbl.extensions.checkedAdd
import xyz.mcxross.mbl.extensions.readUleb128AsU64
import kotlin.math.min

fun checkTable(tables: MutableList<Table>, binaryLength: Int): Result<UInt, PartialVMError> {
    tables.sortBy { it.offset }

    var currentOffset = 0u
    val tableTypes = mutableSetOf<TableType>()

    for (table in tables) {
        when {
            table.offset != currentOffset ->
                return Err(PartialVMError.new(StatusCode.BAD_HEADER_TABLE))

            table.count == 0u ->
                return Err(PartialVMError.new(StatusCode.BAD_HEADER_TABLE))

            !tableTypes.add(table.kind) ->
                return Err(PartialVMError.new(StatusCode.DUPLICATE_TABLE))

            currentOffset > binaryLength.toUInt() ->
                return Err(PartialVMError.new(StatusCode.BAD_HEADER_TABLE))
        }

        currentOffset = currentOffset.checkedAdd(table.count)
            ?: return Err(PartialVMError.new(StatusCode.BAD_HEADER_TABLE))
    }

    return Ok(currentOffset)
}

internal class VersionedBinary private constructor(
    val binary: ByteArray,
    val version: Int,
    val publishable: Boolean,
    val tables: List<Table>,
    val moduleHandleIndex: ModuleHandleIndex,
    private val dataOffset: Int,
    val binaryEndOffset: Int
) {
    companion object {

        @OptIn(ExperimentalUnsignedTypes::class)
        fun initialize(
            data: ByteArray,
            config: BinaryConfig = BinaryConfig.withExtraneousBytesCheck(false),
        ): Result<VersionedBinary, PartialVMError> {

            val buffer = Buffer().apply { write(data) }
            val initialSize = buffer.size

            val magic = runCatching {
                buffer.readByteArray(BinaryConstants.MOVE_MAGIC_SIZE)
            }.getOrElse { throw it }

            val publishable =
                when (BinaryConstants.decodeMagic(magic.toUByteArray(), BinaryConstants.MOVE_MAGIC_SIZE)) {
                    Ok(MagicKind.Normal) -> true

                    Ok(MagicKind.Unpublishable) -> {
                        if (config.allowUnpublishable()) false else return Err(
                            PartialVMError.new(StatusCode.BAD_MAGIC).withMessage("Binary header not allowed")
                        )
                    }

                    Err(MagicError.BadSize) -> return Err(
                        PartialVMError.new(StatusCode.BAD_MAGIC).withMessage("Binary header too short")
                    )

                    Err(MagicError.BadNumber) -> return Err(
                        PartialVMError.new(StatusCode.BAD_MAGIC).withMessage("Unexpected binary header")
                    )

                    else -> return Err(
                        PartialVMError.new(StatusCode.BAD_MAGIC).withMessage("Unexpected binary header")
                    )
                }

            val flavouredVersion = runCatching {
                buffer.readIntLe()
            }.getOrElse {
                return Err(PartialVMError.new(StatusCode.MALFORMED).withMessage("Bad binary header"))
            }

            val version = BinaryFlavor.decodeVersion(flavouredVersion)
            val flavor = BinaryFlavor.decodeFlavor(flavouredVersion)

            if (version < config.minBinaryFormatVersion.toInt()) return Err(PartialVMError.new(StatusCode.UNKNOWN_VERSION))

            if (version > min(
                    config.maxBinaryFormatVersion,
                    VERSION_MAX
                ).toInt()
            ) return Err(PartialVMError.new(StatusCode.MALFORMED))

            if (version >= VERSION_7.toInt() && flavor != BinaryFlavor.SUI_FLAVOR) {
                return Err(PartialVMError.new(StatusCode.UNKNOWN_VERSION))
            }

            val tableCount =
                loadTableCount(buffer).getOrElse {
                    return Err(PartialVMError.new(StatusCode.MALFORMED))
                }

            val tables = mutableListOf<Table>()

            readTables(buffer, tableCount.toUByte(), tables)

            val dataOffset = (initialSize - buffer.size).toUInt()

            val tableSize = checkTable(tables, data.size).getOrElse {
                return Err(PartialVMError.new(StatusCode.BAD_HEADER_TABLE))
            }

            if (tableSize + dataOffset > data.size.toUInt()) {
                return Err(PartialVMError.new(StatusCode.MALFORMED).withMessage("Table size too big"))
            }

            val moduleIndexOffset = dataOffset + tableSize
            val moduleIndexBuffer = Buffer().apply {
                write(data, startIndex = moduleIndexOffset.toInt())
            }

            val selfModuleHandleIndex = ModuleHandleIndex(moduleIndexBuffer.readUleb128AsU64().toUShort())
            val endOfBinary = moduleIndexOffset + moduleIndexBuffer.size.toUInt()

            return Ok(
                VersionedBinary(
                    binary = data,
                    version,
                    publishable,
                    tables,
                    moduleHandleIndex = selfModuleHandleIndex,
                    dataOffset = dataOffset.toInt(),
                    binaryEndOffset = endOfBinary.toInt()
                )
            )

        }
    }

    fun newCursorForTable(table: Table): Buffer {
        val start = this.dataOffset + table.offset.toInt()

        val end = start + table.count.toInt()

        val tableBytes = this.binary.copyOfRange(start, end)

        return Buffer().apply { write(tableBytes) }
    }

    override fun toString(): String {
        return buildString {
            appendLine("VersionedBinary(")
            appendLine("  version=$version,")
            appendLine("  publishable=${publishable}")
            appendLine("  moduleHandleIndex=$moduleHandleIndex,")
            appendLine("  dataOffset=$dataOffset,")
            appendLine("  binarySize=${binary.size},")

            appendLine("  tables=[")
            tables.forEach { table ->
                appendLine("    $table,")
            }
            appendLine("  ]")
            appendLine(")")
        }
    }

    fun deserializeIfValue() {

    }
}

internal fun readUleb(buffer: Buffer, max: ULong): Result<ULong, PartialVMError> {
    return Ok(buffer.readUleb128AsU64())
}

fun loadTableCount(buffer: Buffer): Result<ULong, PartialVMError> = readUleb(buffer, TABLE_COUNT_MAX)

fun readTable(buffer: Buffer): Result<Table, PartialVMError> = binding {
    val kind = buffer.readByte()

    val tableOffset = loadTableOffset(buffer).bind()
    val tableCount = loadTableSize(buffer).bind()
    val tableType = TableType.fromByte(kind).bind()

    Table(tableType, tableOffset.toUInt(), tableCount.toUInt())
}

fun loadTableOffset(buffer: Buffer): Result<ULong, PartialVMError> = readUleb(buffer, TABLE_OFFSET_MAX)

fun loadTableSize(buffer: Buffer): Result<ULong, PartialVMError> = readUleb(buffer, TABLE_SIZE_MAX)

fun readTables(source: Buffer, tableCount: UByte, tables: MutableList<Table>) {
    repeat(tableCount.toInt()) {
        val table = readTable(source).getOrElse { return@repeat }
        tables.add(table)
    }
}