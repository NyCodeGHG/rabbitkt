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

import de.nycode.rabbitkt.annotations.KotlinRabbitInternals
import de.nycode.rabbitkt.plugin.Plugin
import de.nycode.rabbitkt.plugin.PluginConfiguration
import de.nycode.rabbitkt.plugin.PluginHolder
import de.nycode.rabbitkt.receiver.CoroutineReceiver
import de.nycode.rabbitkt.sender.CoroutineSender
import de.nycode.rabbitkt.sender.CoroutineSenderImpl
import reactor.rabbitmq.*
import java.io.Closeable

public class KotlinRabbitClient internal constructor(configuration: KotlinRabbitClientConfiguration) : Closeable {

    @KotlinRabbitInternals
    @PublishedApi
    internal val plugins: Map<PluginHolder<*, *>, Plugin<*>> = configuration.plugins

    override fun close() {
        plugins.values.forEach(Plugin<*>::shutdown)
    }

    public inline fun <reified T : Plugin<*>> getPlugin(): T? {
        return plugins.values.filterIsInstance<T>().firstOrNull()
    }

    /**
     * Create a new [Sender] and configure it with the [builder].
     * @param builder The Configuration Block
     * @return The newly created sender
     */
    public fun createSender(builder: SenderOptions.() -> Unit = {}): CoroutineSender {
        val senderOptions = SenderOptions().apply(builder)
        return CoroutineSenderImpl(this, RabbitFlux.createSender(senderOptions))
    }

    /**
     * Create a new [Receiver] and configure it with the [builder].
     * @param builder The Configuration Block
     * @return The newly created receiver
     */
    public fun createReceiver(builder: ReceiverOptions.() -> Unit = {}): CoroutineReceiver {
        val receiverOptions = ReceiverOptions().apply(builder)
        return CoroutineReceiver(this, RabbitFlux.createReceiver(receiverOptions))
    }
}

public class KotlinRabbitClientConfiguration internal constructor() {

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
