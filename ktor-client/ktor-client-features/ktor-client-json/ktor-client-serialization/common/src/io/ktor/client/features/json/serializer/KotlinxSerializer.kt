/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.features.json.serializer

import io.ktor.client.call.*
import io.ktor.client.features.json.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.utils.io.core.*
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

/**
 * A [JsonSerializer] implemented for kotlinx [Serializable] classes.
 */
@Suppress("EXPERIMENTAL_API_USAGE_ERROR")
class KotlinxSerializer(
    private val json: Json = Json(DefaultJsonConfiguration)
) : JsonSerializer {

    override fun write(data: Any, contentType: ContentType): OutgoingContent {
        @Suppress("UNCHECKED_CAST")
        return TextContent(writeContent(data), contentType)
    }

    internal fun writeContent(data: Any): String =
        json.encodeToString(buildSerializer(data, json.serializersModule) as KSerializer<Any>, data)

    override fun read(type: TypeInfo, body: Input): Any {
        val text = body.readText()
        val deserializationStrategy = json.serializersModule.getContextual(type.type)
        val mapper = deserializationStrategy ?: (type.kotlinType?.let { serializer(it) } ?: type.type.serializer())
        return json.decodeFromString(mapper, text)!!
    }

    companion object {
        /**
         * Default [Json] configuration for [KotlinxSerializer].
         */
        val DefaultJsonConfiguration: JsonConfiguration = JsonConfiguration(
            isLenient = false,
            ignoreUnknownKeys = false,
            serializeSpecialFloatingPointValues = true,
            useArrayPolymorphism = false
        )
    }
}

@Suppress("UNCHECKED_CAST", "EXPERIMENTAL_API_USAGE_ERROR")
private fun buildSerializer(value: Any, module: SerializersModule): KSerializer<*> = when (value) {
    is JsonElement -> JsonElementSerializer
    is List<*> -> ListSerializer(value.elementSerializer(module))
    is Array<*> -> value.firstOrNull()?.let { buildSerializer(it, module) } ?: ListSerializer(String.serializer())
    is Set<*> -> SetSerializer(value.elementSerializer(module))
    is Map<*, *> -> {
        val keySerializer = value.keys.elementSerializer(module) as KSerializer<Any>
        val valueSerializer = value.values.elementSerializer(module) as KSerializer<Any>
        MapSerializer(keySerializer, valueSerializer)
    }
    else -> module.getContextual(value::class) ?: value::class.serializer()
}

@Suppress("EXPERIMENTAL_API_USAGE_ERROR")
private fun Collection<*>.elementSerializer(module: SerializersModule): KSerializer<*> {
    val serializers: List<KSerializer<*>> =
        filterNotNull().map { buildSerializer(it, module) }.distinctBy { it.descriptor.serialName }

    if (serializers.size > 1) {
        error(
            "Serializing collections of different element types is not yet supported. " +
                "Selected serializers: ${serializers.map { it.descriptor.serialName }}"
        )
    }

    val selected = serializers.singleOrNull() ?: String.serializer()

    if (selected.descriptor.isNullable) {
        return selected
    }

    @Suppress("UNCHECKED_CAST")
    selected as KSerializer<Any>

    if (any { it == null }) {
        return selected.nullable
    }

    return selected
}
