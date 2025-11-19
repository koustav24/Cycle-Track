package com.example.cyclingtracker.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AqiApiService {
    /**
     * Fetches Air Quality Index data based on geographical coordinates.
     * API documentation: [https://aqicn.org/json-api/doc/](https://aqicn.org/json-api/doc/)
     * Example URL: /feed/geo:lat;lng/?token=YOUR_TOKEN
     */
    @GET("feed/geo:{lat};{lng}/")
    suspend fun getAqi(
        @Path("lat") lat: Double,
        @Path("lng") lng: Double,
        @Query("token") token: String
    ): AqiResponse
}
