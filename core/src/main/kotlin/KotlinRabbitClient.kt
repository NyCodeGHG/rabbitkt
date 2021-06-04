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

package de.nycode.rabbitkt

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Delivery
import de.nycode.rabbitkt.annotations.KotlinRabbitInternals
import de.nycode.rabbitkt.exchange.Exchange
import de.nycode.rabbitkt.exchange.ExchangeBuilder
import de.nycode.rabbitkt.exchange.ExchangeType
import de.nycode.rabbitkt.plugin.Plugin
import de.nycode.rabbitkt.plugin.PluginConfiguration
import de.nycode.rabbitkt.plugin.PluginHolder
import de.nycode.rabbitkt.queue.Queue
import de.nycode.rabbitkt.queue.QueueBuilder
import de.nycode.rabbitkt.receiver.AcknowledgeHandler
import de.nycode.rabbitkt.receiver.CoroutineReceiver
import de.nycode.rabbitkt.receiver.CoroutineReceiverImpl
import de.nycode.rabbitkt.sender.CoroutineSender
import de.nycode.rabbitkt.sender.CoroutineSenderImpl
import de.nycode.rabbitkt.utils.withNIO
import kotlinx.coroutines.flow.Flow
import reactor.rabbitmq.*

public class KotlinRabbitClient internal constructor(
    configuration: KotlinRabbitClientConfiguration,
) : CoroutineSender, CoroutineReceiver {

    @KotlinRabbitInternals
    @PublishedApi
    internal val plugins: Map<PluginHolder<*, *>, Plugin<*>> = configuration.plugins

    private val connection = configuration.connectionFactory.withNIO()

    private val sender = CoroutineSenderImpl(
        this,
        RabbitFlux.createSender(SenderOptions().connectionFactory(connection))
    )

    private val receiver = CoroutineReceiverImpl(
        this,
        RabbitFlux.createReceiver(ReceiverOptions().connectionFactory(connection))
    )

    public inline fun <reified T : Plugin<*>> getPlugin(): T? {
        return plugins.values.filterIsInstance<T>().firstOrNull()
    }

    @KotlinRabbitInternals
    public fun asCoroutineSender(): CoroutineSenderImpl = sender

    @KotlinRabbitInternals
    public fun asCoroutineReceiver(): CoroutineReceiverImpl = receiver

    /**
     * Create a new [Sender] and configure it with the [builder].
     * @param builder The Configuration Block
     * @return The newly created sender
     */
    public fun createSender(builder: SenderOptions.() -> Unit = {}): CoroutineSender {
        val senderOptions = SenderOptions().apply(builder).apply {
            connectionFactory(connection)
        }
        return CoroutineSenderImpl(this, RabbitFlux.createSender(senderOptions))
    }

    /**
     * Create a new [Receiver] and configure it with the [builder].
     * @param builder The Configuration Block
     * @return The newly created receiver
     */
    public fun createReceiver(builder: ReceiverOptions.() -> Unit = {}): CoroutineReceiver {
        val receiverOptions = ReceiverOptions().apply(builder).apply {
            connectionFactory(connection)
        }
        return CoroutineReceiverImpl(this, RabbitFlux.createReceiver(receiverOptions))
    }

    override fun close() {
        sender.close()
        receiver.close()
    }

    override fun consumeAutoAckFlow(queue: String): Flow<Delivery> = receiver.consumeAutoAckFlow(queue)

    override suspend fun consumeAutoAck(queue: String, handler: suspend (Delivery) -> Unit): Unit =
        receiver.consumeAutoAck(queue, handler)

    override fun consume(queue: String): Flow<AcknowledgableDelivery> = receiver.consume(queue)

    override suspend fun consume(queue: String, handler: suspend AcknowledgeHandler.() -> Unit): Unit =
        receiver.consume(queue, handler)

    override suspend fun declareExchange(
        name: String,
        type: ExchangeType,
        builder: ExchangeBuilder.() -> Unit
    ): Exchange = sender.declareExchange(name, type, builder)

    override suspend fun declareQueue(name: String, builder: QueueBuilder.() -> Unit): Queue =
        sender.declareQueue(name, builder)

    override suspend fun sendFlow(messages: Flow<OutboundMessage>): Unit = sender.sendFlow(messages)

    override suspend fun send(vararg messages: OutboundMessage): Unit = sender.send(*messages)

    override suspend fun sendAndConfirmFlow(
        messages: Flow<OutboundMessage>,
        action: suspend (OutboundMessageResult<OutboundMessage>) -> Unit
    ): Unit = sender.sendAndConfirmFlow(messages, action)

    override fun sendAndConfirmAsync(messages: Flow<OutboundMessage>): Flow<OutboundMessageResult<OutboundMessage>> =
        sender.sendAndConfirmAsync(messages)

    override suspend fun sendAndConfirm(
        vararg messages: OutboundMessage,
        action: suspend (OutboundMessageResult<OutboundMessage>) -> Unit
    ): Unit = sender.sendAndConfirm(*messages) {
        action(it)
    }

    override fun sendAndConfirmAsync(vararg messages: OutboundMessage): Flow<OutboundMessageResult<OutboundMessage>> =
        sender.sendAndConfirmAsync(*messages)

    override fun asSender(): Sender = sender.asSender()
}

public class KotlinRabbitClientConfiguration internal constructor(
    public var connectionFactory: ConnectionFactory = ConnectionFactory()
) {

    @KotlinRabbitInternals
    @PublishedApi
    internal val plugins: MutableMap<PluginHolder<*, *>, Plugin<*>> = mutableMapOf()

    public inline fun <C : PluginConfiguration, reified P : Plugin<C>> install(
        plugin: PluginHolder<C, P>,
        configBuilder: C.() -> Unit
    ) {
        if (plugin in plugins.keys) {
            error("Cannot install plugin of type ${plugin.name} twice!")
        }
        val config = plugin.createDefaultConfiguration().apply(configBuilder)
        if (!config.isValid) {
            error("Invalid configuration for plugin ${plugin.name}")
        }
        val pluginInstance = plugin.createInstance(config)
        plugins[plugin] = pluginInstance
    }
}

/**
 * Creates a new [KotlinRabbitClient].
 * @param builder configuration builder
 * @return a new [KotlinRabbitClient]
 */
public fun createRabbitClient(builder: KotlinRabbitClientConfiguration.() -> Unit = {}): KotlinRabbitClient {
    val configuration = KotlinRabbitClientConfiguration().apply(builder)
    val client = KotlinRabbitClient(configuration)
    client.plugins.values.forEach(Plugin<*>::initialization)
    return client
}
