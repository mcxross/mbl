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
package xyz.mcxross.mbl.model;

enum class IndexKind {
    ModuleHandle,
    DatatypeHandle,
    FunctionHandle,
    FieldHandle,
    FriendDeclaration,
    FunctionInstantiation,
    FieldInstantiation,
    StructDefinition,
    StructDefInstantiation,
    FunctionDefinition,
    FieldDefinition,
    Signature,
    Identifier,
    AddressIdentifier,
    ConstantPool,
    LocalPool,
    CodeDefinition,
    TypeParameter,
    MemberCount,
    EnumDefinition,
    EnumDefInstantiation,
    VariantHandle,
    VariantInstantiationHandle,
    VariantJumpTable,
    VariantTag;

    companion object {
        val variants: List<IndexKind> = listOf(
            ModuleHandle,
            DatatypeHandle,
            FunctionHandle,
            FieldHandle,
            FriendDeclaration,
            StructDefInstantiation,
            FunctionInstantiation,
            FieldInstantiation,
            StructDefinition,
            FunctionDefinition,
            FieldDefinition,
            Signature,
            Identifier,
            ConstantPool,
            LocalPool,
            CodeDefinition,
            TypeParameter,
            MemberCount,
            EnumDefinition,
            EnumDefInstantiation,
            VariantHandle,
            VariantInstantiationHandle,
            VariantJumpTable,
            VariantTag
        )
    }
}