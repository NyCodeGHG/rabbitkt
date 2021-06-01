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
package de.nycode.rabbitkt.serialization.kotlinx.core

import de.nycode.rabbitkt.annotations.KotlinRabbitInternals
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.modules.SerializersModule
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

@PublishedApi
internal val customSerializers: MutableMap<KClass<*>, KSerializer<*>> = ConcurrentHashMap()

private val customModules = CopyOnWriteArraySet<SerializersModule>()

@Volatile
@PublishedApi
internal var checkBaseModule: Boolean = true

/**
 * Add a custom [SerializersModule] to kotlinx.serialization json provider.
 */
public fun registerModule(module: SerializersModule) {
    customModules.add(module)
}

/**
 * Register a custom [KSerializer].
 */
public inline fun <reified T> registerSerializer(serializer: KSerializer<T>) {
    customSerializers[T::class] = serializer
}

@OptIn(KotlinRabbitInternals::class)
public val rabbitktSerializationModule: SerializersModule
    get() = KotlinRabbitSerializationRepository.module

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
@Suppress("UNCHECKED_CAST")
@KotlinRabbitInternals
public object KotlinRabbitSerializationRepository {

    private val serializersMap: Map<KClass<*>, KSerializer<*>> = mapOf()

    private fun <T : Any> getBaseSerializer(
        obj: T,
        kClass: KClass<T> = obj.javaClass.kotlin
    ): SerializationStrategy<*>? {
        return when (obj) {
            is Pair<*, *> -> PairSerializer(getSerializer(obj.first), getSerializer(obj.second))
            is Triple<*, *, *> -> TripleSerializer(
                getSerializer(obj.first),
                getSerializer(obj.second),
                getSerializer(obj.third)
            )
            is List<*> -> ListSerializer(obj.filterNotNull().let {
                if (it.isEmpty()) String.serializer() else getSerializer(it.first())
            } as KSerializer<Any>)
            is Array<*> -> ArraySerializer(
                kClass as KClass<Any>,
                obj.filterNotNull().let {
                    if (it.isEmpty()) String.serializer() else getSerializer(it.first())
                } as KSerializer<Any>
            )
            else -> module.getContextual(kClass)
                ?: findPolymorphic(kClass, obj)?.let {
                    PolymorphicSerializer(it)
                }
                ?: findSealed(kClass)?.serializerOrNull()
        }
    }

    private fun <T : Any> findPolymorphic(kClass: KClass<*>, obj: T): KClass<*>? =
        module.getPolymorphic(kClass as KClass<T>, obj)
            ?.let { kClass }
            ?: kClass.superclasses.asSequence().map { findPolymorphic(it, obj) }.filterNotNull().firstOrNull()

    private fun findSealed(kClass: KClass<*>): KClass<*>? =
        kClass.takeIf { it.isSealed }
            ?: kClass.superclasses.asSequence().map { findSealed(it) }.filterNotNull().firstOrNull()

    public fun <T : Any> getSerializer(kClass: KClass<T>, obj: T?): KSerializer<T> =
        if (obj == null) {
            error("no serializer for null")
        } else {
            (serializersMap[kClass]
                ?: getBaseSerializer(obj, kClass)
                ?: kClass.serializer()) as? KSerializer<T>
                ?: error("no serializer for $obj of class $kClass")
        }

    private fun <T : Any> getSerializer(obj: T?): KSerializer<T> =
        if (obj == null) {
            error("no serializer for null")
        } else {
            (serializersMap[obj.javaClass.kotlin]
                ?: getBaseSerializer(obj)
                ?: obj.javaClass.kotlin.serializer()) as? KSerializer<T>
                ?: error("no serializer for $obj of class ${obj.javaClass.kotlin}")
        }

    public fun <T : Any> getSerializer(kClass: KClass<T>): KSerializer<T> =
        (serializersMap[kClass]
            ?: module.getContextual(kClass)
            ?: try {
                kClass.serializer()
            } catch (e: SerializationException) {
                if (kClass.isAbstract || kClass.isOpen || kClass.isSealed) {
                    PolymorphicSerializer(kClass)
                } else {
                    throw e
                }
            }
                ) as? KSerializer<T>
            ?: error("no serializer for $kClass of class $kClass")

    @Volatile
    private var baseModule: SerializersModule = initBaseModule()

    private fun initBaseModule(): SerializersModule = SerializersModule {
        serializersMap.forEach { contextual(it.key as KClass<Any>, it.value as KSerializer<Any>) }
        customSerializers.forEach { contextual(it.key as KClass<Any>, it.value as KSerializer<Any>) }
        customModules.forEach { include(it) }
    }

    public val module: SerializersModule
        get() {
            if (checkBaseModule) {
                checkBaseModule = false
                baseModule = initBaseModule()
            }
            return baseModule
        }
}
