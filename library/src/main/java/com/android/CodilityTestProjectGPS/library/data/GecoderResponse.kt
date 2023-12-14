package com.android.CodilityTestProjectGPS.library.data

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class GecoderResponse (
    @SerializedName("place_id")
    val place_Id : Int,
    @SerializedName ("display_name")
    val display_name : String,
    @SerializedName("address")
    val address_Name : JsonObject,
)