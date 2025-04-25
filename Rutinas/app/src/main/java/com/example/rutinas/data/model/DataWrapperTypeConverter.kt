package com.example.rutinas.data.model

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DataWrapperTypeConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromDataWrapper(dataWrapper: DataWrapper?): String? {
        return dataWrapper?.let { gson.toJson(it.data) }
    }

    @TypeConverter
    fun toDataWrapper(dataString: String?): DataWrapper? {
        dataString?.let {
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val data: Map<String, Any> = gson.fromJson(it, mapType)
            return DataWrapper(data)
        }
        return null
    }
}