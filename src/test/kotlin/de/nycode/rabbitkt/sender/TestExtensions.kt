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

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.testcontainers.containers.RabbitMQContainer
import strikt.api.*

fun Assertion.Builder<RabbitMQContainer>.hasExchange(testExchangeName: String) {
    assertThat("container should have exchange $testExchangeName") { container ->
        val result =
            Json.decodeFromString<List<TestExchange>>(
                container.execInContainer(
                    "rabbitmqctl",
                    "list_exchanges",
                    "--formatter",
                    "json"
                ).stdout
            )
        result.any { it.name == testExchangeName }
    }
}

fun Assertion.Builder<RabbitMQContainer>.hasQueue(testQueueName: String) {
    assertThat("container should have queue $testQueueName") { container ->
        val result =
            Json.decodeFromString<List<TestQueue>>(
                container.execInContainer(
                    "rabbitmqctl",
                    "list_queues",
                    "--formatter",
                    "json"
                ).stdout
            )
        result.any { it.name == testQueueName }
    }
}

fun Assertion.Builder<RabbitMQContainer>.hasBinding(builder: TestBinding.() -> Unit) {
    assertThat("container should have binding") { container ->
        val result =
            Json.decodeFromString<List<TestBinding>>(
                container.execInContainer(
                    "rabbitmqctl",
                    "list_bindings",
                    "--formatter",
                    "json"
                ).stdout
            )
        val binding = TestBinding().apply(builder)
        binding in result
    }
}
