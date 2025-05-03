package com.example.rutinas.data.model

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

@Parcelize
data class DataWrapper(val data: Map<String, Any> = emptyMap()) : Parcelable {

    companion object : Parceler<DataWrapper> {

        override fun create(parcel: Parcel): DataWrapper {
            val serializedMap = parcel.readString()
            val map = deserializeMap(serializedMap) ?: emptyMap()
            return DataWrapper(map)
        }

        override fun DataWrapper.write(parcel: Parcel, flags: Int) {
            val serializedMap = serializeMap(data)
            parcel.writeString(serializedMap)
        }

        private fun serializeMap(map: Map<String, Any>): String? {
            return try {
                val byteArrayOutputStream = ByteArrayOutputStream()
                ObjectOutputStream(byteArrayOutputStream).use { objectOutputStream ->
                    objectOutputStream.writeObject(map)
                }
                byteArrayOutputStream.toString("ISO-8859-1")
            } catch (e: Exception) {
                null
            }
        }
        @Suppress("UNCHECKED_CAST")
        private fun deserializeMap(serializedMap: String?): Map<String, Any>? {
            if (serializedMap == null) return null
            return try {
                val byteArrayInputStream = ByteArrayInputStream(serializedMap.toByteArray(charset("ISO-8859-1")))
                ObjectInputStream(byteArrayInputStream).use { objectInputStream ->
                    objectInputStream.readObject() as? Map<String, Any>
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}
