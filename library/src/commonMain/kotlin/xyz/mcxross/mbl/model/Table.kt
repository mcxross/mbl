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
package xyz.mcxross.mbl.model

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import xyz.mcxross.mbl.exception.PartialVMError

enum class TableType(val value: Int) {
    MODULE_HANDLES(0x1),
    DATATYPE_HANDLES(0x2),
    FUNCTION_HANDLES(0x3),
    FUNCTION_INST(0x4),
    SIGNATURES(0x5),
    CONSTANT_POOL(0x6),
    IDENTIFIERS(0x7),
    ADDRESS_IDENTIFIERS(0x8),
    STRUCT_DEFS(0xA),
    STRUCT_DEF_INST(0xB),
    FUNCTION_DEFS(0xC),
    FIELD_HANDLE(0xD),
    FIELD_INST(0xE),
    FRIEND_DECLS(0xF),
    METADATA(0x10),
    ENUM_DEFS(0x11),
    ENUM_DEF_INST(0x12),
    VARIANT_HANDLES(0x13),
    VARIANT_INST_HANDLES(0x14);

    companion object {
        private val map = entries.associateBy(TableType::value)
        fun fromValue(value: Int): TableType? = map[value]
        fun fromByte(byte: Byte): Result<TableType, PartialVMError> = map[byte.toInt()]?.let {
            Ok(it)
        } ?: throw NoSuchElementException("No TableType with value $byte")
    }
}

data class Table(
    val kind: TableType,
    val offset: UInt,
    val count: UInt
) {
    companion object {
        fun new(kind: TableType, offset: UInt, count: UInt): Table {
            return Table(kind, offset, count)
        }
    }
}