package com.example.rutinas.data.model

import android.os.Parcel
import android.os.Parcelable
import com.example.rutinas.data.local.FrequencyTypeConverter // Asegúrate de importar tu conversor
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import java.time.LocalDateTime // Import if you are storing LocalDateTime
import java.time.format.DateTimeParseException

@Parcelize
data class DataWrapper(val data: Map<String, Any?> = emptyMap()) : Parcelable {

    companion object : Parceler<DataWrapper> {

        // Usa una instancia de Gson configurada para manejar FrequencyType
        private val gson: Gson = GsonBuilder()
        // No necesitamos TypeAdapters aquí for Map<String, Any>,
        // because we will handle type casting in the getters.
        // Gson will deserialize primitive types, Strings, Lists, and Maps correctly.
        // The custom types like enums will be deserialized as their JSON representation (likely String).
            // Considera registrar otros TypeAdapter si tienes tipos personalizados en tu mapa
            .create()

        override fun create(parcel: Parcel): DataWrapper {
            val jsonString = parcel.readString()
            val data: Map<String, Any> = if (jsonString != null) {
                val mapType = object : TypeToken<Map<String, Any>>() {}.type
                gson.fromJson(jsonString, mapType) ?: emptyMap() // Ahora devuelve el mapa tal cual Gson lo deserializa
            } else {
                emptyMap()
            }
            return DataWrapper(data)
        }

        override fun DataWrapper.write(parcel: Parcel, flags: Int) {
            // Serializa usando Gson
            val jsonString = gson.toJson(data)
            parcel.writeString(jsonString)
        }
    }

    // --- ROBUST GETTERS ---
    fun getInt(key: String): Int? {
        return data[key]?.let {
            when (it) {
                is Int -> it
                is Double -> it.toInt() // Safely convert Double to Int
                else -> null // Return null for other types
            }
        }
    }

    fun getDouble(key: String): Double? {
        return data[key]?.let {
            when (it) {
                is Double -> it
                is Number -> it.toDouble() // Convert other Number types to Double
                is String -> try { it.toDouble() } catch (e: NumberFormatException) { null } // Try parsing String to Double
                else -> null
            }
        }
    }

    fun getString(key: String): String? {
        return data[key]?.toString() // toString() is generally safe and handles null gracefully
    }

    // Getter for List<Int>
    fun getListOfInt(key: String): List<Int>? {
        // Safely get the value, cast it to a List<*> (List of any type)
        return (data[key] as? List<*>)
            // If the cast is successful (not null), then mapNotNull the items
            // The lambda correctly declares 'item' as the parameter
            ?.mapNotNull { item ->
                when (item) {
                    is Int -> item
                    is Double -> item.toInt() // Handle Double to Int
                    is Number -> item.toInt() // Handle other Numbers to Int
                    is String -> item.toIntOrNull() // Try parsing String to Int
                    else -> null // Ignore other types
                }
            }
        }

    // Getter for FrequencyType
    fun getFrequencyType(key: String): FrequencyType? {
            return data[key]?.let { rawValue ->
            // Get the raw value (likely a String from Gson) and convert it using your converter
            val frequencyString = rawValue.toString()
            try {
                    FrequencyTypeConverter().toFrequencyType(frequencyString) // Use your existing converter
                } catch (e: IllegalArgumentException) {
                    Timber.w("DataWrapper: Failed to convert string '$frequencyString' to FrequencyType for key '$key'.", e)
                    null // Return null if conversion fails
                }
                }
        }

    // Getter for LocalDateTime (if you are storing it as String)
    fun getLocalDateTime(key: String): LocalDateTime? {
            return data[key]?.let { rawValue ->
            val dateTimeString = rawValue.toString()
            try {
                    // You might need to adjust the parsing pattern based on how you save LocalDateTime as a String
                    LocalDateTime.parse(dateTimeString) // Assuming standard ISO 8601 format
                } catch (e: DateTimeParseException) {
                    Timber.w("DataWrapper: Failed to parse string '$dateTimeString' to LocalDateTime for key '$key'.", e)
                    null // Return null if parsing fails
                }
                }
        }
    // Add other specific getters as needed for other types you store (e.g., Boolean, other enums)
}

