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

import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlinx.io.writeIntLe
import kotlinx.io.writeLongLe
import kotlinx.io.writeShortLe
import xyz.mcxross.mbl.*
import xyz.mcxross.mbl.format.AbilitySet
import xyz.mcxross.mbl.format.BinaryConstants
import xyz.mcxross.mbl.format.BinaryFlavor
import xyz.mcxross.mbl.format.CodeUnit
import xyz.mcxross.mbl.format.CompiledModule
import xyz.mcxross.mbl.format.Constant
import xyz.mcxross.mbl.format.DatatypeHandle
import xyz.mcxross.mbl.format.DatatypeTyParameter
import xyz.mcxross.mbl.format.EnumDefInstantiation
import xyz.mcxross.mbl.format.EnumDefinition
import xyz.mcxross.mbl.format.FieldHandle
import xyz.mcxross.mbl.format.FieldInstantiation
import xyz.mcxross.mbl.format.FunctionDefinition
import xyz.mcxross.mbl.format.FunctionHandle
import xyz.mcxross.mbl.format.FunctionInstantiation
import xyz.mcxross.mbl.format.ModuleHandle
import xyz.mcxross.mbl.format.Signature
import xyz.mcxross.mbl.format.SignatureToken
import xyz.mcxross.mbl.format.StructDefInstantiation
import xyz.mcxross.mbl.format.StructDefinition
import xyz.mcxross.mbl.format.StructFieldInformation
import xyz.mcxross.mbl.format.VariantDefinition
import xyz.mcxross.mbl.format.VariantHandle
import xyz.mcxross.mbl.format.VariantInstantiationHandle
import xyz.mcxross.mbl.format.VariantJumpTable
import xyz.mcxross.mbl.model.*
import xyz.mcxross.mbl.utils.ULEB128

