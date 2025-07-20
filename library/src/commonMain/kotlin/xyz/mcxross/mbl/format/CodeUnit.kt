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

import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.Sign
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlinx.io.readUByte
import kotlinx.io.readUIntLe
import kotlinx.io.readULongLe
import kotlinx.io.readUShortLe
import xyz.mcxross.mbl.utils.VERSION_7
import xyz.mcxross.mbl.model.Bytecode
import xyz.mcxross.mbl.model.Opcodes
import xyz.mcxross.mbl.extensions.readUleb128AsU64

fun loadCodeUnit(buffer: Buffer, version: Int): CodeUnit {
    val locals = SignatureIndex(buffer.readUleb128AsU64().toUShort())
    val code = loadCode(buffer, version)
    val jumpTables = loadJumpTables(buffer, version)

    return CodeUnit(
        locals = locals,
        code = code,
        jumpTables = jumpTables,
    )
}

private fun loadCode(buffer: Buffer, version: Int): List<Bytecode> {
    val bytecodeCount = buffer.readUleb128AsU64()
    val code = mutableListOf<Bytecode>()

    while (code.size < bytecodeCount.toInt()) {
        val instruction = parseBytecodeInstruction(buffer, version)
        code.add(instruction)
    }
    return code
}

private fun loadJumpTables(buffer: Buffer, version: Int): List<VariantJumpTable> {
    if (version < VERSION_7.toInt()) {
        return emptyList()
    }

    val jumpTableCount = buffer.readUleb128AsU64()
    return (1..jumpTableCount.toInt()).map { parseJumpTable(buffer) }
}

private fun parseJumpTable(buffer: Buffer): VariantJumpTable {
    val headEnum = EnumDefinitionIndex(buffer.readUleb128AsU64().toUShort())
    val branchCount = buffer.readUleb128AsU64()
    buffer.readByte() // Skip the JumpTableInner::Full flag for now
    val branches = (1..branchCount.toInt()).map { buffer.readUleb128AsU64().toUShort() }
    return VariantJumpTable(headEnum, branches)
}

