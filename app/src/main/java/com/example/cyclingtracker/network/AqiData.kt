package com.example.cyclingtracker.network

import com.google.gson.annotations.SerializedName

/**
 * Data classes to parse the JSON response from the WAQI API.
 * We only care about the 'aqi' value inside the 'data' object.
 */
data class AqiResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("data")
    val data: AqiData?
)

data class AqiData(
    @SerializedName("aqi")
    val aqi: Int
)
