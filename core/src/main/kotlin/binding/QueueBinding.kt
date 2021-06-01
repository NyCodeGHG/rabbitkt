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
import de.nycode.rabbitkt.queue.Queue
import de.nycode.rabbitkt.sender.CoroutineSenderImpl

public data class QueueBinding internal constructor(
    val source: Exchange,
    val destination: Queue,
    val routingKey: String,
    private val sender: CoroutineSenderImpl
) : Binding {
    public override suspend fun unbind() {
        sender.unbindQueue(source.name, routingKey, destination.name)
    }
}
