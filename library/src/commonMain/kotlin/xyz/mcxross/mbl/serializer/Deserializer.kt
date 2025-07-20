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
package xyz.mcxross.mbl.serializer

import com.github.michaelbull.result.Ok
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlinx.io.readString
import kotlinx.io.readUByte
import xyz.mcxross.mbl.format.AbilitySet
import xyz.mcxross.mbl.format.AddressIdentifierIndex
import xyz.mcxross.mbl.format.CompiledModule
import xyz.mcxross.mbl.format.Constant
import xyz.mcxross.mbl.format.DatatypeHandleIndex
import xyz.mcxross.mbl.format.FunctionDefinition
import xyz.mcxross.mbl.format.FunctionHandle
import xyz.mcxross.mbl.format.FunctionHandleIndex
import xyz.mcxross.mbl.format.IdentifierIndex
import xyz.mcxross.mbl.format.ModuleHandle
import xyz.mcxross.mbl.model.ModuleHandleIndex
import xyz.mcxross.mbl.utils.SIGNATURE_TOKEN_DEPTH_MAX
import xyz.mcxross.mbl.format.Signature
import xyz.mcxross.mbl.format.SignatureIndex
import xyz.mcxross.mbl.format.SignatureToken
import xyz.mcxross.mbl.format.StructDefinitionIndex
import xyz.mcxross.mbl.format.TypeParameterIndex
import xyz.mcxross.mbl.model.Ability
import xyz.mcxross.mbl.model.DeprecatedKind
import xyz.mcxross.mbl.model.SerializedType
import xyz.mcxross.mbl.model.TableType
import xyz.mcxross.mbl.model.TypeBuilder
import xyz.mcxross.mbl.model.Visibility
import xyz.mcxross.mbl.exception.PartialVMError
import xyz.mcxross.mbl.extensions.readUleb128AsU64
import xyz.mcxross.mbl.format.loadCodeUnit
import com.github.michaelbull.result.Result
import xyz.mcxross.mbl.format.DatatypeHandle
import xyz.mcxross.mbl.format.DatatypeTyParameter
import xyz.mcxross.mbl.format.EnumDefInstantiation
import xyz.mcxross.mbl.format.EnumDefInstantiationIndex
import xyz.mcxross.mbl.format.EnumDefinition
import xyz.mcxross.mbl.format.EnumDefinitionIndex
import xyz.mcxross.mbl.format.FieldDefinition
import xyz.mcxross.mbl.format.FieldHandle
import xyz.mcxross.mbl.format.FieldHandleIndex
import xyz.mcxross.mbl.format.FieldInstantiation
import xyz.mcxross.mbl.format.FunctionInstantiation
import xyz.mcxross.mbl.format.StructDefInstantiation
import xyz.mcxross.mbl.format.StructDefinition
import xyz.mcxross.mbl.format.StructFieldInformation
import xyz.mcxross.mbl.format.TypeSignature
import xyz.mcxross.mbl.format.VariantDefinition
import xyz.mcxross.mbl.format.VariantHandle
import xyz.mcxross.mbl.format.VariantInstantiationHandle
import xyz.mcxross.mbl.format.VersionedBinary
import xyz.mcxross.mbl.model.AccountAddress
import xyz.mcxross.mbl.model.Identifier
import xyz.mcxross.mbl.model.Metadata


fun loadIdentifiers(buffer: Buffer): List<Identifier> {
    val identifiers = mutableListOf<Identifier>()
    while (!buffer.exhausted()) {
        val len = buffer.readUleb128AsU64()
        identifiers.add(Identifier.of(buffer.readString(len.toLong())))
    }
    return identifiers
}

fun loadModuleHandles(buffer: Buffer): List<ModuleHandle> {
    val handles = mutableListOf<ModuleHandle>()
    while (!buffer.exhausted()) {
        val addressIndex = AddressIdentifierIndex(buffer.readUleb128AsU64().toUShort())
        val nameIndex = IdentifierIndex(buffer.readUleb128AsU64().toUShort())
        handles.add(ModuleHandle(addressIndex, nameIndex))
    }
    return handles
}

fun loadAddressIdentifiers(buffer: Buffer): List<AccountAddress> {
    val addresses = mutableListOf<AccountAddress>()
    val addressLength = 32
    while (!buffer.exhausted()) {
        addresses.add(AccountAddress(buffer.readByteArray(addressLength)))
    }
    return addresses
}

