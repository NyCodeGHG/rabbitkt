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
import de.nycode.rabbitkt.KotlinRabbitClient
import de.nycode.rabbitkt.exchange.Exchange
import de.nycode.rabbitkt.exchange.ExchangeBuilder
import de.nycode.rabbitkt.exchange.ExchangeType
import de.nycode.rabbitkt.queue.Queue
import de.nycode.rabbitkt.queue.QueueBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.asPublisher
import kotlinx.coroutines.reactor.awaitSingle
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import reactor.rabbitmq.BindingSpecification
import reactor.rabbitmq.OutboundMessage
import reactor.rabbitmq.OutboundMessageResult
import reactor.rabbitmq.Sender
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalContracts::class)
public class CoroutineSenderImpl(private val client: KotlinRabbitClient, private val sender: Sender) : CoroutineSender {

    private fun declareExchangeReactive(
        name: String,
        type: ExchangeType,
        builder: ExchangeBuilder.() -> Unit
    ): Mono<AMQP.Exchange.DeclareOk> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return sender.declareExchange(ExchangeBuilder(name, type).apply(builder).toExchangeSpecification())
    }

    /**
     * Declare an exchange with the specified name.
     * Configure the Exchange with the builder.
     * @param name the name of the exchange
     * @param builder the configuration builder
     * @return the declared [Exchange]
     */
    override suspend fun declareExchange(
        name: String,
        type: ExchangeType,
        builder: ExchangeBuilder.() -> Unit
    ): Exchange {
        declareExchangeReactive(name, type, builder).awaitSingle()
        return Exchange(name, type, client)
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
     * @return the declared [Queue]
     */
    override suspend fun declareQueue(name: String, builder: QueueBuilder.() -> Unit): Queue {
        val result = declareQueueReactive(name, builder).awaitSingle()
        return Queue(result.queue, client)
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
        sender.unbindExchange(BindingSpecification.binding(exchangeFrom, routingKey, exchangeTo))

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
        type: ExchangeType,
        ifUnused: Boolean = false
    ): Mono<AMQP.Exchange.DeleteOk> =
        sender.deleteExchange(ExchangeBuilder(name, type).toExchangeSpecification(), ifUnused)

    /**
     * Delete an exchange.
     * @param name the name of the exchange
     */
    public suspend fun deleteExchange(
        name: String,
        type: ExchangeType,
        ifUnused: Boolean
    ): AMQP.Exchange.DeleteOk = deleteExchangeReactive(name, type, ifUnused).awaitSingle()

    private fun sendReactive(messages: Publisher<OutboundMessage>) =
        sender.send(messages)

    /**
     * Send a [Flow] of outbound messages.
     * If you just want to send messages, use the [send]
     * @param messages the message flow
     */
    override suspend fun sendFlow(messages: Flow<OutboundMessage>): Unit = suspendCoroutine { continuation ->
        sendReactive(messages.asPublisher()).doOnSuccess {
            continuation.resume(Unit)
        }.doOnError {
            continuation.resumeWithException(it)
        }.subscribe()
    }

    /**
     * Send outbound messages.
     * If you want to to send messages via a flow, use [sendFlow].
     * When you want to make sure the messages were sent correctly,
     * use [sendAndConfirmFlow] or [sendAndConfirmAsync].
     * This member function is just like fire and forget.
     * @param messages the messages to send
     */
    override suspend fun send(vararg messages: OutboundMessage): Unit = sendFlow(flowOf(*messages))

    private fun sendReactiveAndConfirm(messages: Publisher<OutboundMessage>) =
        sender.sendWithPublishConfirms(messages)

    /**
     * Sends all messages from the [messages] flow and suspends until
     * all message were sent and confirmed by the broker.
     * [action] is called for every confirmation by the broker.
     * @param messages the messages to send via a [Flow]
     * @param action the action which gets called for every confirmation response.
     */
    override suspend fun sendAndConfirmFlow(
        messages: Flow<OutboundMessage>,
        action: suspend (OutboundMessageResult<OutboundMessage>) -> Unit
    ): Unit =
        sendReactiveAndConfirm(messages.asPublisher()).asFlow().collect(action)

    /**
     * Send all [messages] from a [Flow] to your broker and
     * receive the confirmation responses in another [Flow].
     * @param messages the messages to send via a [Flow]
     * @return a [Flow] of message confirmations sent by the Broker
     */
    override fun sendAndConfirmAsync(
        messages: Flow<OutboundMessage>
    ): Flow<OutboundMessageResult<OutboundMessage>> =
        sendReactiveAndConfirm(messages.asPublisher()).asFlow()

    /**
     * Sends all [messages] to your broker and
     * suspends until we received all confirmations that every message arrived.
     * The desired [action] gets called for every arrived message
     * @param messages the messages to send
     * @param action the action which gets called for every response
     */
    override suspend fun sendAndConfirm(
        vararg messages: OutboundMessage,
        action: suspend (OutboundMessageResult<OutboundMessage>) -> Unit
    ): Unit = sendAndConfirmAsync(flowOf(*messages)).collect(action)

    /**
     * Sends all [messages] to your broker.
     * You are able to check if they were sent correctly
     * by using the returned [Flow].
     * @param messages the messages to send
     * @return A [Flow] of the confirmation responses.
     */
    override fun sendAndConfirmAsync(
        vararg messages: OutboundMessage
    ): Flow<OutboundMessageResult<OutboundMessage>> = sendAndConfirmAsync(flowOf(*messages))

    /**
     * Returns the underlying [Sender]
     */
    override fun asSender(): Sender = sender

    /**
     * Closes the underlying [Sender].
     */
    override fun close(): Unit = sender.close()
}
