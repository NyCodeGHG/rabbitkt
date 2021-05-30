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
import de.nycode.rabbitkt.KotlinRabbit
import de.nycode.rabbitkt.sender.BindingKind.EXCHANGE
import de.nycode.rabbitkt.sender.BindingKind.QUEUE
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.RabbitMQContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import strikt.api.*

@OptIn(ExperimentalCoroutinesApi::class)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CoroutineSenderTest {

    private var sender: CoroutineSender? = null

    companion object {
        @JvmStatic
        @Container
        private val rabbit = RabbitMQContainer("rabbitmq:3.8.16-management-alpine")
    }

    @BeforeAll
    fun setup() {
        sender = KotlinRabbit.createSender {
            connectionFactory(ConnectionFactory().apply {
                host = rabbit.host
                port = rabbit.amqpPort
                username = rabbit.adminUsername
                password = rabbit.adminPassword
            })
        }.coroutine
    }

    @Test
    fun declareExchange(): Unit = runBlocking {
        val testExchangeName = "test_exchange"
        sender!!.declareExchange(testExchangeName)
        expectThat(rabbit).hasExchange(testExchangeName)
    }

    @Test
    fun declareQueue(): Unit = runBlocking {
        val testQueueName = "test_queue"
        sender!!.declareQueue(testQueueName)
        expectThat(rabbit).hasQueue(testQueueName)
    }

    private suspend fun declareAndBindExchange(
        fromExchange: String,
        toExchange: String,
        testRoutingKey: String
    ) {
        sender!!.declareExchange(fromExchange)
        sender!!.declareExchange(toExchange)
        sender!!.bindExchange(fromExchange, testRoutingKey, toExchange)
    }

    @Test
    fun bindExchange(): Unit = runBlocking {
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
        sender!!.declareExchange(exchange)
        sender!!.declareQueue(queue)
        sender!!.bindQueue(exchange, testRoutingKey, queue)
    }

    @Test
    fun bindQueue() = runBlocking {
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
    fun unbindExchange(): Unit = runBlocking {
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
    fun unbindQueue(): Unit = runBlocking {
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
    fun deleteExchange(): Unit = runBlocking {
        val exchangeName = "test_exchange"

        sender!!.declareExchange(exchangeName)

        expectThat(rabbit).hasExchange(exchangeName)

        sender!!.deleteExchange(exchangeName, true) {}

        expectThat(rabbit).not().hasExchange(exchangeName)
    }

//    @Test
//    fun sendFlow() {
//    }
//
//    @Test
//    fun send() {
//    }
//
//    @Test
//    fun sendAndConfirm() {
//    }
//
//    @Test
//    fun sendAndConfirmAsync() {
//    }
//
//    @Test
//    fun testSendAndConfirm() {
//    }
//
//    @Test
//    fun testSendAndConfirmAsync() {
//    }
}
