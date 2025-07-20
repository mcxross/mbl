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
package xyz.mcxross.mbl.exception

import xyz.mcxross.mbl.format.CodeOffset
import xyz.mcxross.mbl.format.TableIndex
import xyz.mcxross.mbl.model.AccountAddress
import xyz.mcxross.mbl.model.FunctionDefinitionIndex
import xyz.mcxross.mbl.model.Identifier
import xyz.mcxross.mbl.model.IndexKind
import xyz.mcxross.mbl.model.StatusCode
import kotlin.jvm.JvmInline

data class VMError_(
    val majorStatus: StatusCode,
    val subStatus: ULong?,
    val message: String?,
    val execState: ExecutionState?,
    val location: Location,
    val indices: List<Pair<IndexKind, TableIndex>>,
    val offsets: List<Pair<FunctionDefinitionIndex, CodeOffset>>
)

@JvmInline
value class VMError(val inner: VMError_)

data class ModuleId(val address: AccountAddress, val name: Identifier)

data class ExecutionState(val stackTrace: List<Triple<ModuleId, FunctionDefinitionIndex, CodeOffset>>) {
    companion object {
        fun new(stackTrace: List<Triple<ModuleId, FunctionDefinitionIndex, CodeOffset>>): ExecutionState {
            return ExecutionState(stackTrace)
        }
    }
}

sealed class Location {
    object Undefined : Location()
    data class Module(val moduleId: ModuleId) : Location()
}

data class PartialVMError_(
    var majorStatus: StatusCode,
    var subStatus: ULong? = null,
    var message: String? = null,
    var execState: ExecutionState? = null,
    val indices: MutableList<Pair<IndexKind, TableIndex>> = mutableListOf(),
    val offsets: MutableList<Pair<FunctionDefinitionIndex, CodeOffset>> = mutableListOf()
)

class PartialVMError(private val inner: PartialVMError_) {

    companion object {
        fun new(majorStatus: StatusCode): PartialVMError {
            return PartialVMError(
                PartialVMError_(
                    majorStatus = majorStatus
                )
            )
        }
    }

    fun majorStatus(): StatusCode = inner.majorStatus

    fun withSubStatus(subStatus: ULong): PartialVMError {
        check(inner.subStatus == null) { "subStatus already set" }
        inner.subStatus = subStatus
        return this
    }

    fun withMessage(message: String): PartialVMError {
        check(inner.message == null) { "message already set" }
        inner.message = message
        return this
    }

    fun withExecState(execState: ExecutionState): PartialVMError {
        check(inner.execState == null) { "execState already set" }
        inner.execState = execState
        return this
    }

    fun atIndex(kind: IndexKind, index: TableIndex): PartialVMError {
        inner.indices.add(kind to index)
        return this
    }

    fun atIndices(additionalIndices: List<Pair<IndexKind, TableIndex>>): PartialVMError {
        inner.indices.addAll(additionalIndices)
        return this
    }

    fun atCodeOffset(function: FunctionDefinitionIndex, offset: CodeOffset): PartialVMError {
        inner.offsets.add(function to offset)
        return this
    }

    fun atCodeOffsets(additionalOffsets: List<Pair<FunctionDefinitionIndex, CodeOffset>>): PartialVMError {
        inner.offsets.addAll(additionalOffsets)
        return this
    }

    fun appendMessageWithSeparator(separator: Char, additionalMessage: String): PartialVMError {
        if (inner.message == null) {
            inner.message = additionalMessage
        } else {
            if (inner.message!!.isNotEmpty()) {
                inner.message += separator
            }
            inner.message += additionalMessage
        }
        return this
    }

    fun allData(): TripleGroup {
        return TripleGroup(
            inner.majorStatus,
            inner.subStatus,
            inner.message,
            inner.execState,
            inner.indices.toList(),
            inner.offsets.toList()
        )
    }

    fun finish(location: Location): VMError {
        return VMError(
            VMError_(
                majorStatus = inner.majorStatus,
                subStatus = inner.subStatus,
                message = inner.message,
                execState = inner.execState,
                location = location,
                indices = inner.indices.toList(),
                offsets = inner.offsets.toList()
            )
        )
    }

    data class TripleGroup(
        val majorStatus: StatusCode,
        val subStatus: ULong?,
        val message: String?,
        val execState: ExecutionState?,
        val indices: List<Pair<IndexKind, TableIndex>>,
        val offsets: List<Pair<FunctionDefinitionIndex, CodeOffset>>
    )
}
