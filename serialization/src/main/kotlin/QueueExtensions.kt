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

package de.nycode.rabbitkt.serialization

import de.nycode.rabbitkt.queue.Queue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

public inline fun <reified T : Any> Queue.receiveTyped(autoAck: Boolean): Flow<T> {
    val provider = client.serializationProvider
    return receive(autoAck).mapNotNull { delivery ->
        runCatching<Queue, T> {
            provider.deserialize(delivery.body)
        }.getOrNull()
    }
}
