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
import kotlinx.coroutines.reactor.awaitSingle
import reactor.core.publisher.Mono
import reactor.rabbitmq.BindingSpecification
import reactor.rabbitmq.Sender
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
@JvmInline
public value class CoroutineSender(private val sender: Sender) : AutoCloseable {

    private fun declareExchangeReactive(
        name: String,
        builder: ExchangeBuilder.() -> Unit
    ): Mono<AMQP.Exchange.DeclareOk> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return sender.declareExchange(ExchangeBuilder(name).apply(builder).toExchangeSpecification())
    }

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

    public suspend fun bindQueue(
        exchange: String,
        routingKey: String,
        queue: String
    ): AMQP.Queue.BindOk {
        return bindQueueReactive(exchange, routingKey, queue).awaitSingle()
    }

    override fun close(): Unit = sender.close()
}

public val Sender.coroutine: CoroutineSender get() = CoroutineSender(this)
