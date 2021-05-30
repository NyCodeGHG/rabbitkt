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
import de.nycode.rabbitkt.queue.Queue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.asFlow
import reactor.rabbitmq.AcknowledgableDelivery
import reactor.rabbitmq.Receiver
import java.io.Closeable

public inline val Receiver.coroutine: CoroutineReceiver
    get() = CoroutineReceiver(this)

@JvmInline
public value class CoroutineReceiver(private val receiver: Receiver) : Closeable {
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
    public fun consumeAutoAckFlow(queue: Queue): Flow<Delivery> {
        return receiver.consumeAutoAck(queue.name).asFlow()
    }

    /**
     * Consume messages of the given [queue] with your given [handler].
     * Every message gets acknowledged automatically.
     * @param queue the targeted queue.
     * @param handler the handler which gets called for every message.
     */
    public suspend fun consumeAutoAck(queue: Queue, handler: suspend (Delivery) -> Unit) {
        receiver.consumeAutoAck(queue.name).asFlow().collect(handler)
    }

    /**
     * Consume messages of the given [queue] with a [Flow].
     * You have to acknowledge every message manually with either
     * [AcknowledgeHandler.ack] or [AcknowledgeHandler.reject].
     * @param queue the targeted queue.
     * @return A [Flow] of deliveries which must be acknowledged or rejected.
     */
    public fun consume(queue: Queue): Flow<AcknowledgableDelivery> =
        receiver.consumeManualAck(queue.name)
            .asFlow()

    /**
     * Consume message of the given [queue] with your given [handler].
     * You have to acknowledge every message manually with either
     * [AcknowledgeHandler.ack] or [AcknowledgeHandler.reject].
     * @param queue the targeted queue.
     * @param handler the handler which gets called for every message.
     */
    public suspend fun consume(queue: Queue, handler: suspend AcknowledgeHandler.() -> Unit): Unit =
        receiver.consumeManualAck(queue.name)
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
