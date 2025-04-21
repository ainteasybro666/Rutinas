package com.example.rutinas.data.local

import androidx.room.TypeConverter
import com.example.rutinas.data.model.FrequencyType

class FrequencyTypeConverter {
    @TypeConverter
    fun fromFrequencyType(frequencyType: FrequencyType): String {
        return frequencyType.name
    }

    @TypeConverter
    fun toFrequencyType(name: String): FrequencyType {
        return try {
            FrequencyType.valueOf(name)
        } catch (e: IllegalArgumentException) {
            FrequencyType.NONE
        }
    }
}