fun loadSignatureToken(buffer: Buffer): SignatureToken {

    fun readNextBuilder(buf: Buffer): TypeBuilder {
        val typeCode = buf.readUByte()
        return when (typeCode) {
            SerializedType.BOOL -> TypeBuilder.Saturated(SignatureToken.Bool)
            SerializedType.U8 -> TypeBuilder.Saturated(SignatureToken.U8)
            SerializedType.U16 -> TypeBuilder.Saturated(SignatureToken.U16)
            SerializedType.U32 -> TypeBuilder.Saturated(SignatureToken.U32)
            SerializedType.U64 -> TypeBuilder.Saturated(SignatureToken.U64)
            SerializedType.U128 -> TypeBuilder.Saturated(SignatureToken.U128)
            SerializedType.U256 -> TypeBuilder.Saturated(SignatureToken.U256)
            SerializedType.ADDRESS -> TypeBuilder.Saturated(SignatureToken.Address)
            SerializedType.SIGNER -> TypeBuilder.Saturated(SignatureToken.Signer)
            SerializedType.VECTOR -> TypeBuilder.VectorBuilder
            SerializedType.REFERENCE -> TypeBuilder.ReferenceBuilder
            SerializedType.MUTABLE_REFERENCE -> TypeBuilder.MutableReferenceBuilder
            SerializedType.TYPE_PARAMETER -> {
                val index = TypeParameterIndex(buf.readUleb128AsU64().toUShort())
                TypeBuilder.Saturated(SignatureToken.TypeParameter(index))
            }

            SerializedType.STRUCT -> {
                val index = DatatypeHandleIndex(buf.readUleb128AsU64().toUShort())
                TypeBuilder.Saturated(SignatureToken.Datatype(index))
            }

            SerializedType.DATATYPE_INST -> {
                val handleIndex = DatatypeHandleIndex(buf.readUleb128AsU64().toUShort())
                val arity = buf.readUleb128AsU64().toInt()
                if (arity == 0) {
                    TypeBuilder.Saturated(SignatureToken.DatatypeInstantiation(handleIndex, emptyList()))
                } else {
                    TypeBuilder.DatatypeInstBuilder(handleIndex, arity)
                }
            }

            else -> throw IllegalStateException("Unknown serialized type code: $typeCode")
        }
    }

    val firstBuilder = readNextBuilder(buffer)
    if (firstBuilder is TypeBuilder.Saturated) {
        return firstBuilder.token
    }

    val stack = mutableListOf(firstBuilder)

    while (true) {
        if (stack.size > SIGNATURE_TOKEN_DEPTH_MAX) {
            throw IllegalStateException("Signature token depth exceeded maximum")
        }

        val top = stack.last()
        if (top is TypeBuilder.Saturated) {
            val saturatedToken = stack.removeLast().let { (it as TypeBuilder.Saturated).token }
            if (stack.isEmpty()) {
                return saturatedToken
            }
            stack[stack.lastIndex] = stack.last().apply(saturatedToken)
        } else {
            stack.add(readNextBuilder(buffer))
        }
    }
}

fun loadConstantPool(buffer: Buffer): List<Constant> {
    val constants = mutableListOf<Constant>()
    while (!buffer.exhausted()) {
        val type = loadSignatureToken(buffer) // Assuming loadSignatureToken is implemented
        val len = buffer.readUleb128AsU64()
        val data = buffer.readByteArray(len.toInt())
        constants.add(Constant(type, data))
    }
    return constants
}


fun loadSignatures(buffer: Buffer): List<Signature> {
    val signatures = mutableListOf<Signature>()
    while (!buffer.exhausted()) {
        val len = buffer.readUleb128AsU64()
        val tokens = (1..len.toInt()).map { loadSignatureToken(buffer) }
        signatures.add(Signature(tokens))
    }
    return signatures
}

fun loadAbilitySet(buffer: Buffer, version: Int): AbilitySet {
    if (version < 2) {
        val byte = buffer.readUByte()
        val kind = DeprecatedKind.fromUByte(byte)
            ?: throw IllegalStateException("Invalid deprecated kind byte: $byte")

        return when (kind) {
            DeprecatedKind.ALL -> AbilitySet.EMPTY
            DeprecatedKind.COPYABLE -> AbilitySet.singleton(Ability.Copy)
                .union(AbilitySet.singleton(Ability.Drop))

            DeprecatedKind.RESOURCE -> AbilitySet.singleton(Ability.Key)
        }
    } else {
        val byte = buffer.readUleb128AsU64().toUByte()
        return AbilitySet.fromUByte(byte)
            ?: throw IllegalStateException("Invalid ability set byte: $byte")
    }
}


