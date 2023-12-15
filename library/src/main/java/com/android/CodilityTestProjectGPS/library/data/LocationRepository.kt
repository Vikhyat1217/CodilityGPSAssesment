package com.android.CodilityTestProjectGPS.library.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import retrofit2.Retrofit
import javax.inject.Inject

class LocationRepository @Inject constructor(
    private val sharedLocationManager: SharedLocationManager,
    private val remoteDataSource: RemoteDataSource,
    @ApplicationContext context: Context
) : BaseApiResponse(context){
    /**
     * Status of whether the app is actively subscribed to location changes.
     */

    val receivingLocationUpdates: StateFlow<Boolean> =
        sharedLocationManager.receivingLocationUpdates

    @ExperimentalCoroutinesApi
    fun getLocations() = sharedLocationManager.locationFlow()

    suspend fun getGecodingResponse(latitude: Double, longitude: Double): Flow<NetworkResult<GecoderResponse>> {
        return flow {
            Retrofit.Builder()
            emit(safeApiCall { remoteDataSource.getReverseGecoding(latitude,longitude) })
        }.flowOn(Dispatchers.IO)
    }

}