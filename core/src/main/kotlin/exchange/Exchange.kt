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

package de.nycode.rabbitkt.exchange

import de.nycode.rabbitkt.KotlinRabbitClient
import de.nycode.rabbitkt.binding.ExchangeBinding
import de.nycode.rabbitkt.binding.QueueBinding
import de.nycode.rabbitkt.queue.Queue
import de.nycode.rabbitkt.sender.CoroutineSenderImpl
import kotlinx.coroutines.flow.Flow
import reactor.rabbitmq.OutboundMessage

/**
 * Represents an exchange in a RabbitMQ Broker.
 */
public data class Exchange internal constructor(
    val name: String,
    val type: ExchangeType,
    val sender: CoroutineSenderImpl,
    val client: KotlinRabbitClient
) {
    /**
     * Bind this exchange to another exchange.
     * Every message sent to this exchange will also get sent to the other
     * exchange. Using the [routingKey] this allows richer routing.
     * @param destination the exchange to route the messages to
     * @param routingKey the routing key used for routing.
     */
    public suspend fun bindTo(destination: Exchange, routingKey: String = ""): ExchangeBinding {
        sender.bindExchange(name, routingKey, destination.name)
        return ExchangeBinding(this, destination, routingKey, sender)
    }

    /**
     * Bind this exchange to a [Queue].
     * Messages sent to this exchange are able to get consumed in the [destination] queue
     * when sent with a matching [routingKey].
     * @param destination the queue where to route the messages to
     * @param routingKey the routing key used for routing.
     */
    public suspend fun bindTo(destination: Queue, routingKey: String = ""): QueueBinding {
        sender.bindQueue(name, routingKey, destination.name)
        return QueueBinding(this, destination, routingKey, sender)
    }

    /**
     * Delete this exchange
     */
    public suspend fun delete(ifUnused: Boolean = true) {
        sender.deleteExchange(name, type, ifUnused)
    }

    public suspend fun send(vararg messages: OutboundMessage) {
        sender.send(*messages)
    }

    public suspend fun send(messages: Flow<OutboundMessage>) {
        sender.sendFlow(messages)
    }
}
