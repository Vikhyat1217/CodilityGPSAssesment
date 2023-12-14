package com.android.CodilityTestProjectGPS.library.data

import javax.inject.Inject

class RemoteDataSource @Inject constructor( private val apiInterface: ApiInterface?) {
    suspend fun getReverseGecoding(latitude: Double, longitude: Double) = apiInterface!!.getReverseGeocoding(latitude,longitude)
}