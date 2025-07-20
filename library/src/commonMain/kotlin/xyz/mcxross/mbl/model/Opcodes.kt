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

object Opcodes {
    const val POP: UByte = 0x01u
    const val RET: UByte = 0x02u
    const val BR_TRUE: UByte = 0x03u
    const val BR_FALSE: UByte = 0x04u
    const val BRANCH: UByte = 0x05u
    const val LD_U64: UByte = 0x06u
    const val LD_CONST: UByte = 0x07u
    const val LD_TRUE: UByte = 0x08u
    const val LD_FALSE: UByte = 0x09u
    const val COPY_LOC: UByte = 0x0Au
    const val MOVE_LOC: UByte = 0x0Bu
    const val ST_LOC: UByte = 0x0Cu
    const val MUT_BORROW_LOC: UByte = 0x0Du
    const val IMM_BORROW_LOC: UByte = 0x0Eu
    const val MUT_BORROW_FIELD: UByte = 0x0Fu
    const val IMM_BORROW_FIELD: UByte = 0x10u
    const val CALL: UByte = 0x11u
    const val PACK: UByte = 0x12u
    const val UNPACK: UByte = 0x13u
    const val READ_REF: UByte = 0x14u
    const val WRITE_REF: UByte = 0x15u
    const val ADD: UByte = 0x16u
    const val SUB: UByte = 0x17u
    const val MUL: UByte = 0x18u
    const val MOD: UByte = 0x19u
    const val DIV: UByte = 0x1Au
    const val BIT_OR: UByte = 0x1Bu
    const val BIT_AND: UByte = 0x1Cu
    const val XOR: UByte = 0x1Du
    const val OR: UByte = 0x1Eu
    const val AND: UByte = 0x1Fu
    const val NOT: UByte = 0x20u
    const val EQ: UByte = 0x21u
    const val NEQ: UByte = 0x22u
    const val LT: UByte = 0x23u
    const val GT: UByte = 0x24u
    const val LE: UByte = 0x25u
    const val GE: UByte = 0x26u
    const val ABORT: UByte = 0x27u
    const val NOP: UByte = 0x28u
    const val EXISTS_DEPRECATED: UByte = 0x29u
    const val MUT_BORROW_GLOBAL_DEPRECATED: UByte = 0x2Au
    const val IMM_BORROW_GLOBAL_DEPRECATED: UByte = 0x2Bu
    const val MOVE_FROM_DEPRECATED: UByte = 0x2Cu
    const val MOVE_TO_DEPRECATED: UByte = 0x2Du
    const val FREEZE_REF: UByte = 0x2Eu
    const val SHL: UByte = 0x2Fu
    const val SHR: UByte = 0x30u
    const val LD_U8: UByte = 0x31u
    const val LD_U128: UByte = 0x32u
    const val CAST_U8: UByte = 0x33u
    const val CAST_U64: UByte = 0x34u
    const val CAST_U128: UByte = 0x35u
    const val MUT_BORROW_FIELD_GENERIC: UByte = 0x36u
    const val IMM_BORROW_FIELD_GENERIC: UByte = 0x37u
    const val CALL_GENERIC: UByte = 0x38u
    const val PACK_GENERIC: UByte = 0x39u
    const val UNPACK_GENERIC: UByte = 0x3Au
    const val EXISTS_GENERIC_DEPRECATED: UByte = 0x3Bu
    const val MUT_BORROW_GLOBAL_GENERIC_DEPRECated: UByte = 0x3Cu
    const val IMM_BORROW_GLOBAL_GENERIC_DEPRECATED: UByte = 0x3Du
    const val MOVE_FROM_GENERIC_DEPRECATED: UByte = 0x3Eu
    const val MOVE_TO_GENERIC_DEPRECATED: UByte = 0x3Fu
    const val VEC_PACK: UByte = 0x40u
    const val VEC_LEN: UByte = 0x41u
    const val VEC_IMM_BORROW: UByte = 0x42u
    const val VEC_MUT_BORROW: UByte = 0x43u
    const val VEC_PUSH_BACK: UByte = 0x44u
    const val VEC_POP_BACK: UByte = 0x45u
    const val VEC_UNPACK: UByte = 0x46u
    const val VEC_SWAP: UByte = 0x47u
    const val LD_U16: UByte = 0x48u
    const val LD_U32: UByte = 0x49u
    const val LD_U256: UByte = 0x4Au
    const val CAST_U16: UByte = 0x4Bu
    const val CAST_U32: UByte = 0x4Cu
    const val CAST_U256: UByte = 0x4Du
    const val PACK_VARIANT: UByte = 0x4Eu
    const val PACK_VARIANT_GENERIC: UByte = 0x4Fu
    const val UNPACK_VARIANT: UByte = 0x50u
    const val UNPACK_VARIANT_IMM_REF: UByte = 0x51u
    const val UNPACK_VARIANT_MUT_REF: UByte = 0x52u
    const val UNPACK_VARIANT_GENERIC: UByte = 0x53u
    const val UNPACK_VARIANT_GENERIC_IMM_REF: UByte = 0x54u
    const val UNPACK_VARIANT_GENERIC_MUT_REF: UByte = 0x55u
    const val VARIANT_SWITCH: UByte = 0x56u
}