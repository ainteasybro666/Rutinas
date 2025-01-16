package com.example.rutinas.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {

    private val gson = Gson()

    // Convierte List<String> a String (JSON)
    @TypeConverter
    fun fromListOfStrings(list: List<String>?): String {
        return gson.toJson(list)
    }

    // Convierte String (JSON) a List<String>
    @TypeConverter
    fun toListOfStrings(json: String?): List<String> {
        if (json.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type)
    }
}