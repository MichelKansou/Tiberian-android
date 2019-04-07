package com.kansou.tiberian.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.io.Serializable
import java.util.*

@Parcelize
data class KeyModel (var id: Long, var account: String, var issuer: String, var secret: String, val algorithm: String, val period: String, val type: String, val uri: String): Parcelable {
    var creationDate: Date = Date()
}