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

package de.nycode.rabbitkt.publisher

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.collect
import org.reactivestreams.Publisher
import java.util.concurrent.ConcurrentLinkedQueue

public suspend fun <T> Publisher<T>.toList(): List<T> {
    val queue = ConcurrentLinkedQueue<T>()
    collect { queue.add(it) }
    return queue.toList()
}

/**
 * Coroutine wrapper around [Publisher].
 */
public open class CoroutinePublisher<T : Any>(public open val publisher: Publisher<T>) {

    /**
     * Provides a list of not null elements from the publisher.
     */
    public suspend fun toList(): List<T> = publisher.toList()

    /**
     * Provides a flow of not null elements from the publisher.
     */
    public fun toFlow(): Flow<T> = publisher.asFlow()

    /**
     * Iterates over all elements in the publisher.
     */
    public suspend fun consumeEach(action: suspend (T) -> Unit) {
        publisher.collect { action(it) }
    }
}
