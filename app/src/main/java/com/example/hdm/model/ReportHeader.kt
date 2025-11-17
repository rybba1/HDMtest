package com.example.hdm.model

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
@Parcelize
data class ReportHeader(
    var reportType: String = "",
    var magazynier: String = "",
    var workerId: Int? = null,
    var miejsce: String = "",
    var lokalizacja: String = "",
    var rodzajWozka: String = "",
    var dataGodzina: Long = System.currentTimeMillis(),
    var rodzajPalet: String = "",
    var rodzajSamochodu: String = "",
    var numerAuta: String = "",
    var numerNaczepyKontenera: String = "",
    var numerDokumentuFeniks: String = "",
    var opisZdarzenia: String = "",
    var podpisOdbierajacegoUri: String? = null,
    var podpisData: Long? = null,
    val numerKontenera: String = "",
    var placementSchematicUri: String? = null,
    val zdjecieDuzejEtykietyBase64: String? = null,
    val zdjecieCalejPaletyBase64: String? = null,
    val damageMarkingBitmapBase64: String? = null
) : Parcelable