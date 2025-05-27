package com.example.rutinas.data.local

import androidx.room.TypeConverter
import com.example.rutinas.data.local.FrequencyTypeConverter // Asegúrate de importar tu FrequencyTypeConverter
import com.example.rutinas.data.model.DataWrapper
import com.example.rutinas.data.model.FrequencyType
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

class DataWrapperTypeConverter {
    // Crea una instancia de Gson configurada para manejar FrequencyType
    // porque DataWrapper's getters will handle the type conversion.

    private val gson: Gson = Gson() // Use a standard Gson instance


    @TypeConverter
    fun fromDataWrapper(dataWrapper: DataWrapper?): String? {
        return dataWrapper?.let { gson.toJson(it.data) }
    }

    @TypeConverter
    fun toDataWrapper(dataString: String?): DataWrapper? {
        dataString?.let {
            // Usa TypeToken para un mapa donde los valores pueden ser Any, pero Gson ahora sabe cómo
            // manejar FrequencyType gracias al TypeAdapter registrado.
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val data: Map<String, Any> = gson.fromJson(it, mapType)
            return DataWrapper(data)
        }
        return null
    }
}