fun loadAbilitySets(buffer: Buffer, version: Int): List<AbilitySet> {
    val count = buffer.readUleb128AsU64()
    return (1..count.toInt()).map {
        loadAbilitySet(buffer, version)
    }
}

fun loadFunctionHandles(buffer: Buffer, version: Int): List<FunctionHandle> {
    val handles = mutableListOf<FunctionHandle>()
    while (!buffer.exhausted()) {
        val module = ModuleHandleIndex(buffer.readUleb128AsU64().toUShort())
        val name = IdentifierIndex(buffer.readUleb128AsU64().toUShort())
        val parameters = SignatureIndex(buffer.readUleb128AsU64().toUShort())
        val return_ = SignatureIndex(buffer.readUleb128AsU64().toUShort())
        val typeParameters = loadAbilitySets(buffer, version)
        handles.add(FunctionHandle(module, name, parameters, return_, typeParameters))
    }
    return handles
}

fun loadAcquires(buffer: Buffer): List<StructDefinitionIndex> {
    val count = buffer.readUleb128AsU64()
    return (1..count.toInt()).map {
        StructDefinitionIndex(buffer.readUleb128AsU64().toUShort())
    }
}

fun loadFunctionDefs(buffer: Buffer, version: Int): List<FunctionDefinition> {
    val definitions = mutableListOf<FunctionDefinition>()
    while (!buffer.exhausted()) {
        val function = FunctionHandleIndex(buffer.readUleb128AsU64().toUShort())
        val visibilityValue = buffer.readUByte()
        val visibility = Visibility.fromUByte(visibilityValue) ?: Visibility.Private
        val flags = buffer.readUByte()
        val isEntry = (flags and FunctionDefinition.ENTRY.toUByte()) != 0.toUByte()

        val acquires = loadAcquires(buffer)

        val code = if ((flags and FunctionDefinition.NATIVE.toUByte()) != 0.toUByte()) {
            null
        } else {
            loadCodeUnit(buffer, version)
        }

        definitions.add(
            FunctionDefinition(
                function = function,
                visibility = visibility,
                isEntry = isEntry,
                acquiresGlobalResources = acquires,
                code = code
            )
        )
    }
    return definitions
}

fun loadDatatypeHandles(buffer: Buffer, version: Int): List<DatatypeHandle> {
    val handles = mutableListOf<DatatypeHandle>()
    while (!buffer.exhausted()) {
        val module = ModuleHandleIndex(buffer.readUleb128AsU64().toUShort())
        val name = IdentifierIndex(buffer.readUleb128AsU64().toUShort())
        val abilities = loadAbilitySet(buffer, version)
        val typeParameters = loadDatatypeTyParameters(buffer, version)
        handles.add(DatatypeHandle(module, name, listOf(abilities), typeParameters)) // Wrapped abilities in a list
    }
    return handles
}

fun loadDatatypeTyParameters(buffer: Buffer, version: Int): List<DatatypeTyParameter> {
    val count = buffer.readUleb128AsU64()
    return (1..count.toInt()).map {
        val constraints = loadAbilitySet(buffer, version)
        val isPhantom = if (version < 3) false else (buffer.readUleb128AsU64().toInt() != 0)
        DatatypeTyParameter(constraints, isPhantom)
    }
}

fun loadFieldHandles(buffer: Buffer): List<FieldHandle> {
    val handles = mutableListOf<FieldHandle>()
    while (!buffer.exhausted()) {
        val owner = StructDefinitionIndex(buffer.readUleb128AsU64().toUShort())
        val field = buffer.readUleb128AsU64().toUShort()
        handles.add(FieldHandle(owner, field))
    }
    return handles
}

fun loadFunctionInstantiations(buffer: Buffer): List<FunctionInstantiation> {
    val instantiations = mutableListOf<FunctionInstantiation>()
    while (!buffer.exhausted()) {
        val handle = FunctionHandleIndex(buffer.readUleb128AsU64().toUShort())
        val typeParameters = SignatureIndex(buffer.readUleb128AsU64().toUShort())
        instantiations.add(FunctionInstantiation(handle, typeParameters))
    }
    return instantiations
}

fun loadFieldInstantiations(buffer: Buffer): List<FieldInstantiation> {
    val instantiations = mutableListOf<FieldInstantiation>()
    while (!buffer.exhausted()) {
        val handle = FieldHandleIndex(buffer.readUleb128AsU64().toUShort())
        val typeParameters = SignatureIndex(buffer.readUleb128AsU64().toUShort())
        instantiations.add(FieldInstantiation(handle, typeParameters))
    }
    return instantiations
}