@OptIn(ExperimentalUnsignedTypes::class)
internal fun serializeModule(module: CompiledModule): ByteArray {
    val tableDataBuffer = Buffer()
    val tableInfos = mutableListOf<Table>()
    val version = module.version.toInt()

    val writers = mapOf(
        TableType.MODULE_HANDLES to { buf: Buffer -> module.moduleHandles.forEach { serializeModuleHandle(buf, it) } },
        TableType.DATATYPE_HANDLES to { buf: Buffer ->
            module.datatypeHandles.forEach { handle ->
                serializeDatatypeHandle(buf, handle, version)
            }
        },
        TableType.FUNCTION_HANDLES to { buf: Buffer ->
            module.functionHandles.forEach {
                serializeFunctionHandle(
                    buf,
                    it,
                    version
                )
            }
        },
        TableType.FUNCTION_INST to { buf: Buffer ->
            module.functionInstantiations.forEach {
                serializeFunctionInstantiation(
                    buf,
                    it
                )
            }
        },
        TableType.SIGNATURES to { buf: Buffer -> module.signatures.forEach { serializeSignature(buf, it) } },
        TableType.CONSTANT_POOL to { buf: Buffer -> module.constantPool.forEach { serializeConstant(buf, it) } },
        TableType.IDENTIFIERS to { buf: Buffer -> module.identifiers.forEach { serializeIdentifier(buf, it.value) } },
        TableType.ADDRESS_IDENTIFIERS to { buf: Buffer ->
            module.addressIdentifiers.forEach {
                serializeAddressIdentifier(
                    buf,
                    it
                )
            }
        },
        TableType.STRUCT_DEFS to { buf: Buffer -> module.structDefs.forEach { serializeStructDefinition(buf, it) } },
        TableType.STRUCT_DEF_INST to { buf: Buffer ->
            module.structDefInstantiations.forEach {
                serializeStructDefInstantiation(
                    buf,
                    it
                )
            }
        },
        TableType.FUNCTION_DEFS to { buf: Buffer ->
            module.functionDefs.forEach {
                serializeFunctionDefinition(
                    buf,
                    it,
                    version
                )
            }
        },
        TableType.FIELD_HANDLE to { buf: Buffer -> module.fieldHandles.forEach { serializeFieldHandle(buf, it) } },
        TableType.FIELD_INST to { buf: Buffer ->
            module.fieldInstantiations.forEach {
                serializeFieldInstantiation(
                    buf,
                    it
                )
            }
        },
        TableType.FRIEND_DECLS to { buf: Buffer -> module.friendDecls.forEach { serializeModuleHandle(buf, it) } },
        TableType.METADATA to { buf: Buffer -> module.metadata.forEach { serializeMetadata(buf, it) } },
        TableType.ENUM_DEFS to { buf: Buffer -> module.enumDefs.forEach { serializeEnumDefinition(buf, it) } },
        TableType.ENUM_DEF_INST to { buf: Buffer ->
            module.enumDefInstantiations.forEach {
                serializeEnumDefInstantiation(
                    buf,
                    it
                )
            }
        },
        TableType.VARIANT_HANDLES to { buf: Buffer ->
            module.variantHandles.forEach {
                serializeVariantHandle(
                    buf,
                    it
                )
            }
        },
        TableType.VARIANT_INST_HANDLES to { buf: Buffer ->
            module.variantInstantiationHandles.forEach {
                serializeVariantInstantiationHandle(
                    buf,
                    it
                )
            }
        }
    )

    TableType.entries.sortedBy { it.value }.forEach { tableType ->
        writers[tableType]?.let { writer ->
            val startOffset = tableDataBuffer.size
            writer(tableDataBuffer)
            val count = tableDataBuffer.size - startOffset
            if (count > 0) {
                tableInfos.add(Table(tableType, startOffset.toUInt(), count.toUInt()))
            }
        }
    }

    val finalBuffer = Buffer()

    if (module.publishable) {
        finalBuffer.write(BinaryConstants.MOVE_MAGIC.toByteArray())
    } else {
        finalBuffer.write(BinaryConstants.UNPUBLISHABLE_MAGIC.toByteArray())
    }
    finalBuffer.writeIntLe(BinaryFlavor.encodeVersion(version))

    ULEB128.write(finalBuffer, tableInfos.size)
    tableInfos.forEach { info ->
        finalBuffer.writeByte(info.kind.value.toByte())
        ULEB128.write(finalBuffer, info.offset.toInt())
        ULEB128.write(finalBuffer, info.count.toInt())
    }

    finalBuffer.write(tableDataBuffer, tableDataBuffer.size)

    ULEB128.write(finalBuffer, module.selfModuleHandleIdx.value.toInt())

    return finalBuffer.readByteArray()
}

fun serializeModuleHandle(buffer: Buffer, handle: ModuleHandle) {
    ULEB128.write(buffer, handle.address.value.toInt())
    ULEB128.write(buffer, handle.name.value.toInt())
}

fun serializeDatatypeHandle(buffer: Buffer, handle: DatatypeHandle, version: Int) {
    ULEB128.write(buffer, handle.module.value.toInt())
    ULEB128.write(buffer, handle.name.value.toInt())

    serializeAbilitySet(buffer, handle.abilities.first())
    serializeDatatypeTyParameters(buffer, handle.typeParameters, version)
}

fun serializeFunctionHandle(buffer: Buffer, handle: FunctionHandle, version: Int) {
    ULEB128.write(buffer, handle.module.value.toInt())
    ULEB128.write(buffer, handle.name.value.toInt())
    ULEB128.write(buffer, handle.parameter.value.toInt())
    ULEB128.write(buffer, handle.`return`.value.toInt())

    serializeAbilitySets(buffer, handle.typeParameters, version)
}

fun serializeFunctionInstantiation(buffer: Buffer, inst: FunctionInstantiation) {
    ULEB128.write(buffer, inst.handle.value.toInt())
    ULEB128.write(buffer, inst.typeParameters.value.toInt())
}

