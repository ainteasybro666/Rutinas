package com.example.rutinas.data.model

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize

@Parcelize
data class DataWrapper(
    val data: Map<String, Any>? = null
) : Parcelable {
    companion object : Parceler<DataWrapper> {
        override fun create(parcel: Parcel): DataWrapper {
            val data = parcel.readHashMap(HashMap::class.java.classLoader) as? Map<String, Any>
            return DataWrapper(data)
        }

        override fun DataWrapper.write(parcel: Parcel, flags: Int) {
            parcel.writeMap(this.data)
        }
    }
}

object AnyParceler : Parceler<Any> {
    override fun create(parcel: Parcel): Any {
        return when (val type = parcel.readString()) {
            "String" -> parcel.readString()!!
            "Int" -> parcel.readInt()
            "Boolean" -> parcel.readBoolean()
            "Map" -> MapStringAnyParceler.create(parcel)
            else -> throw IllegalArgumentException("Unsupported type: $type")
        }
    }

    override fun Any.write(parcel: Parcel, flags: Int) {
        when (this) {
            is String -> {
                parcel.writeString("String")
                parcel.writeString(this)
            }

            is Int -> {
                parcel.writeString("Int")
                parcel.writeInt(this)
            }

            is Boolean -> {
                parcel.writeString("Boolean")
                parcel.writeBoolean(this)
            }

            is Map<*, *> -> {
                parcel.writeString("Map")
                MapStringAnyParceler.write(this as Map<String, Any>, parcel, flags)
            }

            else -> throw IllegalArgumentException("Unsupported value type: ${this.javaClass.name}")
        }
    }
}

object MapStringAnyParceler : Parceler<Map<String, Any>> {
    override fun create(parcel: Parcel): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        val size = parcel.readInt()
        for (i in 0 until size) {
            val key = parcel.readString()!!
            val value = AnyParceler.create(parcel)
            map[key] = value
        }
        return map
    }

    override fun Map<String, Any>.write(parcel: Parcel, flags: Int) {
        parcel.writeInt(this.size)
        for ((key, value) in this) {
            parcel.writeString(key)
            AnyParceler.write(value, parcel, flags)
        }
    }
}
