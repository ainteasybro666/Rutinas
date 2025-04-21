package com.example.rutinas.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.rutinas.data.model.Action
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromTriggerData(json: String): Map<String, Any> {
        return gson.fromJson(json, object : TypeToken<Map<String, Any>>() {}.type)
    }

    @TypeConverter
    fun toTriggerData(data: Map<String, Any>): String {
        return gson.toJson(data)
    }

    @TypeConverter
    fun actionFromJson(json: String): Action = Gson().fromJson(json, Action::class.java)

    @TypeConverter
    fun actionToJson(action: Action): String = Gson().toJson(action)

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? {
        return value?.format(formatter)
    }

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it, formatter) }
    }
}
