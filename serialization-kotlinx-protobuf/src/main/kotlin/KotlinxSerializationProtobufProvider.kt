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
@file:OptIn(ExperimentalSerializationApi::class)

package de.nycode.rabbitkt.serialization.kotlinx.protobuf

import de.nycode.rabbitkt.serialization.SerializationPluginConfiguration
import de.nycode.rabbitkt.serialization.SerializationProvider
import de.nycode.rabbitkt.serialization.kotlinx.core.KotlinRabbitSerializationRepository
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoBufBuilder
import kotlin.reflect.KClass

public class KotlinxSerializationProtobufProvider(private val protobuf: ProtoBuf) : SerializationProvider {

    override fun <T : Any> serialize(value: T, type: KClass<T>): ByteArray {
        val serializer = KotlinRabbitSerializationRepository.getSerializer(type)
        return protobuf.encodeToByteArray(serializer, value)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun <T : Any> deserialize(body: ByteArray, type: KClass<T>): T {
        val serializer = KotlinRabbitSerializationRepository.getSerializer(type)
        return protobuf.decodeFromByteArray(serializer, body)
    }
}

public fun SerializationPluginConfiguration.protobuf(builder: ProtoBufBuilder.() -> Unit = {}) {
    val protobuf = ProtoBuf(builderAction = builder)
    provider = KotlinxSerializationProtobufProvider(protobuf)
}
