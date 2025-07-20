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

const val TABLE_COUNT_MAX: ULong = 255u

const val TABLE_OFFSET_MAX: ULong = 0xffff_ffffu
const val TABLE_SIZE_MAX: ULong = 0xffff_ffffu
const val TABLE_CONTENT_SIZE_MAX: ULong = 0xffff_ffffu

const val TABLE_INDEX_MAX: ULong = 65535u
const val SIGNATURE_INDEX_MAX: ULong = TABLE_INDEX_MAX
const val ADDRESS_INDEX_MAX: ULong = TABLE_INDEX_MAX
const val IDENTIFIER_INDEX_MAX: ULong = TABLE_INDEX_MAX
const val MODULE_HANDLE_INDEX_MAX: ULong = TABLE_INDEX_MAX
const val DATATYPE_HANDLE_INDEX_MAX: ULong = TABLE_INDEX_MAX
const val STRUCT_DEF_INDEX_MAX: ULong = TABLE_INDEX_MAX
const val ENUM_DEF_INDEX_MAX: ULong = TABLE_INDEX_MAX
const val FUNCTION_HANDLE_INDEX_MAX: ULong = TABLE_INDEX_MAX
const val FUNCTION_INST_INDEX_MAX: ULong = TABLE_INDEX_MAX
const val FIELD_HANDLE_INDEX_MAX: ULong = TABLE_INDEX_MAX
const val FIELD_INST_INDEX_MAX: ULong = TABLE_INDEX_MAX
const val STRUCT_DEF_INST_INDEX_MAX: ULong = TABLE_INDEX_MAX
const val ENUM_DEF_INST_INDEX_MAX: ULong = TABLE_INDEX_MAX
const val CONSTANT_INDEX_MAX: ULong = TABLE_INDEX_MAX

const val BYTECODE_COUNT_MAX: ULong = 65535u
const val BYTECODE_INDEX_MAX: ULong = 65535u

const val LOCAL_INDEX_MAX: ULong = 255u

const val IDENTIFIER_SIZE_MAX: ULong = 65535u
const val CONSTANT_SIZE_MAX: ULong = 65535u

const val METADATA_KEY_SIZE_MAX: ULong = 1023u
const val METADATA_VALUE_SIZE_MAX: ULong = 65535u

const val SIGNATURE_SIZE_MAX: ULong = 255u

const val ACQUIRES_COUNT_MAX: ULong = 255u

const val FIELD_COUNT_MAX: ULong = 255u
const val FIELD_OFFSET_MAX: ULong = 255u

const val VARIANT_COUNT_MAX: ULong = 127u
val VARIANT_TAG_MAX_VALUE: ULong = VARIANT_COUNT_MAX - 1u

const val JUMP_TABLE_INDEX_MAX: ULong = VARIANT_COUNT_MAX

const val VARIANT_INSTANTIATION_HANDLE_INDEX_MAX: ULong = 1024u
const val VARIANT_HANDLE_INDEX_MAX: ULong = 1024u

const val TYPE_PARAMETER_COUNT_MAX: ULong = 255u
const val TYPE_PARAMETER_INDEX_MAX: ULong = 65536u

const val SIGNATURE_TOKEN_DEPTH_MAX: Int = 256

const val VERSION_1: UInt = 1u
const val VERSION_2: UInt = 2u
const val VERSION_3: UInt = 3u
const val VERSION_4: UInt = 4u
const val VERSION_5: UInt = 5u
const val VERSION_6: UInt = 6u
const val VERSION_7: UInt = 7u

const val VERSION_MAX: UInt = VERSION_7
const val VERSION_MIN: UInt = VERSION_5
