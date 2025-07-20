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

import xyz.mcxross.mbl.format.DatatypeHandleIndex
import xyz.mcxross.mbl.format.SignatureToken

private const val SIGNATURE_TOKEN_DEPTH_MAX = 256

 object SerializedType {
    const val BOOL: UByte = 0x1u
    const val U8: UByte = 0x2u
    const val U64: UByte = 0x3u
    const val U128: UByte = 0x4u
    const val ADDRESS: UByte = 0x5u
    const val REFERENCE: UByte = 0x6u
    const val MUTABLE_REFERENCE: UByte = 0x7u
    const val STRUCT: UByte = 0x8u
    const val TYPE_PARAMETER: UByte = 0x9u
    const val VECTOR: UByte = 0xAu
    const val DATATYPE_INST: UByte = 0xBu
    const val SIGNER: UByte = 0xCu
    const val U16: UByte = 0xDu
    const val U32: UByte = 0xEu
    const val U256: UByte = 0xFu
}

 sealed class TypeBuilder {
    data class Saturated(val token: SignatureToken) : TypeBuilder()
    data object VectorBuilder : TypeBuilder()
    data object ReferenceBuilder : TypeBuilder()
    data object MutableReferenceBuilder : TypeBuilder()
    data class DatatypeInstBuilder(
        val handleIndex: DatatypeHandleIndex,
        val arity: Int,
        val typeArgs: MutableList<SignatureToken> = mutableListOf()
    ) : TypeBuilder()

    fun apply(token: SignatureToken): TypeBuilder = when (this) {
        is VectorBuilder -> Saturated(SignatureToken.Vector(token))
        is ReferenceBuilder -> Saturated(SignatureToken.Reference(token))
        is MutableReferenceBuilder -> Saturated(SignatureToken.MutableReference(token))
        is DatatypeInstBuilder -> {
            this.typeArgs.add(token)
            if (this.typeArgs.size >= this.arity) {
                Saturated(SignatureToken.DatatypeInstantiation(this.handleIndex, this.typeArgs))
            } else {
                this
            }
        }
        is Saturated -> throw IllegalStateException("Cannot apply a token to an already saturated type.")
    }
}