fun serializeSignature(buffer: Buffer, sig: Signature) {
    ULEB128.write(buffer, sig.tokens.size)
    sig.tokens.forEach { serializeSignatureToken(buffer, it) }
}

fun serializeConstant(buffer: Buffer, const: Constant) {
    serializeSignatureToken(buffer, const.type)
    ULEB128.write(buffer, const.data.size)
    buffer.write(const.data)
}

fun serializeIdentifier(buffer: Buffer, identifier: String) {
    val bytes = identifier.encodeToByteArray()
    ULEB128.write(buffer, bytes.size)
    buffer.write(bytes)
}

fun serializeAddressIdentifier(buffer: Buffer, addr: AccountAddress) {
    buffer.write(addr.data)
}

fun serializeStructDefinition(buffer: Buffer, def: StructDefinition) {
    ULEB128.write(buffer, def.structHandle.value.toInt())
    when (val info = def.fieldInformation) {
        is StructFieldInformation.Native -> buffer.writeByte(SerializedNativeStructFlag.NATIVE.toByte())
        is StructFieldInformation.Declared -> {
            buffer.writeByte(SerializedNativeStructFlag.DECLARED.toByte())
            ULEB128.write(buffer, info.fields.size)
            info.fields.forEach { field ->
                ULEB128.write(buffer, field.name.value.toInt())
                serializeSignatureToken(buffer, field.signature.value)
            }
        }
    }
}

fun serializeStructDefInstantiation(buffer: Buffer, inst: StructDefInstantiation) {
    ULEB128.write(buffer, inst.def.value.toInt())
    ULEB128.write(buffer, inst.typeParameters.value.toInt())
}

fun serializeFunctionDefinition(buffer: Buffer, def: FunctionDefinition, version: Int) {
    ULEB128.write(buffer, def.function.value.toInt())

    var flags = 0
    if (version < 5) {
        val visibilityValue = if (def.visibility == Visibility.Public && def.isEntry) {
            @Suppress("DEPRECATION")
            Visibility.DEPRECATED_SCRIPT
        } else {
            def.visibility.value
        }
        buffer.writeByte(visibilityValue.toByte())
    } else {
        buffer.writeByte(def.visibility.value.toByte())
        if (def.isEntry) {
            flags = flags or FunctionDefinition.ENTRY
        }
    }

    if (def.code == null) {
        flags = flags or FunctionDefinition.NATIVE
    }
    buffer.writeByte(flags.toByte())

    ULEB128.write(buffer, def.acquiresGlobalResources.size)
    def.acquiresGlobalResources.forEach { ULEB128.write(buffer, it.value.toInt()) }

    def.code?.let { serializeCodeUnit(buffer, it, version) }
}

fun serializeFieldHandle(buffer: Buffer, handle: FieldHandle) {
    ULEB128.write(buffer, handle.owner.value.toInt())
    ULEB128.write(buffer, handle.field.toInt())
}

fun serializeFieldInstantiation(buffer: Buffer, inst: FieldInstantiation) {
    ULEB128.write(buffer, inst.handle.value.toInt())
    ULEB128.write(buffer, inst.typeParameters.value.toInt())
}

fun serializeMetadata(buffer: Buffer, meta: Metadata) {
    ULEB128.write(buffer, meta.key.size)
    buffer.write(meta.key)
    ULEB128.write(buffer, meta.value.size)
    buffer.write(meta.value)
}

fun serializeAbilitySet(buffer: Buffer, abilitySet: AbilitySet) {
    ULEB128.write(buffer, abilitySet.value.toInt())
}

fun serializeAbilitySets(buffer: Buffer, sets: List<AbilitySet>, version: Int) {
    ULEB128.write(buffer, sets.size)
    sets.forEach { serializeAbilitySet(buffer, it) }
}

fun serializeDatatypeTyParameters(buffer: Buffer, params: List<DatatypeTyParameter>, version: Int) {
    ULEB128.write(buffer, params.size)
    params.forEach { param ->
        serializeAbilitySet(buffer, param.constraints)
        if (version >= 3) {
            ULEB128.write(buffer, if (param.isPhantom) 1 else 0)
        }
    }
}

