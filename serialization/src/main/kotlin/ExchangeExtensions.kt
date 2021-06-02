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

import de.nycode.rabbitkt.KotlinRabbitClient
import de.nycode.rabbitkt.annotations.KotlinRabbitInternals
import de.nycode.rabbitkt.exchange.Exchange
import reactor.rabbitmq.OutboundMessage

public suspend inline fun <reified T : Any> Exchange.send(message: T, routingKey: String = "") {
    val provider = client.serializationProvider
    val serializedMessage = provider.serialize(message)
    send(OutboundMessage(name, routingKey, serializedMessage))
}

@KotlinRabbitInternals
@PublishedApi
internal val KotlinRabbitClient.serializationProvider: SerializationProvider
    get() =
        requireNotNull(getSerializationPlugin().provider) { "There is no serialization provider installed!" }
