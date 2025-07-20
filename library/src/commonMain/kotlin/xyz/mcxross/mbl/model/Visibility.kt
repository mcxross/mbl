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

enum class Visibility(val value: UByte) {
    Private(0x0u),
    Public(0x1u),
    Friend(0x3u);

    companion object {
        @Deprecated(
            "Replaced by the 'entry' modifier on functions.",
            level = DeprecationLevel.WARNING
        )
        const val DEPRECATED_SCRIPT: UByte = 0x2u

        fun fromUByte(value: UByte): Visibility? {
            // Replaced by `entries.find { it.value == value }` in Kotlin 1.9+
            return entries.find { it.value == value }
        }
    }
}