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

import com.rabbitmq.client.ConnectionFactory
import de.nycode.rabbitkt.createRabbitClient
import de.nycode.rabbitkt.exchange.ExchangeType.DIRECT
import de.nycode.rabbitkt.sender.BindingKind.EXCHANGE
import de.nycode.rabbitkt.sender.BindingKind.QUEUE
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import reactor.rabbitmq.OutboundMessage
import strikt.api.*
import strikt.assertions.*

@OptIn(ExperimentalCoroutinesApi::class)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CoroutineSenderTest {

    private var sender: CoroutineSenderImpl? = null

    companion object {
        @Container
        @JvmStatic
        private val rabbit = TestRabbitMQContainer("rabbitmq:3.8.16-management-alpine")
    }

    @BeforeEach
    fun setup() {
        sender = createRabbitClient().createSender {
            connectionFactory(ConnectionFactory().apply {
                host = rabbit.host
                port = rabbit.amqpPort
                username = rabbit.adminUsername
                password = rabbit.adminPassword
            })
        } as CoroutineSenderImpl
    }

    @AfterEach
    fun tearDown() {
        sender!!.close()
        rabbit.stop()
        rabbit.start()
    }

    @Test
    fun `Declaring an exchange should work`(): Unit = runBlocking {
        val testExchangeName = "test_exchange"
        sender!!.declareExchange(testExchangeName, DIRECT)
        expectThat(rabbit).hasExchange(testExchangeName)
    }

    @Test
    fun `Declaring a queue should work`(): Unit = runBlocking {
        val testQueueName = "test_queue"
        sender!!.declareQueue(testQueueName)
        expectThat(rabbit).hasQueue(testQueueName)
    }

    private suspend fun declareAndBindExchange(
        fromExchange: String,
        toExchange: String,
        testRoutingKey: String
    ) {
        sender!!.declareExchange(fromExchange, DIRECT)
        sender!!.declareExchange(toExchange, DIRECT)
        sender!!.bindExchange(fromExchange, testRoutingKey, toExchange)
    }

    @Test
    fun `Binding an exchange to another exchange should work`(): Unit = runBlocking {
        val fromExchange = "test_exchange"
        val toExchange = "test_another_exchange"
        val testRoutingKey = "test_routing_key"
        declareAndBindExchange(fromExchange, toExchange, testRoutingKey)

        expectThat(rabbit).hasBinding {
            sourceName = fromExchange
            sourceKind = EXCHANGE
            destinationName = toExchange
            destinationKind = EXCHANGE
            routingKey = testRoutingKey
        }
    }

    private suspend fun declareAndBindQueue(exchange: String, queue: String, testRoutingKey: String) {
        sender!!.declareExchange(exchange, DIRECT)
        sender!!.declareQueue(queue)
        sender!!.bindQueue(exchange, testRoutingKey, queue)
    }

    @Test
    fun `Binding an exchange to a queue should work`() = runBlocking {
        val exchange = "test_exchange"
        val testRoutingKey = "test_routing_key"
        val queue = "test_queue"

        declareAndBindQueue(exchange, queue, testRoutingKey)

        expectThat(rabbit).hasBinding {
            sourceName = exchange
            sourceKind = EXCHANGE
            destinationName = queue
            destinationKind = QUEUE
            routingKey = testRoutingKey
        }
    }

    @Test
    fun `Unbinding an exchange to exchange binding should work`(): Unit = runBlocking {
        val fromExchange = "test_exchange"
        val toExchange = "test_another_exchange"
        val testRoutingKey = "test_routing_key"
        declareAndBindExchange(fromExchange, toExchange, testRoutingKey)

        val assertion: TestBinding.() -> Unit = {
            sourceName = fromExchange
            sourceKind = EXCHANGE
            destinationName = toExchange
            destinationKind = EXCHANGE
            routingKey = testRoutingKey
        }

        expectThat(rabbit).hasBinding(assertion)

        sender!!.unbindExchange(fromExchange, testRoutingKey, toExchange)

        expectThat(rabbit).not().hasBinding(assertion)
    }

    @Test
    fun `Unbinding an exchange to queue binding should work`(): Unit = runBlocking {
        val exchange = "test_exchange"
        val testRoutingKey = "test_routing_key"
        val queue = "test_queue"

        declareAndBindQueue(exchange, queue, testRoutingKey)

        val assertion: TestBinding.() -> Unit = {
            sourceName = exchange
            sourceKind = EXCHANGE
            destinationName = queue
            destinationKind = QUEUE
            routingKey = testRoutingKey
        }

        expectThat(rabbit).hasBinding(assertion)

        sender!!.unbindQueue(exchange, testRoutingKey, queue)

        expectThat(rabbit).not().hasBinding(assertion)
    }

    @Test
    fun `Delete an exchange should work`(): Unit = runBlocking {
        val exchangeName = "test_exchange"

        sender!!.declareExchange(exchangeName, DIRECT)

        expectThat(rabbit).hasExchange(exchangeName)

        sender!!.deleteExchange(exchangeName, DIRECT, true)

        expectThat(rabbit).not().hasExchange(exchangeName)
    }

    @Test
    fun `Sending messages with a flow should work`(): Unit = runBlocking {
        val exchangeName = "test_exchange"
        val queueName = "test_queue"
        sender!!.declareExchange(exchangeName, DIRECT) {
            autoDelete = true
        }
        sender!!.declareQueue(queueName) {
            autoDelete = true
        }

        expectThat(rabbit).hasExchange(exchangeName)

        val expected = "test_data"
        sender!!.sendFlow(flowOf(OutboundMessage(exchangeName, "", expected.encodeToByteArray())))
    }

    @Test
    fun `Sending a simple message should work`(): Unit = runBlocking {
        val exchangeName = "test_exchange"
        val queueName = "test_queue"
        sender!!.declareExchange(exchangeName, DIRECT) {
            autoDelete = true
        }
        sender!!.declareQueue(queueName) {
            autoDelete = true
        }

        expectThat(rabbit).hasExchange(exchangeName)

        val expected = "test_data"
        sender!!.send(OutboundMessage(exchangeName, "", expected.encodeToByteArray()))
    }

    @Test
    fun `Sending messages with confirmation should work`(): Unit = runBlocking {
        val exchangeName = "test_exchange"
        val queueName = "test_queue"
        sender!!.declareExchange(exchangeName, DIRECT)
        sender!!.declareQueue(queueName)

        expectThat(rabbit).hasExchange(exchangeName)

        sender!!.bindQueue(exchangeName, "", queueName)

        expectThat(rabbit).hasBinding {
            sourceName = exchangeName
            sourceKind = EXCHANGE
            destinationName = queueName
            destinationKind = QUEUE
            routingKey = ""
        }

        val expected = "test_data"
        var confirmed = false
        sender!!.sendAndConfirm(OutboundMessage(exchangeName, "", expected.encodeToByteArray())) {
            confirmed = true
            expectThat(it.outboundMessage.body.decodeToString()).isEqualTo(expected)
        }

        expectThat(confirmed).isTrue()
    }

    @Test
    fun `Sending an confirmation of messages should work async`(): Unit = runBlocking {
        val exchangeName = "test_exchange"
        val queueName = "test_queue"

        sender!!.declareExchange(exchangeName, DIRECT)
        expectThat(rabbit).hasExchange(exchangeName)

        sender!!.declareQueue(queueName)
        expectThat(rabbit).hasQueue(queueName)

        sender!!.bindQueue(exchangeName, "", queueName)

        expectThat(rabbit).hasBinding {
            sourceName = exchangeName
            sourceKind = EXCHANGE
            destinationName = queueName
            destinationKind = QUEUE
            routingKey = ""
        }

        val expected = "test_data"
        val result = sender!!.sendAndConfirmAsync(OutboundMessage(exchangeName, "", expected.encodeToByteArray()))
            .single()
            .outboundMessage
            .body
            .decodeToString()
        expectThat(result).isEqualTo(result)
    }
}
