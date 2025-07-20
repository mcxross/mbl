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
import xyz.mcxross.mbl.model.MagicError
import xyz.mcxross.mbl.model.MagicKind

@OptIn(ExperimentalUnsignedTypes::class)
object BinaryConstants {
    const val MOVE_MAGIC_SIZE: Int = 4
    val MOVE_MAGIC: UByteArray = ubyteArrayOf(0xA1u, 0x1Cu, 0xEBu, 0x0Bu)
    val UNPUBLISHABLE_MAGIC: UByteArray = ubyteArrayOf(0xDEu, 0xADu, 0xC0u, 0xDEu)
    const val HEADER_SIZE: Int = MOVE_MAGIC_SIZE + 5
    val TABLE_HEADER_SIZE: UByte = (4 * 2 + 1).toUByte()

    fun decodeMagic(magic: UByteArray, count: Int): Result<MagicKind, MagicError> {
        return when {
            count != MOVE_MAGIC_SIZE -> Err(MagicError.BadSize)
            magic.contentEquals(MOVE_MAGIC) -> Ok(MagicKind.Normal)
            magic.contentEquals(UNPUBLISHABLE_MAGIC) -> Ok(MagicKind.Unpublishable)
            else -> Err(MagicError.BadNumber)
        }
    }
}