fun loadStructDefInstantiations(buffer: Buffer): List<StructDefInstantiation> {
    val instantiations = mutableListOf<StructDefInstantiation>()
    while (!buffer.exhausted()) {
        val def = StructDefinitionIndex(buffer.readUleb128AsU64().toUShort())
        val typeParameters = SignatureIndex(buffer.readUleb128AsU64().toUShort())
        instantiations.add(StructDefInstantiation(def, typeParameters))
    }
    return instantiations
}

fun loadStructDefs(buffer: Buffer): List<StructDefinition> {
    val definitions = mutableListOf<StructDefinition>()
    var structCounter = 0
    while (!buffer.exhausted()) {
        structCounter++

        val structHandle = DatatypeHandleIndex(buffer.readUleb128AsU64().toUShort())

        val fieldInfoFlag = buffer.readUByte()

        val fieldInformation = if (fieldInfoFlag == SerializedNativeStructFlag.NATIVE) {
            StructFieldInformation.Native
        } else {
            val fieldCount = buffer.readUleb128AsU64()

            val fields = (1..fieldCount.toInt()).map { fieldIndex ->
                val name = IdentifierIndex(buffer.readUleb128AsU64().toUShort())

                val signature = TypeSignature(loadSignatureToken(buffer))

                FieldDefinition(name, signature)
            }
            StructFieldInformation.Declared(fields)
        }
        definitions.add(StructDefinition(structHandle, fieldInformation))
    }
    return definitions
}

object SerializedNativeStructFlag {
    const val NATIVE: UByte = 0x1u
    const val DECLARED: UByte = 0x2u
}

fun loadEnumDefs(buffer: Buffer): List<EnumDefinition> {
    val definitions = mutableListOf<EnumDefinition>()
    while (!buffer.exhausted()) {
        val enumHandle = DatatypeHandleIndex(buffer.readUleb128AsU64().toUShort())
        // Skip the enum flag (always DECLARED for now)
        buffer.readByte()
        val variantCount = buffer.readUleb128AsU64()
        val variants = (1..variantCount.toInt()).map {
            loadVariantDef(buffer)
        }
        definitions.add(EnumDefinition(enumHandle, variants))
    }
    return definitions
}

private fun loadVariantDef(buffer: Buffer): VariantDefinition {
    val variantName = IdentifierIndex(buffer.readUleb128AsU64().toUShort())
    val fieldCount = buffer.readUleb128AsU64()
    val fields = (1..fieldCount.toInt()).map {
        val name = IdentifierIndex(buffer.readUleb128AsU64().toUShort())
        val signature = TypeSignature(loadSignatureToken(buffer))
        FieldDefinition(name, signature)
    }
    return VariantDefinition(variantName, fields)
}

fun loadEnumDefInstantiations(buffer: Buffer): List<EnumDefInstantiation> {
    val instantiations = mutableListOf<EnumDefInstantiation>()
    while (!buffer.exhausted()) {
        val def = EnumDefinitionIndex(buffer.readUleb128AsU64().toUShort())
        val typeParameters = SignatureIndex(buffer.readUleb128AsU64().toUShort())
        instantiations.add(EnumDefInstantiation(def, typeParameters))
    }
    return instantiations
}

fun loadVariantHandles(buffer: Buffer): List<VariantHandle> {
    val handles = mutableListOf<VariantHandle>()
    while (!buffer.exhausted()) {
        val enumDef = EnumDefinitionIndex(buffer.readUleb128AsU64().toUShort())
        val variant = buffer.readUleb128AsU64().toUShort()
        handles.add(VariantHandle(enumDef, variant))
    }
    return handles
}

fun loadVariantInstantiationHandles(buffer: Buffer): List<VariantInstantiationHandle> {
    val handles = mutableListOf<VariantInstantiationHandle>()
    while (!buffer.exhausted()) {
        val enumDef = EnumDefInstantiationIndex(buffer.readUleb128AsU64().toUShort())
        val variant = buffer.readUleb128AsU64().toUShort()
        handles.add(VariantInstantiationHandle(enumDef, variant))
    }
    return handles
}

fun loadMetadata(buffer: Buffer): List<Metadata> {
    val metadataList = mutableListOf<Metadata>()
    while (!buffer.exhausted()) {
        val keyLen = buffer.readUleb128AsU64()
        val key = buffer.readByteArray(keyLen.toInt())
        val valueLen = buffer.readUleb128AsU64()
        val value = buffer.readByteArray(valueLen.toInt())
        metadataList.add(Metadata(key, value))
    }
    return metadataList
}

