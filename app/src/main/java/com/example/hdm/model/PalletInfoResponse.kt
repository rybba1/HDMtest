// Ścieżka pliku: com/example/hdm/model/PalletInfoResponse.kt

package com.example.hdm.model

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class AttachedImageBase64(
    val filename: String? = null,
    val data: String? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class PalletInfoResponse(
    @SerialName("_id")
    val id: String? = null,
    @SerialName("pallet_number_raw")
    val palletNumberRaw: String? = null,
    @SerialName("pallet_number")
    val palletNumber: String? = null,
    @SerialName("lot_number")
    val lotNumber: String? = null,
    @SerialName("item_symbol")
    val itemSymbol: String? = null,
    @SerialName("logical_warehouse")
    val logicalWarehouse: String? = null,
    @SerialName("damage_type_key")
    val damageTypeKey: String? = null,
    val details: String? = null,
    @SerialName("report_ref_no")
    val reportRefNo: String? = null,
    @SerialName("report_pic_wh")
    val reportPicWh: String? = null,
    val place: String? = null,
    @SerialName("report_type")
    val reportType: String? = null,
    @SerialName("repack_done")
    val repackDone: Boolean? = null,
    val accepted: Boolean? = null,
    @SerialName("PhotoLabel")
    val photoLabel: List<String> = emptyList(),
    @SerialName("PhotosDamage")
    val photosDamage: List<String> = emptyList(),
    @SerialName("PhotosOverview")
    val photosOverview: List<String> = emptyList(),
    val status: String? = null,
    @SerialName("report_datetime")
    val reportDatetime: String? = null,
    @SerialName("container_number")
    val containerNumber: String? = null,
    @SerialName("country_of_origin")
    val countryOfOrigin: String? = null,
    @SerialName("timestamp_added")
    val timestampAdded: String? = null,
    @SerialName("attached_images_base64")
    val attachedImages: List<AttachedImageBase64> = emptyList(),
    val error: String? = null
)