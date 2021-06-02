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
package de.nycode.rabbitkt.serialization.kotlinx.json

import de.nycode.rabbitkt.annotations.KotlinRabbitInternals
import de.nycode.rabbitkt.serialization.SerializationProvider
import de.nycode.rabbitkt.serialization.kotlinx.core.KotlinRabbitSerializationRepository
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass

public class KotlinxSerializationJsonProvider(private val json: Json) : SerializationProvider {

    override fun <T : Any> serialize(value: T, type: KClass<T>): ByteArray {
        val serializer = KotlinRabbitSerializationRepository.getSerializer(type)
        return json.encodeToString(serializer, value).encodeToByteArray()
    }

    override fun <T : Any> deserialize(body: ByteArray, type: KClass<T>): T {
        val serializer = KotlinRabbitSerializationRepository.getSerializer(type)
        return json.decodeFromString(serializer, body.decodeToString())
    }
}
