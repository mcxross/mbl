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

import kotlin.jvm.JvmInline

@JvmInline
value class Identifier private constructor(val value: String) {
    companion object {
        fun of(value: String): Identifier {
            require(value.isNotBlank()) { "Identifier cannot be blank" }
            return Identifier(value)
        }
    }
}
