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

import de.nycode.rabbitkt.serialization.deserialize
import de.nycode.rabbitkt.serialization.kotlinx.json.KotlinxSerializationJsonProvider
import de.nycode.rabbitkt.serialization.serialize
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import strikt.api.*
import strikt.assertions.*

@Serializable
data class TestData(
    val aString: String,
    val aNumber: Int
)

@Serializable
data class TestData2(val test: List<Int>)

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class KotlinxSerializationJsonProviderTest {

    private val provider = KotlinxSerializationJsonProvider(Json)

    @Test
    fun `Serializing a String should work`() {
        val testString = "Hi, I'm a test string!"

        val bytes = provider.serialize(testString)
        val result = provider.deserialize<String>(bytes)

        expectThat(result).isEqualTo(testString)
    }

    @Test
    fun `Serializing a simple data class should work`() {
        val testData = TestData("Hi, I'm another test string!", 42)

        val bytes = provider.serialize(testData)
        val result = provider.deserialize<TestData>(bytes)

        expectThat(result).isEqualTo(testData)
    }

    @Test
    fun `Serializing a list of Ints inside a data class should work`() {
        val data = TestData2((1..20).toList())

        val bytes = provider.serialize(data)
        val result = provider.deserialize<TestData2>(bytes)

        expectThat(result).isEqualTo(data)
    }
}
