/*
 *    Copyright 2021 NyCode
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */
package de.nycode.rabbitkt.serialization

import kotlin.reflect.KClass

/**
 * Provider used for serializing Messages using different serialization frameworks/libraries.
 */
public interface SerializationProvider {

    /**
     * Serializes the given [value] to a [ByteArray]
     * @param value the value to serialize
     * @return the serialized value in form of a [ByteArray]
     */
    public fun <T : Any> serialize(value: T, type: KClass<T>): ByteArray

    /**
     * Deserializes the given [body] to an actual value using a serialization framework/library.
     * @param body the body to deserialize
     * @return the deserialized value
     */
    public fun <T : Any> deserialize(body: ByteArray, type: KClass<T>): T

}

public inline fun <reified T : Any> SerializationProvider.serialize(value: T): ByteArray {
    return serialize(value, T::class)
}

public inline fun <reified T : Any> SerializationProvider.deserialize(body: ByteArray): T {
    return deserialize(body, T::class)
}
