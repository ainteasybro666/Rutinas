package com.example.rutinas.data.model

import android.os.Parcel
import android.os.Parcelable
import java.util.UUID

data class Action(
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val actionType: String,
    val routineId: Long = 0,
    val data: DataWrapper? = null,
    val executionOrder: Int = 0,
    val pauseDuration: Long = 0,
) : Parcelable {
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(uuid)
        parcel.writeString(actionType)
        parcel.writeLong(routineId)
        parcel.writeParcelable(data, flags)
        parcel.writeInt(executionOrder)
        parcel.writeLong(pauseDuration)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Action> {
        override fun createFromParcel(parcel: Parcel): Action {
            return Action(
                parcel.readLong(),
                parcel.readString() ?: UUID.randomUUID().toString(),
                parcel.readString() ?: "",
                parcel.readLong(),
                parcel.readParcelable(DataWrapper::class.java.classLoader),
                parcel.readInt(),
                parcel.readLong()
            )
        }

        override fun newArray(size: Int): Array<Action?> {
            return arrayOfNulls(size)
        }
    }
}
