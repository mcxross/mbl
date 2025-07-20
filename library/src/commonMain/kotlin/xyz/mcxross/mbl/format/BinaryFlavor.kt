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

object BinaryFlavor {
    private const val VERSION_6 = 6

    const val FLAVOR_MASK: Int = 0xFF000000.toInt()
    const val VERSION_MASK: Int = 0x00FFFFFF
    const val SUI_FLAVOR: Byte = 0x05
    private const val SHIFT_AMOUNT: Int = 24

    fun encodeVersion(unflavoredVersion: Int): Int {
        if (unflavoredVersion <= VERSION_6) {
            return unflavoredVersion
        }
        return shiftAndFlavor(unflavoredVersion)
    }

    fun decodeVersion(flavoredVersion: Int): Int {
        if (flavoredVersion <= VERSION_6) {
            return flavoredVersion
        }
        return flavoredVersion and VERSION_MASK
    }

    fun decodeFlavor(flavoredVersion: Int): Byte? {
        if (flavoredVersion <= VERSION_6) {
            return null
        }
        return maskAndShiftToUnflavor(flavoredVersion)
    }

    private fun maskAndShiftToUnflavor(flavored: Int): Byte {
        return ((flavored and FLAVOR_MASK) ushr SHIFT_AMOUNT).toByte()
    }

    private fun shiftAndFlavor(unflavored: Int): Int {
        return (SUI_FLAVOR.toInt() shl SHIFT_AMOUNT) or unflavored
    }
}