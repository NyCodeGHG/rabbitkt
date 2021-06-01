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

package de.nycode.rabbitkt.binding

import de.nycode.rabbitkt.exchange.Exchange
import de.nycode.rabbitkt.sender.CoroutineSenderImpl

/**
 * Represents a binding between two exchanges.
 */
public data class ExchangeBinding internal constructor(
    val source: Exchange,
    val destination: Exchange,
    val routingKey: String,
    private val sender: CoroutineSenderImpl
) : Binding {
    public override suspend fun unbind() {
        sender.unbindExchange(source.name, routingKey, destination.name)
    }
}
