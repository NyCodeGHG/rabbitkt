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

package de.nycode.rabbitkt.receiver

import com.rabbitmq.client.Delivery
import de.nycode.rabbitkt.KotlinRabbitClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.asFlow
import reactor.rabbitmq.AcknowledgableDelivery
import reactor.rabbitmq.Receiver

public class CoroutineReceiverImpl(private val client: KotlinRabbitClient, private val receiver: Receiver) :
    CoroutineReceiver {
    /**
     * Closes the underlying [Receiver].
     */
    override fun close(): Unit = receiver.close()

    /**
     * Consume messages of the given [queue] with a [Flow].
     * Every message gets acknowledged automatically.
     * @param queue the targeted queue.
     * @return A [Flow] of deliveries.
     */
    override fun consumeAutoAckFlow(queue: String): Flow<Delivery> {
        return receiver.consumeAutoAck(queue).asFlow()
    }

    /**
     * Consume messages of the given [queue] with your given [handler].
     * Every message gets acknowledged automatically.
     * @param queue the targeted queue.
     * @param handler the handler which gets called for every message.
     */
    override suspend fun consumeAutoAck(queue: String, handler: suspend (Delivery) -> Unit) {
        receiver.consumeAutoAck(queue).asFlow().collect(handler)
    }

    /**
     * Consume messages of the given [queue] with a [Flow].
     * You have to acknowledge every message manually with either
     * [AcknowledgeHandler.ack] or [AcknowledgeHandler.reject].
     * @param queue the targeted queue.
     * @return A [Flow] of deliveries which must be acknowledged or rejected.
     */
    override fun consume(queue: String): Flow<AcknowledgableDelivery> =
        receiver.consumeManualAck(queue)
            .asFlow()

    /**
     * Consume message of the given [queue] with your given [handler].
     * You have to acknowledge every message manually with either
     * [AcknowledgeHandler.ack] or [AcknowledgeHandler.reject].
     * @param queue the targeted queue.
     * @param handler the handler which gets called for every message.
     */
    override suspend fun consume(queue: String, handler: suspend AcknowledgeHandler.() -> Unit): Unit =
        receiver.consumeManualAck(queue)
            .asFlow()
            .collect {
                handler(AcknowledgeHandler(it))
            }

}

@JvmInline
public value class AcknowledgeHandler internal constructor(
    private val acknowledgableDelivery: AcknowledgableDelivery
) {

    /**
     * The underlying delivery of this handler.
     */
    public val delivery: Delivery
        get() = acknowledgableDelivery

    /**
     * Acknowledges the delivery.
     * @param multiple acknowledge multiple messages or not
     */
    public fun ack(multiple: Boolean = false): Unit = acknowledgableDelivery.ack(multiple)

    /**
     * Rejects the delivery.
     * @param multiple acknowledge multiple messages or not.
     * @param requeue requeue the message into the broker or not.
     */
    public fun reject(multiple: Boolean = false, requeue: Boolean): Unit =
        acknowledgableDelivery.nack(multiple, requeue)
}