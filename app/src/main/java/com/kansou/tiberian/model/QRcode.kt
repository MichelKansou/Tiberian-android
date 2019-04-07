package com.kansou.tiberian.model

import android.os.Parcelable
import com.google.android.gms.vision.barcode.Barcode
import kotlinx.android.parcel.Parcelize

@Parcelize
data class QRcode (val data: Barcode) : Parcelable