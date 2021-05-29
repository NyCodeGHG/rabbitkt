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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TestBinding(
    @SerialName("source_name")
    var sourceName: String = "",
    @SerialName("source_kind")
    var sourceKind: BindingKind = BindingKind.EXCHANGE,
    @SerialName("destination_name")
    var destinationName: String = "",
    @SerialName("destination_kind")
    var destinationKind: BindingKind = BindingKind.EXCHANGE,
    @SerialName("routing_key")
    var routingKey: String = "",
    var arguments: List<String> = emptyList()
)

@Serializable
enum class BindingKind {
    @SerialName("exchange")
    EXCHANGE,

    @SerialName("queue")
    QUEUE
}