fun serializeCodeUnit(buffer: Buffer, codeUnit: CodeUnit, version: Int) {
    ULEB128.write(buffer, codeUnit.locals.value.toInt())
    ULEB128.write(buffer, codeUnit.code.size)
    codeUnit.code.forEach { serializeBytecode(buffer, it) }
    if (version >= 7) {
        ULEB128.write(buffer, codeUnit.jumpTables.size)
        codeUnit.jumpTables.forEach { serializeJumpTable(buffer, it) }
    }
}

fun serializeJumpTable(buffer: Buffer, jumpTable: VariantJumpTable) {
    ULEB128.write(buffer, jumpTable.headEnum.value.toInt())
    ULEB128.write(buffer, jumpTable.jumpTable.size)
    buffer.writeByte(0x1)
    jumpTable.jumpTable.forEach { ULEB128.write(buffer, it.toInt()) }
}

fun serializeSignatureToken(buffer: Buffer, token: SignatureToken) {
    val stack = mutableListOf(token)
    while (stack.isNotEmpty()) {
        val current = stack.removeLast()
        when (current) {
            is SignatureToken.Bool -> buffer.writeByte(SerializedType.BOOL.toByte())
            is SignatureToken.U8 -> buffer.writeByte(SerializedType.U8.toByte())
            is SignatureToken.U16 -> buffer.writeByte(SerializedType.U16.toByte())
            is SignatureToken.U32 -> buffer.writeByte(SerializedType.U32.toByte())
            is SignatureToken.U64 -> buffer.writeByte(SerializedType.U64.toByte())
            is SignatureToken.U128 -> buffer.writeByte(SerializedType.U128.toByte())
            is SignatureToken.U256 -> buffer.writeByte(SerializedType.U256.toByte())
            is SignatureToken.Address -> buffer.writeByte(SerializedType.ADDRESS.toByte())
            is SignatureToken.Signer -> buffer.writeByte(SerializedType.SIGNER.toByte())
            is SignatureToken.Vector -> {
                buffer.writeByte(SerializedType.VECTOR.toByte())
                stack.add(current.elementType)
            }

            is SignatureToken.Reference -> {
                buffer.writeByte(SerializedType.REFERENCE.toByte())
                stack.add(current.referencedType)
            }

            is SignatureToken.MutableReference -> {
                buffer.writeByte(SerializedType.MUTABLE_REFERENCE.toByte())
                stack.add(current.referencedType)
            }

            is SignatureToken.TypeParameter -> {
                buffer.writeByte(SerializedType.TYPE_PARAMETER.toByte())
                ULEB128.write(buffer, current.index.value.toInt())
            }

            is SignatureToken.Datatype -> {
                buffer.writeByte(SerializedType.STRUCT.toByte())
                ULEB128.write(buffer, current.index.value.toInt())
            }

            is SignatureToken.DatatypeInstantiation -> {
                buffer.writeByte(SerializedType.DATATYPE_INST.toByte())
                ULEB128.write(buffer, current.index.value.toInt())
                ULEB128.write(buffer, current.typeArgs.size)
                // Add type args to the stack in reverse order for correct processing
                stack.addAll(current.typeArgs.reversed())
            }
        }
    }
}

/**
 * Serializes a single Bytecode instruction and its operands to the buffer.
 */
