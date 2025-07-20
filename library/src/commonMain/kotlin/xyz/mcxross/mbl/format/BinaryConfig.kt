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

import xyz.mcxross.mbl.utils.VERSION_1
import xyz.mcxross.mbl.utils.VERSION_MAX

data class TableConfig(
    val moduleHandles: UShort,
    val datatypeHandles: UShort,
    val functionHandles: UShort,
    val functionInstantiations: UShort,
    val signatures: UShort,
    val constantPool: UShort,
    val identifiers: UShort,
    val addressIdentifiers: UShort,
    val structDefs: UShort,
    val structDefInstantiations: UShort,
    val functionDefs: UShort,
    val fieldHandles: UShort,
    val fieldInstantiations: UShort,
    val friendDecls: UShort,
    val enumDefs: UShort,
    val enumDefInstantiations: UShort,
    val variantHandles: UShort,
    val variantInstantiationHandles: UShort
) {
    companion object {
        fun legacy(): TableConfig {
            val max = UShort.MAX_VALUE
            return TableConfig(
                moduleHandles = max,
                datatypeHandles = max,
                functionHandles = max,
                functionInstantiations = max,
                signatures = max,
                constantPool = max,
                identifiers = max,
                addressIdentifiers = max,
                structDefs = max,
                structDefInstantiations = max,
                functionDefs = max,
                fieldHandles = max,
                fieldInstantiations = max,
                friendDecls = max,
                enumDefs = max,
                enumDefInstantiations = max,
                variantHandles = 1024u,
                variantInstantiationHandles = 1024u
            )
        }
    }
}

data class BinaryConfig(
    val maxBinaryFormatVersion: UInt,
    val minBinaryFormatVersion: UInt,
    val checkNoExtraneousBytes: Boolean,
    val tableConfig: TableConfig,
    private val allowUnpublishable: Boolean = false
) {
    companion object {
        fun new(
            maxBinaryFormatVersion: UInt,
            minBinaryFormatVersion: UInt,
            checkNoExtraneousBytes: Boolean,
            tableConfig: TableConfig
        ): BinaryConfig {
            return BinaryConfig(
                maxBinaryFormatVersion,
                minBinaryFormatVersion,
                checkNoExtraneousBytes,
                tableConfig,
                allowUnpublishable = false
            )
        }

        fun legacy(
            maxBinaryFormatVersion: UInt,
            minBinaryFormatVersion: UInt,
            checkNoExtraneousBytes: Boolean
        ): BinaryConfig {
            return new(
                maxBinaryFormatVersion,
                minBinaryFormatVersion,
                checkNoExtraneousBytes,
                TableConfig.legacy()
            )
        }

        fun withExtraneousBytesCheck(checkNoExtraneousBytes: Boolean): BinaryConfig {
            return legacy(VERSION_MAX, VERSION_1, checkNoExtraneousBytes)
        }

        fun standard(): BinaryConfig {
            return withExtraneousBytesCheck(true)
        }

        fun newUnpublishable(): BinaryConfig {
            return BinaryConfig(
                maxBinaryFormatVersion = VERSION_MAX,
                minBinaryFormatVersion = VERSION_1,
                checkNoExtraneousBytes = true,
                tableConfig = TableConfig.legacy(),
                allowUnpublishable = true
            )
        }
    }

    fun allowUnpublishable(): Boolean {
        return allowUnpublishable
    }
}