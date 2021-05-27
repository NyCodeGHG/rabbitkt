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

package de.nycode.rabbitkt.sender

import com.rabbitmq.client.AMQP
import de.nycode.rabbitkt.exchange.ExchangeBuilder
import de.nycode.rabbitkt.queue.QueueBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.reactive.asPublisher
import kotlinx.coroutines.reactor.awaitSingle
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import reactor.rabbitmq.BindingSpecification
import reactor.rabbitmq.OutboundMessage
import reactor.rabbitmq.Sender
import java.io.Closeable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
@JvmInline
public value class CoroutineSender(private val sender: Sender) : Closeable {

    private fun declareExchangeReactive(
        name: String,
        builder: ExchangeBuilder.() -> Unit
    ): Mono<AMQP.Exchange.DeclareOk> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return sender.declareExchange(ExchangeBuilder(name).apply(builder).toExchangeSpecification())
    }

    /**
     * Declare an exchange with the specified name.
     * Configure the Exchange with the builder.
     * @param name the name of the exchange
     * @param builder the configuration builder
     * @return The RabbitMQ DeclareOk Result
     */
    public suspend fun declareExchange(
        name: String,
        builder: ExchangeBuilder.() -> Unit = {}
    ): AMQP.Exchange.DeclareOk {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return declareExchangeReactive(name, builder).awaitSingle()
    }

    private fun declareQueueReactive(
        name: String,
        builder: QueueBuilder.() -> Unit
    ): Mono<AMQP.Queue.DeclareOk> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return sender.declareQueue(QueueBuilder(name).apply(builder).toQueueSpecification())
    }

    /**
     * Declare a queue with the specified name.
     * Configure the Queue with the builder.
     * @param name the name of the queue
     * @param builder the configuration builder
     * @return The RabbitMQ DeclareOk Result
     */
    public suspend fun declareQueue(name: String, builder: QueueBuilder.() -> Unit = {}): AMQP.Queue.DeclareOk {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return declareQueueReactive(name, builder).awaitSingle()
    }

    private fun bindExchangeReactive(
        exchangeFrom: String,
        routingKey: String,
        exchangeTo: String
    ): Mono<AMQP.Exchange.BindOk> {
        return sender.bindExchange(
            BindingSpecification.exchangeBinding(exchangeFrom, routingKey, exchangeTo)
        )
    }

    /**
     * Bind an exchange to another exchange.
     * @param exchangeFrom the exchange where the messages come from.
     * @param routingKey the routing key used for this binding.
     * @param exchangeTo the exchange where the messages are going to.
     */
    public suspend fun bindExchange(
        exchangeFrom: String,
        routingKey: String,
        exchangeTo: String
    ): AMQP.Exchange.BindOk {
        return bindExchangeReactive(exchangeFrom, routingKey, exchangeTo).awaitSingle()
    }

    private fun bindQueueReactive(
        exchange: String,
        routingKey: String,
        queue: String
    ): Mono<AMQP.Queue.BindOk> {
        return sender.bindQueue(BindingSpecification.queueBinding(exchange, routingKey, queue))
    }

    /**
     * Bind an exchange to a queue. This specified queue will be able to receive messages from this Exchange.
     * @param exchange the exchange to bind.
     * @param routingKey the routing key used for this binding.
     * @param queue the queue which should receive messages from this exchange.
     */
    public suspend fun bindQueue(
        exchange: String,
        routingKey: String,
        queue: String
    ): AMQP.Queue.BindOk {
        return bindQueueReactive(exchange, routingKey, queue).awaitSingle()
    }

    private fun unbindExchangeReactive(
        exchangeFrom: String,
        routingKey: String,
        exchangeTo: String
    ) =
        sender.unbindExchange(BindingSpecification.exchangeBinding(exchangeFrom, routingKey, exchangeTo))

    /**
     * Unbind an exchange from another Exchange. This is doing the opposite of [bindExchange]
     * as it removes an existing binding between two exchanges.
     * @param exchangeFrom the exchange where the messages come from.
     * @param routingKey the routing key used for this binding.
     * @param exchangeTo the exchange where the messages are going to.
     */
    public suspend fun unbindExchange(
        exchangeFrom: String,
        routingKey: String,
        exchangeTo: String
    ): AMQP.Exchange.UnbindOk {
        return unbindExchangeReactive(exchangeFrom, routingKey, exchangeTo).awaitSingle()
    }

    private fun unbindQueueReactive(
        exchange: String,
        routingKey: String,
        queue: String
    ) = sender.unbindQueue(BindingSpecification.queueBinding(exchange, routingKey, queue))

    /**
     * Unbind an exchange from an existing queue. This is doing the opposite of [bindQueue]
     * as it removes an existing binding between an exchange an a queue.
     * @param exchange the exchange where the messages are coming from.
     * @param routingKey the routing key used in the binding.
     * @param queue the queue where the messages of the binding are going to.
     */
    public suspend fun unbindQueue(
        exchange: String,
        routingKey: String,
        queue: String
    ): AMQP.Queue.UnbindOk = unbindQueueReactive(exchange, routingKey, queue).awaitSingle()

    private fun deleteExchangeReactive(
        name: String,
        ifUnused: Boolean = false,
        builder: ExchangeBuilder.() -> Unit = {}
    ) =
        sender.deleteExchange(ExchangeBuilder(name).apply(builder).toExchangeSpecification(), ifUnused)

    /**
     * Delete an exchange.
     * @param name the name of the exchange
     */
    public suspend fun deleteExchange(
        name: String,
        ifUnused: Boolean,
        builder: ExchangeBuilder.() -> Unit
    ): AMQP.Exchange.DeleteOk = deleteExchangeReactive(name, ifUnused, builder).awaitSingle()

    private fun sendReactive(messages: Publisher<OutboundMessage>) =
        sender.send(messages)

    /**
     * Send a [Flow] of outbound messages.
     * If you just want to send messages, use the [send]
     * @param messages the message flow
     */
    public suspend fun sendFlow(messages: Flow<OutboundMessage>) {
        sendReactive(messages.asPublisher()).awaitSingle()
    }

    /**
     * Send outbound messages.
     * If you want to to send messages via a flow, use [sendFlow].
     * @param messages the messages to send
     */
    public suspend fun send(vararg messages: OutboundMessage) {
        sendReactive(flowOf(*messages).asPublisher()).awaitSingle()
    }

    override fun close(): Unit = sender.close()
}

public val Sender.coroutine: CoroutineSender get() = CoroutineSender(this)