private fun parseBytecodeInstruction(buffer: Buffer, version: Int): Bytecode {
    when (val opcode = buffer.readUByte()) {
        Opcodes.POP -> return Bytecode.Pop
        Opcodes.RET -> return Bytecode.Ret
        Opcodes.BR_TRUE -> return Bytecode.BrTrue(buffer.readUleb128AsU64().toUShort())
        Opcodes.BR_FALSE -> return Bytecode.BrFalse(buffer.readUleb128AsU64().toUShort())
        Opcodes.BRANCH -> return Bytecode.Branch(buffer.readUleb128AsU64().toUShort())
        Opcodes.LD_U8 -> return Bytecode.LdU8(buffer.readUByte())
        Opcodes.LD_U16 -> return Bytecode.LdU16(buffer.readUShortLe())
        Opcodes.LD_U32 -> return Bytecode.LdU32(buffer.readUIntLe())
        Opcodes.LD_U64 -> return Bytecode.LdU64(buffer.readULongLe())
        Opcodes.LD_U128 -> return Bytecode.LdU128(
            BigInteger.fromByteArray(
                buffer.readByteArray(16).reversedArray(),
                Sign.POSITIVE
            )
        )

        Opcodes.LD_U256 -> return Bytecode.LdU256(
            BigInteger.fromByteArray(
                buffer.readByteArray(32).reversedArray(),
                Sign.POSITIVE
            )
        )

        Opcodes.LD_CONST -> return Bytecode.LdConst(ConstantPoolIndex(buffer.readUleb128AsU64().toUShort()))
        Opcodes.LD_TRUE -> return Bytecode.LdTrue
        Opcodes.LD_FALSE -> return Bytecode.LdFalse
        Opcodes.COPY_LOC -> return Bytecode.CopyLoc(buffer.readUleb128AsU64().toUByte())
        Opcodes.MOVE_LOC -> return Bytecode.MoveLoc(buffer.readUleb128AsU64().toUByte())
        Opcodes.ST_LOC -> return Bytecode.StLoc(buffer.readUleb128AsU64().toUByte())
        Opcodes.CALL -> return Bytecode.Call(FunctionHandleIndex(buffer.readUleb128AsU64().toUShort()))
        Opcodes.CALL_GENERIC -> return Bytecode.CallGeneric(
            FunctionInstantiationIndex(
                buffer.readUleb128AsU64().toUShort()
            )
        )

        Opcodes.PACK -> return Bytecode.Pack(StructDefinitionIndex(buffer.readUleb128AsU64().toUShort()))
        Opcodes.PACK_GENERIC -> return Bytecode.PackGeneric(
            StructDefInstantiationIndex(
                buffer.readUleb128AsU64().toUShort()
            )
        )

        Opcodes.UNPACK -> return Bytecode.Unpack(StructDefinitionIndex(buffer.readUleb128AsU64().toUShort()))
        Opcodes.UNPACK_GENERIC -> return Bytecode.UnpackGeneric(
            StructDefInstantiationIndex(
                buffer.readUleb128AsU64().toUShort()
            )
        )

        Opcodes.READ_REF -> return Bytecode.ReadRef
        Opcodes.WRITE_REF -> return Bytecode.WriteRef
        Opcodes.FREEZE_REF -> return Bytecode.FreezeRef
        Opcodes.MUT_BORROW_LOC -> return Bytecode.MutBorrowLoc(buffer.readUleb128AsU64().toUByte())
        Opcodes.IMM_BORROW_LOC -> return Bytecode.ImmBorrowLoc(buffer.readUleb128AsU64().toUByte())
        Opcodes.MUT_BORROW_FIELD -> return Bytecode.MutBorrowField(
            FieldHandleIndex(
                buffer.readUleb128AsU64().toUShort()
            )
        )

        Opcodes.MUT_BORROW_FIELD_GENERIC -> return Bytecode.MutBorrowFieldGeneric(
            FieldInstantiationIndex(
                buffer.readUleb128AsU64().toUShort()
            )
        )

        Opcodes.IMM_BORROW_FIELD -> return Bytecode.ImmBorrowField(
            FieldHandleIndex(
                buffer.readUleb128AsU64().toUShort()
            )
        )

        Opcodes.IMM_BORROW_FIELD_GENERIC -> return Bytecode.ImmBorrowFieldGeneric(
            FieldInstantiationIndex(
                buffer.readUleb128AsU64().toUShort()
            )
        )

        Opcodes.ADD -> return Bytecode.Add
        Opcodes.SUB -> return Bytecode.Sub
        Opcodes.MUL -> return Bytecode.Mul
        Opcodes.MOD -> return Bytecode.Mod
        Opcodes.DIV -> return Bytecode.Div
        Opcodes.BIT_OR -> return Bytecode.BitOr
        Opcodes.BIT_AND -> return Bytecode.BitAnd
        Opcodes.XOR -> return Bytecode.Xor
        Opcodes.SHL -> return Bytecode.Shl
        Opcodes.SHR -> return Bytecode.Shr
        Opcodes.OR -> return Bytecode.Or
        Opcodes.AND -> return Bytecode.And
        Opcodes.NOT -> return Bytecode.Not
        Opcodes.EQ -> return Bytecode.Eq
        Opcodes.NEQ -> return Bytecode.Neq
        Opcodes.LT -> return Bytecode.Lt
        Opcodes.GT -> return Bytecode.Gt
        Opcodes.LE -> return Bytecode.Le
        Opcodes.GE -> return Bytecode.Ge
        Opcodes.ABORT -> return Bytecode.Abort
        Opcodes.NOP -> return Bytecode.Nop
        Opcodes.CAST_U8 -> return Bytecode.CastU8
        Opcodes.CAST_U16 -> return Bytecode.CastU16
        Opcodes.CAST_U32 -> return Bytecode.CastU32
        Opcodes.CAST_U64 -> return Bytecode.CastU64
        Opcodes.CAST_U128 -> return Bytecode.CastU128
        Opcodes.CAST_U256 -> return Bytecode.CastU256
        Opcodes.VEC_PACK -> return Bytecode.VecPack(
            SignatureIndex(buffer.readUleb128AsU64().toUShort()),
            buffer.readUleb128AsU64()
        )

        Opcodes.VEC_LEN -> return Bytecode.VecLen(SignatureIndex(buffer.readUleb128AsU64().toUShort()))
        Opcodes.VEC_IMM_BORROW -> return Bytecode.VecImmBorrow(SignatureIndex(buffer.readUleb128AsU64().toUShort()))
        Opcodes.VEC_MUT_BORROW -> return Bytecode.VecMutBorrow(SignatureIndex(buffer.readUleb128AsU64().toUShort()))
        Opcodes.VEC_PUSH_BACK -> return Bytecode.VecPushBack(SignatureIndex(buffer.readUleb128AsU64().toUShort()))
        Opcodes.VEC_POP_BACK -> return Bytecode.VecPopBack(SignatureIndex(buffer.readUleb128AsU64().toUShort()))
        Opcodes.VEC_UNPACK -> return Bytecode.VecUnpack(
            SignatureIndex(buffer.readUleb128AsU64().toUShort()),
            buffer.readUleb128AsU64()
        )

        Opcodes.VEC_SWAP -> return Bytecode.VecSwap(SignatureIndex(buffer.readUleb128AsU64().toUShort()))
        Opcodes.PACK_VARIANT -> return Bytecode.PackVariant(VariantHandleIndex(buffer.readUleb128AsU64().toUShort()))
        Opcodes.PACK_VARIANT_GENERIC -> return Bytecode.PackVariantGeneric(
            VariantInstantiationHandleIndex(
                buffer.readUleb128AsU64().toUShort()
            )
        )

        Opcodes.UNPACK_VARIANT -> return Bytecode.UnpackVariant(
            VariantHandleIndex(
                buffer.readUleb128AsU64().toUShort()
            )
        )
        Opcodes.UNPACK_VARIANT_IMM_REF -> return Bytecode.UnpackVariantImmRef(
            VariantHandleIndex(
                buffer.readUleb128AsU64().toUShort()
            )
        )

        Opcodes.UNPACK_VARIANT_MUT_REF -> return Bytecode.UnpackVariantMutRef(
            VariantHandleIndex(
                buffer.readUleb128AsU64().toUShort()
            )
        )

        Opcodes.UNPACK_VARIANT_GENERIC -> return Bytecode.UnpackVariantGeneric(
            VariantInstantiationHandleIndex(
                buffer.readUleb128AsU64().toUShort()
            )
        )

        Opcodes.UNPACK_VARIANT_GENERIC_IMM_REF -> return Bytecode.UnpackVariantGenericImmRef(
            VariantInstantiationHandleIndex(buffer.readUleb128AsU64().toUShort())
        )

        Opcodes.UNPACK_VARIANT_GENERIC_MUT_REF -> return Bytecode.UnpackVariantGenericMutRef(
            VariantInstantiationHandleIndex(buffer.readUleb128AsU64().toUShort())
        )

        Opcodes.VARIANT_SWITCH -> return Bytecode.VariantSwitch(
            VariantJumpTableIndex(
                buffer.readUleb128AsU64().toUShort()
            )
        )
        // Deprecated Opcodes
        Opcodes.EXISTS_DEPRECATED -> return Bytecode.ExistsDeprecated(
            StructDefinitionIndex(
                buffer.readUleb128AsU64().toUShort()
            )
        )

        Opcodes.EXISTS_GENERIC_DEPRECATED -> return Bytecode.ExistsGenericDeprecated(
            StructDefInstantiationIndex(
                buffer.readUleb128AsU64().toUShort()
            )
        )

        Opcodes.MOVE_FROM_DEPRECATED -> return Bytecode.MoveFromDeprecated(
            StructDefinitionIndex(
                buffer.readUleb128AsU64().toUShort()
            )
        )

        Opcodes.MOVE_FROM_GENERIC_DEPRECATED -> return Bytecode.MoveFromGenericDeprecated(
            StructDefInstantiationIndex(
                buffer.readUleb128AsU64().toUShort()
            )
        )

        Opcodes.MOVE_TO_DEPRECATED -> return Bytecode.MoveToDeprecated(
            StructDefinitionIndex(
                buffer.readUleb128AsU64().toUShort()
            )
        )

        Opcodes.MOVE_TO_GENERIC_DEPRECATED -> return Bytecode.MoveToGenericDeprecated(
            StructDefInstantiationIndex(
                buffer.readUleb128AsU64().toUShort()
            )
        )

        Opcodes.MUT_BORROW_GLOBAL_DEPRECATED -> return Bytecode.MutBorrowGlobalDeprecated(
            StructDefinitionIndex(
                buffer.readUleb128AsU64().toUShort()
            )
        )

        Opcodes.IMM_BORROW_GLOBAL_DEPRECATED -> return Bytecode.ImmBorrowGlobalDeprecated(
            StructDefinitionIndex(
                buffer.readUleb128AsU64().toUShort()
            )
        )

        Opcodes.MUT_BORROW_GLOBAL_GENERIC_DEPRECated -> return Bytecode.MutBorrowGlobalGenericDeprecated(
            StructDefInstantiationIndex(buffer.readUleb128AsU64().toUShort())
        )

        Opcodes.IMM_BORROW_GLOBAL_GENERIC_DEPRECATED -> return Bytecode.ImmBorrowGlobalGenericDeprecated(
            StructDefInstantiationIndex(buffer.readUleb128AsU64().toUShort())
        )

        else -> throw IllegalStateException("Unknown opcode value: $opcode")
    }
}