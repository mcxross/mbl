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
package xyz.mcxross.mbl.utils

import kotlinx.io.Buffer

object ULEB128 {
    fun write(buffer: Buffer, value: Int) {
        var v = value
        do {
            var byte = (v and 0x7F).toByte()
            v = v ushr 7
            if (v != 0) {
                byte = (byte.toInt() or 0x80).toByte()
            }
            buffer.writeByte(byte)
        } while (v != 0)
    }

    fun write(buffer: Buffer, value: Long) {
        var v = value
        do {
            var byte = (v and 0x7F).toByte()
            v = v ushr 7
            if (v != 0L) {
                byte = (byte.toInt() or 0x80).toByte()
            }
            buffer.writeByte(byte)
        } while (v != 0L)
    }
}