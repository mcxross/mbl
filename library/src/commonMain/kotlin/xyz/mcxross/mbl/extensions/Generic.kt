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
package xyz.mcxross.mbl.extensions

import kotlinx.io.Source

fun Source.readUleb128AsU64(): ULong {
    var result = 0UL
    var shift = 0

    while (true) {
        if (shift >= 70) {
            throw IllegalArgumentException("Invalid ULEB128 sequence: exceeds 64-bit range")
        }

        val byte = this.readByte().toULong()
        val segment = byte and 0x7FU
        result = result or (segment shl shift)

        if ((byte and 0x80U) == 0UL) {
            return result
        }

        shift += 7
    }
}

fun UInt.checkedAdd(other: UInt): UInt? =
    if (this + other < this) null else this + other