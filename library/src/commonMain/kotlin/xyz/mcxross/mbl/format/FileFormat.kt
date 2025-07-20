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

import xyz.mcxross.mbl.model.Ability
import xyz.mcxross.mbl.model.AccountAddress
import xyz.mcxross.mbl.model.Bytecode
import xyz.mcxross.mbl.model.Identifier
import xyz.mcxross.mbl.model.Metadata
import xyz.mcxross.mbl.model.ModuleHandleIndex
import xyz.mcxross.mbl.model.Visibility
import kotlin.jvm.JvmInline

typealias CodeOffset = UShort
typealias TableIndex = UShort
typealias MemberCount = UShort
typealias IdentifierPool = List<Identifier>
typealias AddressIdentifierPool = List<AccountAddress>
typealias ConstantPool = List<Constant>
typealias SignaturePool = List<Signature>
typealias LocalIndex = UByte
typealias VariantTag = MemberCount

@JvmInline
value class DatatypeHandleIndex(val value: UShort)

@JvmInline
value class TypeParameterIndex(val value: UShort)

@JvmInline
value class SignatureIndex(val value: UShort)

@JvmInline
value class FunctionHandleIndex(val value: UShort)

@JvmInline
value class FunctionInstantiationIndex(val value: UShort)

@JvmInline
value class ConstantPoolIndex(val value: UShort)

@JvmInline
value class StructDefinitionIndex(val value: UShort)

@JvmInline
value class StructDefInstantiationIndex(val value: UShort)

@JvmInline
value class FieldHandleIndex(val value: UShort)

@JvmInline
value class FieldInstantiationIndex(val value: UShort)

@JvmInline
value class EnumDefinitionIndex(val value: UShort)

@JvmInline
value class EnumDefInstantiationIndex(val value: UShort)

@JvmInline
value class VariantHandleIndex(val value: UShort)

@JvmInline
value class VariantInstantiationHandleIndex(val value: UShort)

@JvmInline
value class VariantJumpTableIndex(val value: UShort)

sealed class SignatureToken {

    object Bool : SignatureToken()

    object U8 : SignatureToken()

    object U64 : SignatureToken()

    object U128 : SignatureToken()

    object Address : SignatureToken()

    object Signer : SignatureToken()

    data class Vector(val elementType: SignatureToken) : SignatureToken()

    data class Datatype(val index: DatatypeHandleIndex) : SignatureToken()

    data class DatatypeInstantiation(
        val index: DatatypeHandleIndex,
        val typeArgs: List<SignatureToken>
    ) : SignatureToken()

    data class Reference(val referencedType: SignatureToken) : SignatureToken()

    data class MutableReference(val referencedType: SignatureToken) : SignatureToken()

    data class TypeParameter(val index: TypeParameterIndex) : SignatureToken()

    object U16 : SignatureToken()

    object U32 : SignatureToken()

    object U256 : SignatureToken()
}

data class Constant(
    val type: SignatureToken,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Constant

        return data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        return data.contentHashCode()
    }
}

data class Signature(
    val tokens: List<SignatureToken>
)

@JvmInline
value class AddressIdentifierIndex(val value: UShort)

@JvmInline
value class IdentifierIndex(val value: UShort)

data class ModuleHandle(
    val address: AddressIdentifierIndex,
    val name: IdentifierIndex
)

@JvmInline
value class AbilitySet(val value: UByte) {

    companion object {
        val EMPTY = AbilitySet(0u)

        val PRIMITIVES = AbilitySet(
            Ability.Copy.bit or Ability.Drop.bit or Ability.Store.bit
        )

        val REFERENCES = AbilitySet(
            Ability.Copy.bit or Ability.Drop.bit
        )

        val SIGNER = AbilitySet(Ability.Drop.bit)

        val VECTOR = AbilitySet(
            Ability.Copy.bit or Ability.Drop.bit or Ability.Store.bit
        )

        val ALL = AbilitySet(
            Ability.Copy.bit or Ability.Drop.bit or Ability.Store.bit or Ability.Key.bit
        )

        fun singleton(ability: Ability): AbilitySet = AbilitySet(ability.bit)

        fun fromUByte(byte: UByte): AbilitySet? =
            if (isSubsetBits(byte, ALL.value)) AbilitySet(byte) else null

        private fun isSubsetBits(sub: UByte, sup: UByte): Boolean =
            (sub.toInt() and sup.toInt()) == sub.toInt()
    }

    fun hasAbility(ability: Ability): Boolean =
        (value.toInt() and ability.bit.toInt()) == ability.bit.toInt()

    fun hasCopy() = hasAbility(Ability.Copy)
    fun hasDrop() = hasAbility(Ability.Drop)
    fun hasStore() = hasAbility(Ability.Store)
    fun hasKey() = hasAbility(Ability.Key)

    fun remove(ability: Ability): AbilitySet = difference(singleton(ability))
    fun intersect(other: AbilitySet): AbilitySet = AbilitySet(value and other.value)
    fun union(other: AbilitySet): AbilitySet = AbilitySet(value or other.value)
    fun difference(other: AbilitySet): AbilitySet = AbilitySet(value and other.value.inv())

    fun isSubset(other: AbilitySet): Boolean =
        isSubsetBits(this.value, other.value)

    fun toUByte(): UByte = value

    fun polymorphicAbilities(
        declaredAbilities: AbilitySet,
        declaredPhantomParameters: List<Boolean>,
        typeArguments: List<AbilitySet>
    ): Result<AbilitySet> {
        if (declaredPhantomParameters.size != typeArguments.size) {
            return Result.failure(IllegalArgumentException("Mismatch in phantom parameter and type argument sizes"))
        }

        val filteredAbilities = typeArguments.zip(declaredPhantomParameters)
            .filter { (_, isPhantom) -> !isPhantom }
            .map { (abilities, _) ->
                abilities.toAbilities()
                    .map { it.requiredBy() }
                    .fold(EMPTY) { acc, req -> acc.union(singleton(req)) }
            }
            .fold(declaredAbilities) { acc, next -> acc.intersect(next) }

        return Result.success(filteredAbilities)
    }

    private fun toAbilities(): List<Ability> =
        Ability.entries.filter { hasAbility(it) }

}