fun serializeBytecode(buffer: Buffer, bytecode: Bytecode) {
    when (bytecode) {
        is Bytecode.Pop -> buffer.writeByte(Opcodes.POP.toByte())
        is Bytecode.Ret -> buffer.writeByte(Opcodes.RET.toByte())
        is Bytecode.BrTrue -> {
            buffer.writeByte(Opcodes.BR_TRUE.toByte()); ULEB128.write(buffer, bytecode.offset.toInt())
        }

        is Bytecode.BrFalse -> {
            buffer.writeByte(Opcodes.BR_FALSE.toByte()); ULEB128.write(buffer, bytecode.offset.toInt())
        }

        is Bytecode.Branch -> {
            buffer.writeByte(Opcodes.BRANCH.toByte()); ULEB128.write(buffer, bytecode.offset.toInt())
        }

        is Bytecode.LdU8 -> {
            buffer.writeByte(Opcodes.LD_U8.toByte()); buffer.writeByte(bytecode.value.toByte())
        }

        is Bytecode.LdU16 -> {
            buffer.writeByte(Opcodes.LD_U16.toByte()); buffer.writeShortLe(bytecode.value.toShort())
        }

        is Bytecode.LdU32 -> {
            buffer.writeByte(Opcodes.LD_U32.toByte()); buffer.writeIntLe(bytecode.value.toInt())
        }

        is Bytecode.LdU64 -> {
            buffer.writeByte(Opcodes.LD_U64.toByte()); buffer.writeLongLe(bytecode.value.toLong())
        }

        is Bytecode.LdU128 -> {
            buffer.writeByte(Opcodes.LD_U128.toByte())
            val bytes = bytecode.value.toByteArray().reversedArray()
            buffer.write(bytes)
            repeat(16 - bytes.size) { buffer.writeByte(0) }
        }

        is Bytecode.LdU256 -> {
            buffer.writeByte(Opcodes.LD_U256.toByte())
            val bytes = bytecode.value.toByteArray().reversedArray()
            buffer.write(bytes)
            repeat(32 - bytes.size) { buffer.writeByte(0) }
        }

        is Bytecode.LdConst -> {
            buffer.writeByte(Opcodes.LD_CONST.toByte()); ULEB128.write(buffer, bytecode.index.value.toInt())
        }

        is Bytecode.LdTrue -> buffer.writeByte(Opcodes.LD_TRUE.toByte())
        is Bytecode.LdFalse -> buffer.writeByte(Opcodes.LD_FALSE.toByte())
        is Bytecode.CopyLoc -> {
            buffer.writeByte(Opcodes.COPY_LOC.toByte()); ULEB128.write(buffer, bytecode.index.toInt())
        }

        is Bytecode.MoveLoc -> {
            buffer.writeByte(Opcodes.MOVE_LOC.toByte()); ULEB128.write(buffer, bytecode.index.toInt())
        }

        is Bytecode.StLoc -> {
            buffer.writeByte(Opcodes.ST_LOC.toByte()); ULEB128.write(buffer, bytecode.index.toInt())
        }

        is Bytecode.Call -> {
            buffer.writeByte(Opcodes.CALL.toByte()); ULEB128.write(buffer, bytecode.index.value.toInt())
        }

        is Bytecode.CallGeneric -> {
            buffer.writeByte(Opcodes.CALL_GENERIC.toByte()); ULEB128.write(buffer, bytecode.index.value.toInt())
        }

        is Bytecode.Pack -> {
            buffer.writeByte(Opcodes.PACK.toByte()); ULEB128.write(buffer, bytecode.index.value.toInt())
        }

        is Bytecode.PackGeneric -> {
            buffer.writeByte(Opcodes.PACK_GENERIC.toByte()); ULEB128.write(buffer, bytecode.index.value.toInt())
        }

        is Bytecode.Unpack -> {
            buffer.writeByte(Opcodes.UNPACK.toByte()); ULEB128.write(buffer, bytecode.index.value.toInt())
        }

        is Bytecode.UnpackGeneric -> {
            buffer.writeByte(Opcodes.UNPACK_GENERIC.toByte()); ULEB128.write(buffer, bytecode.index.value.toInt())
        }

        is Bytecode.ReadRef -> buffer.writeByte(Opcodes.READ_REF.toByte())
        is Bytecode.WriteRef -> buffer.writeByte(Opcodes.WRITE_REF.toByte())
        is Bytecode.FreezeRef -> buffer.writeByte(Opcodes.FREEZE_REF.toByte())
        is Bytecode.MutBorrowLoc -> {
            buffer.writeByte(Opcodes.MUT_BORROW_LOC.toByte()); ULEB128.write(buffer, bytecode.index.toInt())
        }

        is Bytecode.ImmBorrowLoc -> {
            buffer.writeByte(Opcodes.IMM_BORROW_LOC.toByte()); ULEB128.write(buffer, bytecode.index.toInt())
        }

        is Bytecode.MutBorrowField -> {
            buffer.writeByte(Opcodes.MUT_BORROW_FIELD.toByte()); ULEB128.write(buffer, bytecode.index.value.toInt())
        }

        is Bytecode.MutBorrowFieldGeneric -> {
            buffer.writeByte(Opcodes.MUT_BORROW_FIELD_GENERIC.toByte()); ULEB128.write(
                buffer,
                bytecode.index.value.toInt()
            )
        }

        is Bytecode.ImmBorrowField -> {
            buffer.writeByte(Opcodes.IMM_BORROW_FIELD.toByte()); ULEB128.write(buffer, bytecode.index.value.toInt())
        }

        is Bytecode.ImmBorrowFieldGeneric -> {
            buffer.writeByte(Opcodes.IMM_BORROW_FIELD_GENERIC.toByte()); ULEB128.write(
                buffer,
                bytecode.index.value.toInt()
            )
        }

        is Bytecode.Add -> buffer.writeByte(Opcodes.ADD.toByte())
        is Bytecode.Sub -> buffer.writeByte(Opcodes.SUB.toByte())
        is Bytecode.Mul -> buffer.writeByte(Opcodes.MUL.toByte())
        is Bytecode.Mod -> buffer.writeByte(Opcodes.MOD.toByte())
        is Bytecode.Div -> buffer.writeByte(Opcodes.DIV.toByte())
        is Bytecode.BitOr -> buffer.writeByte(Opcodes.BIT_OR.toByte())
        is Bytecode.BitAnd -> buffer.writeByte(Opcodes.BIT_AND.toByte())
        is Bytecode.Xor -> buffer.writeByte(Opcodes.XOR.toByte())
        is Bytecode.Shl -> buffer.writeByte(Opcodes.SHL.toByte())
        is Bytecode.Shr -> buffer.writeByte(Opcodes.SHR.toByte())
        is Bytecode.Or -> buffer.writeByte(Opcodes.OR.toByte())
        is Bytecode.And -> buffer.writeByte(Opcodes.AND.toByte())
        is Bytecode.Not -> buffer.writeByte(Opcodes.NOT.toByte())
        is Bytecode.Eq -> buffer.writeByte(Opcodes.EQ.toByte())
        is Bytecode.Neq -> buffer.writeByte(Opcodes.NEQ.toByte())
        is Bytecode.Lt -> buffer.writeByte(Opcodes.LT.toByte())
        is Bytecode.Gt -> buffer.writeByte(Opcodes.GT.toByte())
        is Bytecode.Le -> buffer.writeByte(Opcodes.LE.toByte())
        is Bytecode.Ge -> buffer.writeByte(Opcodes.GE.toByte())
        is Bytecode.Abort -> buffer.writeByte(Opcodes.ABORT.toByte())
        is Bytecode.Nop -> buffer.writeByte(Opcodes.NOP.toByte())
        is Bytecode.CastU8 -> buffer.writeByte(Opcodes.CAST_U8.toByte())
        is Bytecode.CastU16 -> buffer.writeByte(Opcodes.CAST_U16.toByte())
        is Bytecode.CastU32 -> buffer.writeByte(Opcodes.CAST_U32.toByte())
        is Bytecode.CastU64 -> buffer.writeByte(Opcodes.CAST_U64.toByte())
        is Bytecode.CastU128 -> buffer.writeByte(Opcodes.CAST_U128.toByte())
        is Bytecode.CastU256 -> buffer.writeByte(Opcodes.CAST_U256.toByte())
        is Bytecode.VecPack -> {
            buffer.writeByte(Opcodes.VEC_PACK.toByte())
            ULEB128.write(buffer, bytecode.sigIndex.value.toInt())
            ULEB128.write(buffer, bytecode.num.toLong())
        }

        is Bytecode.VecLen -> {
            buffer.writeByte(Opcodes.VEC_LEN.toByte()); ULEB128.write(buffer, bytecode.sigIndex.value.toInt())
        }

        is Bytecode.VecImmBorrow -> {
            buffer.writeByte(Opcodes.VEC_IMM_BORROW.toByte()); ULEB128.write(buffer, bytecode.sigIndex.value.toInt())
        }

        is Bytecode.VecMutBorrow -> {
            buffer.writeByte(Opcodes.VEC_MUT_BORROW.toByte()); ULEB128.write(buffer, bytecode.sigIndex.value.toInt())
        }

        is Bytecode.VecPushBack -> {
            buffer.writeByte(Opcodes.VEC_PUSH_BACK.toByte()); ULEB128.write(buffer, bytecode.sigIndex.value.toInt())
        }

        is Bytecode.VecPopBack -> {
            buffer.writeByte(Opcodes.VEC_POP_BACK.toByte()); ULEB128.write(buffer, bytecode.sigIndex.value.toInt())
        }

        is Bytecode.VecUnpack -> {
            buffer.writeByte(Opcodes.VEC_UNPACK.toByte())
            ULEB128.write(buffer, bytecode.sigIndex.value.toInt())
            ULEB128.write(buffer, bytecode.num.toLong())
        }

        is Bytecode.VecSwap -> {
            buffer.writeByte(Opcodes.VEC_SWAP.toByte()); ULEB128.write(buffer, bytecode.sigIndex.value.toInt())
        }

        is Bytecode.PackVariant -> {
            buffer.writeByte(Opcodes.PACK_VARIANT.toByte()); ULEB128.write(buffer, bytecode.index.value.toInt())
        }

        is Bytecode.PackVariantGeneric -> {
            buffer.writeByte(Opcodes.PACK_VARIANT_GENERIC.toByte()); ULEB128.write(buffer, bytecode.index.value.toInt())
        }

        is Bytecode.UnpackVariant -> {
            buffer.writeByte(Opcodes.UNPACK_VARIANT.toByte()); ULEB128.write(buffer, bytecode.index.value.toInt())
        }

        is Bytecode.UnpackVariantImmRef -> {
            buffer.writeByte(Opcodes.UNPACK_VARIANT_IMM_REF.toByte()); ULEB128.write(
                buffer,
                bytecode.index.value.toInt()
            )
        }

        is Bytecode.UnpackVariantMutRef -> {
            buffer.writeByte(Opcodes.UNPACK_VARIANT_MUT_REF.toByte()); ULEB128.write(
                buffer,
                bytecode.index.value.toInt()
            )
        }

        is Bytecode.UnpackVariantGeneric -> {
            buffer.writeByte(Opcodes.UNPACK_VARIANT_GENERIC.toByte()); ULEB128.write(
                buffer,
                bytecode.index.value.toInt()
            )
        }

        is Bytecode.UnpackVariantGenericImmRef -> {
            buffer.writeByte(Opcodes.UNPACK_VARIANT_GENERIC_IMM_REF.toByte()); ULEB128.write(
                buffer,
                bytecode.index.value.toInt()
            )
        }

        is Bytecode.UnpackVariantGenericMutRef -> {
            buffer.writeByte(Opcodes.UNPACK_VARIANT_GENERIC_MUT_REF.toByte()); ULEB128.write(
                buffer,
                bytecode.index.value.toInt()
            )
        }

        is Bytecode.VariantSwitch -> {
            buffer.writeByte(Opcodes.VARIANT_SWITCH.toByte()); ULEB128.write(buffer, bytecode.index.value.toInt())
        }
        // Deprecated Bytecodes
        is Bytecode.ExistsDeprecated -> {
            buffer.writeByte(Opcodes.EXISTS_DEPRECATED.toByte()); ULEB128.write(buffer, bytecode.index.value.toInt())
        }

        is Bytecode.ExistsGenericDeprecated -> {
            buffer.writeByte(Opcodes.EXISTS_GENERIC_DEPRECATED.toByte()); ULEB128.write(
                buffer,
                bytecode.index.value.toInt()
            )
        }

        is Bytecode.MoveFromDeprecated -> {
            buffer.writeByte(Opcodes.MOVE_FROM_DEPRECATED.toByte()); ULEB128.write(buffer, bytecode.index.value.toInt())
        }

        is Bytecode.MoveFromGenericDeprecated -> {
            buffer.writeByte(Opcodes.MOVE_FROM_GENERIC_DEPRECATED.toByte()); ULEB128.write(
                buffer,
                bytecode.index.value.toInt()
            )
        }

        is Bytecode.MoveToDeprecated -> {
            buffer.writeByte(Opcodes.MOVE_TO_DEPRECATED.toByte()); ULEB128.write(buffer, bytecode.index.value.toInt())
        }

        is Bytecode.MoveToGenericDeprecated -> {
            buffer.writeByte(Opcodes.MOVE_TO_GENERIC_DEPRECATED.toByte()); ULEB128.write(
                buffer,
                bytecode.index.value.toInt()
            )
        }

        is Bytecode.MutBorrowGlobalDeprecated -> {
            buffer.writeByte(Opcodes.MUT_BORROW_GLOBAL_DEPRECATED.toByte()); ULEB128.write(
                buffer,
                bytecode.index.value.toInt()
            )
        }

        is Bytecode.MutBorrowGlobalGenericDeprecated -> {
            buffer.writeByte(Opcodes.MUT_BORROW_GLOBAL_GENERIC_DEPRECated.toByte()); ULEB128.write(
                buffer,
                bytecode.index.value.toInt()
            )
        }

        is Bytecode.ImmBorrowGlobalDeprecated -> {
            buffer.writeByte(Opcodes.IMM_BORROW_GLOBAL_DEPRECATED.toByte()); ULEB128.write(
                buffer,
                bytecode.index.value.toInt()
            )
        }

        is Bytecode.ImmBorrowGlobalGenericDeprecated -> {
            buffer.writeByte(Opcodes.IMM_BORROW_GLOBAL_GENERIC_DEPRECATED.toByte()); ULEB128.write(
                buffer,
                bytecode.index.value.toInt()
            )
        }
    }
}

