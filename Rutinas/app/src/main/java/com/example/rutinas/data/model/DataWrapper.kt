package com.example.rutinas.data.model

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

// Parceler para Any
class AnyParceler : Parceler<Any> {
    override fun create(parcel: Parcel): Any {
        return when (parcel.readInt()) {
            0 -> parcel.readString()!!
            1 -> parcel.readInt()
            2 -> parcel.readLong()
            3 -> parcel.readDouble()
            4 -> parcel.readBoolean()
            else -> throw IllegalArgumentException("Tipo no soportado")
        }
    }

    override fun write(parcel: Parcel, value: Any, flags: Int) {
        when (value) {
            is String -> {
                parcel.writeInt(0)
                parcel.writeString(value)
            }
            is Int -> {
                parcel.writeInt(1)
                parcel.writeInt(value)
            }
            is Long -> {
                parcel.writeInt(2)
                parcel.writeLong(value)
            }
            is Double -> {
                parcel.writeInt(3)
                parcel.writeDouble(value)
            }
            is Boolean -> {
                parcel.writeInt(4)
                parcel.writeBoolean(value)
            }
            else -> throw IllegalArgumentException("Tipo no soportado")
        }
    }
}

// Parceler para Map<String, Any>
class MapStringAnyParceler : Parceler<Map<String, Any>> {
    override fun create(parcel: Parcel): Map<String, Any> {
        val size = parcel.readInt()
        val map = mutableMapOf<String, Any>()
        for (i in 0 until size) {
            val key = parcel.readString() ?: ""
            val value = AnyParceler().create(parcel)
            map[key] = value
        }
        return map
    }

    override fun write(parcel: Parcel, value: Map<String, Any>, flags: Int) {
        parcel.writeInt(value.size)
        for ((key, v) in value) {
            parcel.writeString(key)
            AnyParceler().write(parcel, v, flags)
        }
    }
}
@Parcelize
@TypeParceler<Map<String, Any>, MapStringAnyParceler>()
data class DataWrapper(val data: Map<String, Any>) : Parcelable
