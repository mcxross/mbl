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

import com.ionspin.kotlin.bignum.integer.BigInteger
import xyz.mcxross.mbl.format.CodeOffset
import xyz.mcxross.mbl.format.ConstantPoolIndex
import xyz.mcxross.mbl.format.FieldHandleIndex
import xyz.mcxross.mbl.format.FieldInstantiationIndex
import xyz.mcxross.mbl.format.FunctionHandleIndex
import xyz.mcxross.mbl.format.FunctionInstantiationIndex
import xyz.mcxross.mbl.format.LocalIndex
import xyz.mcxross.mbl.format.SignatureIndex
import xyz.mcxross.mbl.format.StructDefInstantiationIndex
import xyz.mcxross.mbl.format.StructDefinitionIndex
import xyz.mcxross.mbl.format.VariantHandleIndex
import xyz.mcxross.mbl.format.VariantInstantiationHandleIndex
import xyz.mcxross.mbl.format.VariantJumpTableIndex

sealed interface Bytecode {
    data object Pop : Bytecode
    data object Ret : Bytecode
    data class BrTrue(val offset: CodeOffset) : Bytecode
    data class BrFalse(val offset: CodeOffset) : Bytecode
    data class Branch(val offset: CodeOffset) : Bytecode
    data class LdU8(val value: UByte) : Bytecode
    data class LdU16(val value: UShort) : Bytecode
    data class LdU32(val value: UInt) : Bytecode
    data class LdU64(val value: ULong) : Bytecode
    data class LdU128(val value: BigInteger) : Bytecode
    data class LdU256(val value: BigInteger) : Bytecode
    data object CastU8 : Bytecode
    data object CastU16 : Bytecode
    data object CastU32 : Bytecode
    data object CastU64 : Bytecode
    data object CastU128 : Bytecode
    data object CastU256 : Bytecode
    data class LdConst(val index: ConstantPoolIndex) : Bytecode
    data object LdTrue : Bytecode
    data object LdFalse : Bytecode
    data class CopyLoc(val index: LocalIndex) : Bytecode
    data class MoveLoc(val index: LocalIndex) : Bytecode
    data class StLoc(val index: LocalIndex) : Bytecode
    data class Call(val index: FunctionHandleIndex) : Bytecode
    data class CallGeneric(val index: FunctionInstantiationIndex) : Bytecode
    data class Pack(val index: StructDefinitionIndex) : Bytecode
    data class PackGeneric(val index: StructDefInstantiationIndex) : Bytecode
    data class Unpack(val index: StructDefinitionIndex) : Bytecode
    data class UnpackGeneric(val index: StructDefInstantiationIndex) : Bytecode
    data object ReadRef : Bytecode
    data object WriteRef : Bytecode
    data object FreezeRef : Bytecode
    data class MutBorrowLoc(val index: LocalIndex) : Bytecode
    data class ImmBorrowLoc(val index: LocalIndex) : Bytecode
    data class MutBorrowField(val index: FieldHandleIndex) : Bytecode
    data class MutBorrowFieldGeneric(val index: FieldInstantiationIndex) : Bytecode
    data class ImmBorrowField(val index: FieldHandleIndex) : Bytecode
    data class ImmBorrowFieldGeneric(val index: FieldInstantiationIndex) : Bytecode
    data object Add : Bytecode
    data object Sub : Bytecode
    data object Mul : Bytecode
    data object Mod : Bytecode
    data object Div : Bytecode
    data object BitOr : Bytecode
    data object BitAnd : Bytecode
    data object Xor : Bytecode
    data object Shl : Bytecode
    data object Shr : Bytecode
    data object Or : Bytecode
    data object And : Bytecode
    data object Not : Bytecode
    data object Eq : Bytecode
    data object Neq : Bytecode
    data object Lt : Bytecode
    data object Gt : Bytecode
    data object Le : Bytecode
    data object Ge : Bytecode
    data object Abort : Bytecode
    data object Nop : Bytecode
    data class VecPack(val sigIndex: SignatureIndex, val num: ULong) : Bytecode
    data class VecLen(val sigIndex: SignatureIndex) : Bytecode
    data class VecImmBorrow(val sigIndex: SignatureIndex) : Bytecode
    data class VecMutBorrow(val sigIndex: SignatureIndex) : Bytecode
    data class VecPushBack(val sigIndex: SignatureIndex) : Bytecode
    data class VecPopBack(val sigIndex: SignatureIndex) : Bytecode
    data class VecUnpack(val sigIndex: SignatureIndex, val num: ULong) : Bytecode
    data class VecSwap(val sigIndex: SignatureIndex) : Bytecode
    data class PackVariant(val index: VariantHandleIndex) : Bytecode
    data class PackVariantGeneric(val index: VariantInstantiationHandleIndex) : Bytecode
    data class UnpackVariant(val index: VariantHandleIndex) : Bytecode
    data class UnpackVariantImmRef(val index: VariantHandleIndex) : Bytecode
    data class UnpackVariantMutRef(val index: VariantHandleIndex) : Bytecode
    data class UnpackVariantGeneric(val index: VariantInstantiationHandleIndex) : Bytecode
    data class UnpackVariantGenericImmRef(val index: VariantInstantiationHandleIndex) : Bytecode
    data class UnpackVariantGenericMutRef(val index: VariantInstantiationHandleIndex) : Bytecode
    data class VariantSwitch(val index: VariantJumpTableIndex) : Bytecode

    // --- DEPRECATED BYTECODES ---
    @Deprecated("This bytecode is deprecated.")
    data class ExistsDeprecated(val index: StructDefinitionIndex) : Bytecode
    @Deprecated("This bytecode is deprecated.")
    data class ExistsGenericDeprecated(val index: StructDefInstantiationIndex) : Bytecode
    @Deprecated("This bytecode is deprecated.")
    data class MoveFromDeprecated(val index: StructDefinitionIndex) : Bytecode
    @Deprecated("This bytecode is deprecated.")
    data class MoveFromGenericDeprecated(val index: StructDefInstantiationIndex) : Bytecode
    @Deprecated("This bytecode is deprecated.")
    data class MoveToDeprecated(val index: StructDefinitionIndex) : Bytecode
    @Deprecated("This bytecode is deprecated.")
    data class MoveToGenericDeprecated(val index: StructDefInstantiationIndex) : Bytecode
    @Deprecated("This bytecode is deprecated.")
    data class MutBorrowGlobalDeprecated(val index: StructDefinitionIndex) : Bytecode
    @Deprecated("This bytecode is deprecated.")
    data class MutBorrowGlobalGenericDeprecated(val index: StructDefInstantiationIndex) : Bytecode
    @Deprecated("This bytecode is deprecated.")
    data class ImmBorrowGlobalDeprecated(val index: StructDefinitionIndex) : Bytecode
    @Deprecated("This bytecode is deprecated.")
    data class ImmBorrowGlobalGenericDeprecated(val index: StructDefInstantiationIndex) : Bytecode
}