internal fun deserializeModule(versionedBinary: VersionedBinary): Result<CompiledModule, PartialVMError> {
    val tables = versionedBinary.tables.associateBy { it.kind }
    val version = versionedBinary.version

    val identifiers = tables[TableType.IDENTIFIERS]?.let {
        loadIdentifiers(versionedBinary.newCursorForTable(it))
    } ?: emptyList()

    val addressIdentifiers = tables[TableType.ADDRESS_IDENTIFIERS]?.let {
        loadAddressIdentifiers(versionedBinary.newCursorForTable(it))
    } ?: emptyList()

    val constantPool = tables[TableType.CONSTANT_POOL]?.let {
        loadConstantPool(versionedBinary.newCursorForTable(it))
    } ?: emptyList()

    val signatures = tables[TableType.SIGNATURES]?.let {
        loadSignatures(versionedBinary.newCursorForTable(it))
    } ?: emptyList()

    val moduleHandles = tables[TableType.MODULE_HANDLES]?.let {
        loadModuleHandles(versionedBinary.newCursorForTable(it))
    } ?: emptyList()

    val datatypeHandles = tables[TableType.DATATYPE_HANDLES]?.let {
        loadDatatypeHandles(versionedBinary.newCursorForTable(it), version)
    } ?: emptyList()

    val functionHandles = tables[TableType.FUNCTION_HANDLES]?.let {
        loadFunctionHandles(versionedBinary.newCursorForTable(it), version)
    } ?: emptyList()

    val fieldHandles = tables[TableType.FIELD_HANDLE]?.let {
        loadFieldHandles(versionedBinary.newCursorForTable(it))
    } ?: emptyList()

    val friendDecls = tables[TableType.FRIEND_DECLS]?.let {
        loadModuleHandles(versionedBinary.newCursorForTable(it))
    } ?: emptyList()

    val functionInstantiations = tables[TableType.FUNCTION_INST]?.let {
        loadFunctionInstantiations(versionedBinary.newCursorForTable(it))
    } ?: emptyList()

    val fieldInstantiations = tables[TableType.FIELD_INST]?.let {
        loadFieldInstantiations(versionedBinary.newCursorForTable(it))
    } ?: emptyList()

    val structDefInstantiations = tables[TableType.STRUCT_DEF_INST]?.let {
        loadStructDefInstantiations(versionedBinary.newCursorForTable(it))
    } ?: emptyList()

    val structDefs = tables[TableType.STRUCT_DEFS]?.let {
        loadStructDefs(versionedBinary.newCursorForTable(it))
    } ?: emptyList()

    val functionDefs = tables[TableType.FUNCTION_DEFS]?.let {
        loadFunctionDefs(versionedBinary.newCursorForTable(it), version)
    } ?: emptyList()

    val metadata = tables[TableType.METADATA]?.let {
        loadMetadata(versionedBinary.newCursorForTable(it))
    } ?: emptyList()

    val enumDefs = tables[TableType.ENUM_DEFS]?.let {
        loadEnumDefs(versionedBinary.newCursorForTable(it))
    } ?: emptyList()

    val enumDefInstantiations = tables[TableType.ENUM_DEF_INST]?.let {
        loadEnumDefInstantiations(versionedBinary.newCursorForTable(it))
    } ?: emptyList()

    val variantHandles = tables[TableType.VARIANT_HANDLES]?.let {
        loadVariantHandles(versionedBinary.newCursorForTable(it))
    } ?: emptyList()

    val variantInstantiationHandles = tables[TableType.VARIANT_INST_HANDLES]?.let {
        loadVariantInstantiationHandles(versionedBinary.newCursorForTable(it))
    } ?: emptyList()


    return Ok(
        CompiledModule(
            version = version.toUInt(),
            publishable = versionedBinary.publishable,
            selfModuleHandleIdx = versionedBinary.moduleHandleIndex,
            moduleHandles,
            datatypeHandles,
            functionHandles,
            fieldHandles,
            friendDecls,
            functionInstantiations,
            fieldInstantiations,
            signatures,
            identifiers,
            addressIdentifiers,
            constantPool,
            metadata,
            structDefs,
            structDefInstantiations,
            functionDefs,
            enumDefs,
            enumDefInstantiations,
            variantHandles,
            variantInstantiationHandles,
        )
    )
}