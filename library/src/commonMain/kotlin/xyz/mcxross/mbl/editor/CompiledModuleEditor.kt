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
package xyz.mcxross.mbl.editor

import xyz.mcxross.mbl.format.CompiledModule
import xyz.mcxross.mbl.format.IdentifierIndex
import xyz.mcxross.mbl.model.*

class CompiledModuleEditor(module: CompiledModule) {

    private var version = module.version
    private var publishable = module.publishable
    private var selfModuleHandleIdx = module.selfModuleHandleIdx
    private val moduleHandles = module.moduleHandles.toMutableList()
    private val datatypeHandles = module.datatypeHandles.toMutableList()
    private val functionHandles = module.functionHandles.toMutableList()
    private val fieldHandles = module.fieldHandles.toMutableList()
    private val friendDecls = module.friendDecls.toMutableList()
    private val structDefInstantiations = module.structDefInstantiations.toMutableList()
    private val functionInstantiations = module.functionInstantiations.toMutableList()
    private val fieldInstantiations = module.fieldInstantiations.toMutableList()
    private val signatures = module.signatures.toMutableList()
    private val identifiers = module.identifiers.toMutableList()
    private val addressIdentifiers = module.addressIdentifiers.toMutableList()
    private val constantPool = module.constantPool.toMutableList()
    private val metadata = module.metadata.toMutableList()
    private val structDefs = module.structDefs.toMutableList()
    private val functionDefs = module.functionDefs.toMutableList()
    private val enumDefs = module.enumDefs.toMutableList()
    private val enumDefInstantiations = module.enumDefInstantiations.toMutableList()
    private val variantHandles = module.variantHandles.toMutableList()
    private val variantInstantiationHandles = module.variantInstantiationHandles.toMutableList()

    fun renameIdentifier(oldName: String, newName: String): CompiledModuleEditor {
        val oldIdentifier = Identifier.of(oldName)
        val newIdentifier = Identifier.of(newName)
        val index = identifiers.indexOf(oldIdentifier)

        if (index != -1) {
            identifiers[index] = newIdentifier
        } else {
            println("Warning: Identifier '$oldName' not found for renaming.")
        }
        return this
    }

    fun replaceConstantString(oldValue: String, newValue: String): CompiledModuleEditor {
        val oldBytes = oldValue.encodeToByteArray()

        // Find the constant by checking the content *after* the length prefix.
        val index = constantPool.indexOfFirst { constant ->
            // Ensure the constant is long enough and its content matches.
            constant.data.size > oldBytes.size &&
                    constant.data.drop(1).toByteArray().contentEquals(oldBytes)
        }

        if (index != -1) {
            // Create the new byte array, making sure to prefix it with its own length.
            val newBytes = newValue.encodeToByteArray()
            val newLengthPrefixedBytes = byteArrayOf(newBytes.size.toByte()) + newBytes

            val oldConstant = constantPool[index]
            constantPool[index] = oldConstant.copy(data = newLengthPrefixedBytes)
        } else {
            println("Warning: Constant string '$oldValue' not found.")
        }
        return this
    }

    fun setFunctionVisibility(functionName: String, newVisibility: Visibility): CompiledModuleEditor {
        val targetIdentifier = Identifier.of(functionName)
        val identifierIndex = IdentifierIndex(identifiers.indexOf(targetIdentifier).toUShort())

        if (identifierIndex.value.toInt() == -1) {
            println("Warning: Identifier for function '$functionName' not found.")
            return this
        }

        val funcHandleIndex = functionHandles.indexOfFirst { it.name == identifierIndex }
        if (funcHandleIndex == -1) {
            println("Warning: Function handle for '$functionName' not found.")
            return this
        }

        val funcDefIndex = functionDefs.indexOfFirst { it.function.value.toInt() == funcHandleIndex }
        if (funcDefIndex != -1) {
            val oldDef = functionDefs[funcDefIndex]
            functionDefs[funcDefIndex] = oldDef.copy(visibility = newVisibility)
        } else {
            println("Warning: Function definition for '$functionName' not found.")
        }

        return this
    }

    fun build(): CompiledModule {
        // For simple value replacement, indices remain the same. If you were to add/remove
        // items, you would need to re-calculate all relevant indices here before building.

        return CompiledModule(
            version = this.version,
            publishable = this.publishable,
            selfModuleHandleIdx = this.selfModuleHandleIdx,
            moduleHandles = this.moduleHandles.toList(),
            datatypeHandles = this.datatypeHandles.toList(),
            functionHandles = this.functionHandles.toList(),
            fieldHandles = this.fieldHandles.toList(),
            friendDecls = this.friendDecls.toList(),
            structDefInstantiations = this.structDefInstantiations.toList(),
            functionInstantiations = this.functionInstantiations.toList(),
            fieldInstantiations = this.fieldInstantiations.toList(),
            signatures = this.signatures.toList(),
            identifiers = this.identifiers.toList(),
            addressIdentifiers = this.addressIdentifiers.toList(),
            constantPool = this.constantPool.toList(),
            metadata = this.metadata.toList(),
            structDefs = this.structDefs.toList(),
            functionDefs = this.functionDefs.toList(),
            enumDefs = this.enumDefs.toList(),
            enumDefInstantiations = this.enumDefInstantiations.toList(),
            variantHandles = this.variantHandles.toList(),
            variantInstantiationHandles = this.variantInstantiationHandles.toList()
        )
    }
}

fun CompiledModule.edit(): CompiledModuleEditor {
    return CompiledModuleEditor(this)
}

fun CompiledModule.edit(block: CompiledModuleEditor.() -> Unit): CompiledModule {
    val editor = CompiledModuleEditor(this)
    editor.block()
    return editor.build()
}