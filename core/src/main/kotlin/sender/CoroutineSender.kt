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

import de.nycode.rabbitkt.exchange.Exchange
import de.nycode.rabbitkt.exchange.ExchangeBuilder
import de.nycode.rabbitkt.exchange.ExchangeType
import de.nycode.rabbitkt.queue.Queue
import de.nycode.rabbitkt.queue.QueueBuilder
import kotlinx.coroutines.flow.Flow
import reactor.rabbitmq.OutboundMessage
import reactor.rabbitmq.OutboundMessageResult
import reactor.rabbitmq.Sender
import java.io.Closeable

public interface CoroutineSender : Closeable {
    /**
     * Declare an exchange with the specified name.
     * Configure the Exchange with the builder.
     * @param name the name of the exchange
     * @param builder the configuration builder
     * @return the declared [Exchange]
     */
    public suspend fun declareExchange(
        name: String,
        type: ExchangeType,
        builder: ExchangeBuilder.() -> Unit = {}
    ): Exchange

    /**
     * Declare a queue with the specified name.
     * Configure the Queue with the builder.
     * @param name the name of the queue
     * @param builder the configuration builder
     * @return the declared [Queue]
     */
    public suspend fun declareQueue(name: String, builder: QueueBuilder.() -> Unit = {}): Queue

    /**
     * Send a [Flow] of outbound messages.
     * If you just want to send messages, use the [send]
     * @param messages the message flow
     */
    public suspend fun sendFlow(messages: Flow<OutboundMessage>)

    /**
     * Send outbound messages.
     * If you want to to send messages via a flow, use [sendFlow].
     * When you want to make sure the messages were sent correctly,
     * use [sendAndConfirmFlow] or [sendAndConfirmAsync].
     * This member function is just like fire and forget.
     * @param messages the messages to send
     */
    public suspend fun send(vararg messages: OutboundMessage)

    /**
     * Sends all messages from the [messages] flow and suspends until
     * all message were sent and confirmed by the broker.
     * [action] is called for every confirmation by the broker.
     * @param messages the messages to send via a [Flow]
     * @param action the action which gets called for every confirmation response.
     */
    public suspend fun sendAndConfirmFlow(
        messages: Flow<OutboundMessage>,
        action: suspend (OutboundMessageResult<OutboundMessage>) -> Unit = {}
    )

    /**
     * Send all [messages] from a [Flow] to your broker and
     * receive the confirmation responses in another [Flow].
     * @param messages the messages to send via a [Flow]
     * @return a [Flow] of message confirmations sent by the Broker
     */
    public fun sendAndConfirmAsync(
        messages: Flow<OutboundMessage>
    ): Flow<OutboundMessageResult<OutboundMessage>>

    /**
     * Sends all [messages] to your broker and
     * suspends until we received all confirmations that every message arrived.
     * The desired [action] gets called for every arrived message
     * @param messages the messages to send
     * @param action the action which gets called for every response
     */
    public suspend fun sendAndConfirm(
        vararg messages: OutboundMessage,
        action: suspend (OutboundMessageResult<OutboundMessage>) -> Unit = {}
    )

    /**
     * Sends all [messages] to your broker.
     * You are able to check if they were sent correctly
     * by using the returned [Flow].
     * @param messages the messages to send
     * @return A [Flow] of the confirmation responses.
     */
    public fun sendAndConfirmAsync(
        vararg messages: OutboundMessage
    ): Flow<OutboundMessageResult<OutboundMessage>>

    /**
     * Returns the underlying [Sender]
     */
    public fun asSender(): Sender

    /**
     * Closes the underlying [Sender].
     */
    override fun close()
}