fun serializeEnumDefinition(buffer: Buffer, def: EnumDefinition) {
    ULEB128.write(buffer, def.enumHandle.value.toInt())
    buffer.writeByte(0x2) // SerializedEnumFlag::DECLARED
    ULEB128.write(buffer, def.variants.size)
    def.variants.forEach { serializeVariantDefinition(buffer, it) }
}

fun serializeVariantDefinition(buffer: Buffer, def: VariantDefinition) {
    ULEB128.write(buffer, def.variantName.value.toInt())
    ULEB128.write(buffer, def.fields.size)
    def.fields.forEach { field ->
        ULEB128.write(buffer, field.name.value.toInt())
        serializeSignatureToken(buffer, field.signature.value)
    }
}

fun serializeEnumDefInstantiation(buffer: Buffer, inst: EnumDefInstantiation) {
    ULEB128.write(buffer, inst.def.value.toInt())
    ULEB128.write(buffer, inst.typeParameters.value.toInt())
}

fun serializeVariantHandle(buffer: Buffer, handle: VariantHandle) {
    ULEB128.write(buffer, handle.enumDef.value.toInt())
    ULEB128.write(buffer, handle.variant.toInt())
}

fun serializeVariantInstantiationHandle(buffer: Buffer, handle: VariantInstantiationHandle) {
    ULEB128.write(buffer, handle.enumDef.value.toInt())
    ULEB128.write(buffer, handle.variant.toInt())
}
