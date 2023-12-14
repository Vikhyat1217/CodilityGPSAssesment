package com.android.CodilityTestProjectGPS.library.data

import com.android.CodilityTestProjectGPS.library.util.Constants
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiInterface {
    @GET(Constants.REVERSE_GEOCODING_URL)
    suspend fun getReverseGeocoding(@Query("lat") latitude: Double, @Query("lon") longitude: Double): Response<GecoderResponse>
}