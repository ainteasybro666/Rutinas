package com.example.rutinas.data.local

import androidx.room.TypeConverter
import com.example.rutinas.data.model.DataWrapper
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import timber.log.Timber

// Custom TypeAdapter para manejar la serialización y deserialización de números,
// tratando enteros como Int y serializándolos sin .0 (esto ya funciona, lo mantenemos por si acaso)
class NumberTypeAdapter : JsonSerializer<Number>, JsonDeserializer<Number> {

    override fun serialize(
        src: Number?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement? {
        if (src == null) {
            return null
        }
        return if (src.toDouble() == src.toInt().toDouble()) {
            JsonPrimitive(src.toInt())
        } else {
            JsonPrimitive(src.toDouble())
        }
    }

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Number? {
        if (json == null || json.isJsonNull) {
            return null
        }
        try {
            return json.asInt
        } catch (e: NumberFormatException) {
            try {
                return json.asDouble
            } catch (e2: NumberFormatException) {
                Timber.e(e2, "NumberTypeAdapter: Cannot deserialize ${json.asString} as Number")
                throw JsonParseException("Cannot deserialize ${json.asString} as Number")
            }
        }
    }
}

// Custom TypeAdapter para Map<String, Any?> para asegurar la deserialización correcta de números y manejar nulls
class MapStringTypeAnyAdapter : JsonDeserializer<Map<String, Any?>>, JsonSerializer<Map<String, Any?>> { // Cambiado Map<String, Any> a Map<String, Any?>

    private val defaultGson = Gson() // Usamos una instancia de Gson interna


    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Map<String, Any?>? { // Cambiado Map<String, Any> a Map<String, Any?>
        Timber.d("MapStringTypeAnyAdapter: Deserializing map: ${defaultGson.toJson(json)}")

        if (json == null || !json.isJsonObject) {
            Timber.d("MapStringTypeAnyAdapter: Input for deserialization is null or not a JsonObject")
            return null
        }

        val map = mutableMapOf<String, Any?>() // Cambiado Map<String, Any> a Map<String, Any?>
        val jsonObject = json.asJsonObject

        for ((key, value) in jsonObject.entrySet()) {
            when {
                value.isJsonPrimitive -> {
                    val primitive = value.asJsonPrimitive
                    when {
                        primitive.isBoolean -> map[key] = primitive.asBoolean
                        primitive.isString -> map[key] = primitive.asString
                        primitive.isNumber -> {
                            val number = primitive.asNumber
                            map[key] = if (number.toDouble() == number.toInt().toDouble()) {
                                Timber.d("MapStringTypeAnyAdapter: Deserializing number $key as Int: ${number.toInt()}")
                                number.toInt()
                            } else {
                                Timber.d("MapStringTypeAnyAdapter: Deserializing number $key as Double: ${number.toDouble()}")
                                number.toDouble()
                            }
                        }
                        else -> {
                            Timber.d("MapStringTypeAnyAdapter: Encountered other primitive type for key $key: ${primitive.javaClass.simpleName}")
                            map[key] = primitive
                        }
                    }
                }
                value.isJsonArray -> {
                    Timber.d("MapStringTypeAnyAdapter: Deserializing array for key $key")
                    val listType = object : TypeToken<List<Any?>>() {}.type // También puede necesitar Any? aquí
                    val list: List<Any?> = defaultGson.fromJson(value, listType)
                    val processedList = list.map { listItem ->
                        if (listItem is Double && listItem == listItem.toInt().toDouble()) {
                            listItem.toInt()
                        } else {
                            listItem
                        }
                    }
                    map[key] = processedList
                }
                value.isJsonObject -> {
                    Timber.d("MapStringTypeAnyAdapter: Deserializing object for key $key")
                    val nestedMapType = object : TypeToken<Map<String, Any?>>() {}.type // También puede necesitar Any? aquí
                    val nestedMap: Map<String, Any?> = defaultGson.fromJson(value, nestedMapType)
                    val processedNestedMap = nestedMap.mapValues { nestedEntry ->
                        if (nestedEntry.value is Double && nestedEntry.value as Double == (nestedEntry.value as Double).toInt().toDouble()) {
                            (nestedEntry.value as Double).toInt()
                        } else {
                            nestedEntry.value
                        }
                    }
                    map[key] = processedNestedMap
                }
                value.isJsonNull -> {
                    Timber.d("MapStringTypeAnyAdapter: Encountered JsonNull for key $key")
                    map[key] = null // Esto ahora es válido porque el mapa acepta Any?
                }
            }
        }
        Timber.d("MapStringTypeAnyAdapter: Finished deserializing map. Result: $map")
        return map
    }

    override fun serialize(
        src: Map<String, Any?>?, // Cambiado Map<String, Any> a Map<String, Any?>
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement? {
        Timber.d("MapStringTypeAnyAdapter: Serializing map: $src")
        if (src == null) {
            Timber.d("MapStringTypeAnyAdapter: Input for serialization is null")
            return null
        }
        return context?.serialize(src, typeOfSrc)
    }
}


class DataWrapperTypeConverter {
    // Crea una instancia de Gson configurada con el TypeAdapter completo para Map<String, Any?>
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(object : TypeToken<Map<String, Any?>>() {}.type, MapStringTypeAnyAdapter()) // Cambiado TypeToken<Map<String, Any>>() a TypeToken<Map<String, Any?>>()
        .create()

    @TypeConverter
    fun fromDataWrapper(dataWrapper: DataWrapper?): String? {
        Timber.d("DataWrapperTypeConverter: Converting DataWrapper to String")
        return dataWrapper?.let { gson.toJson(it.data) }
    }

    @TypeConverter
    fun toDataWrapper(dataString: String?): DataWrapper? {
        Timber.d("DataWrapperTypeConverter: Converting String to DataWrapper: $dataString")
        dataString?.let {
            // Usa TypeToken para el mapa con Any?
            val mapType = object : TypeToken<Map<String, Any?>>() {}.type // Cambiado TypeToken<Map<String, Any>>() a TypeToken<Map<String, Any?>>()
            val data: Map<String, Any?> = gson.fromJson(it, mapType)
            Timber.d("DataWrapperTypeConverter: Deserialized map data: $data")
            return DataWrapper(data)
        }
        Timber.d("DataWrapperTypeConverter: Input string for toDataWrapper is null")
        return null
    }
}
