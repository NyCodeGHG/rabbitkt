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

package de.nycode.rabbitkt.queue

import reactor.rabbitmq.QueueSpecification

public data class QueueBuilder(
    val name: String,
    var durable: Boolean = false,
    var exclusive: Boolean = false,
    var autoDelete: Boolean = false,
    val passive: Boolean = false,
    var arguments: Map<String, Any>? = null
) {
    public fun toQueueSpecification(): QueueSpecification {
        return QueueSpecification.queue(name)
            .durable(durable)
            .exclusive(exclusive)
            .autoDelete(autoDelete)
            .passive(passive)
            .arguments(arguments)
    }
}