data class DatatypeTyParameter(
    val constraints: AbilitySet,
    val isPhantom: Boolean
)

data class DatatypeHandle(
    val module: ModuleHandleIndex,
    val name: IdentifierIndex,
    val abilities: List<AbilitySet>,
    val typeParameters: List<DatatypeTyParameter>
)

data class FunctionHandle(
    val module: ModuleHandleIndex,
    val name: IdentifierIndex,
    val parameter: SignatureIndex,
    val `return`: SignatureIndex,
    val typeParameters: List<AbilitySet>
)

data class  FieldHandle(
    val owner: StructDefinitionIndex,
    val field: MemberCount,
)

data class StructDefInstantiation(
    val def: StructDefinitionIndex,
    val typeParameters: SignatureIndex
)

data class FieldInstantiation(
    val handle: FieldHandleIndex,
    val typeParameters: SignatureIndex
)

@JvmInline
value class TypeSignature(val value: SignatureToken)

data class FieldDefinition(
    val name: IdentifierIndex,
    val signature: TypeSignature
)

sealed class StructFieldInformation {

    object Native : StructFieldInformation()

    data class Declared(val fields: List<FieldDefinition>) : StructFieldInformation()
}

data class StructDefinition(
    val structHandle: DatatypeHandleIndex,
    val fieldInformation: StructFieldInformation
)

data class FunctionInstantiation(
    val handle: FunctionHandleIndex,
    val typeParameters: SignatureIndex,
)

data class VariantJumpTable(
    val headEnum: EnumDefinitionIndex,
    // This list contains the bytecode offsets for each branch.
    val jumpTable: List<UShort>
)

data class CodeUnit(
    val locals: SignatureIndex,
    val code: List<Bytecode>,
    val jumpTables: List<VariantJumpTable>,
)

data class FunctionDefinition(
    val function: FunctionHandleIndex,
    val visibility: Visibility,
    val isEntry: Boolean,
    val acquiresGlobalResources: List<StructDefinitionIndex>,
    val code: CodeUnit?
) {
    companion object {
        const val DEPRECATED_PUBLIC_BIT = 0b01
        const val NATIVE = 0b10
        const val ENTRY = 0b100
    }
}

data class VariantDefinition(
    val variantName: IdentifierIndex,
    val fields: List<FieldDefinition>
)

data class EnumDefinition(
    val enumHandle: DatatypeHandleIndex,
    val variants: List<VariantDefinition>
)

data class EnumDefInstantiation(
    val def: EnumDefinitionIndex,
    val typeParameters: SignatureIndex
)

data class VariantHandle(
    val enumDef: EnumDefinitionIndex,
    val variant: VariantTag
)

data class VariantInstantiationHandle(
    val enumDef: EnumDefInstantiationIndex,
    val variant: VariantTag
)

data class CompiledModule(

    val version: UInt,

    val publishable: Boolean,

    val selfModuleHandleIdx: ModuleHandleIndex,

    val moduleHandles: List<ModuleHandle>,

    val datatypeHandles: List<DatatypeHandle>,

    val functionHandles: List<FunctionHandle>,

    val fieldHandles: List<FieldHandle>,

    val friendDecls: List<ModuleHandle>,

    val functionInstantiations: List<FunctionInstantiation>,
    val fieldInstantiations: List<FieldInstantiation>,

    val signatures: SignaturePool,

    val identifiers: IdentifierPool,

    val addressIdentifiers: AddressIdentifierPool,

    val constantPool: ConstantPool,

    val metadata: List<Metadata>,

    val structDefs: List<StructDefinition>,
    val structDefInstantiations: List<StructDefInstantiation>,

    val functionDefs: List<FunctionDefinition>,

    val enumDefs: List<EnumDefinition>,
    val enumDefInstantiations: List<EnumDefInstantiation>,


    val variantHandles: List<VariantHandle>,
    val variantInstantiationHandles: List<VariantInstantiationHandle>
)