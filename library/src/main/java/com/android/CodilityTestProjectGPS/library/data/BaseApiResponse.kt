package com.android.CodilityTestProjectGPS.library.data

import android.content.Context
import android.util.Log
import com.android.CodilityTestProjectGPS.library.util.PreferenceUtil
import retrofit2.Response
import java.lang.Exception

abstract class BaseApiResponse(private val context: Context) {

    suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): NetworkResult<T> {

        if (PreferenceUtil.hasInternetConnection(context)) {
            try {


                val response = apiCall()
                Log.d("BaseApiResponse :","got resp :"+response.message())
                if (response.isSuccessful) {
                    val body = response.body()
                    body?.let {
                        return NetworkResult.Success(body)
                    }
                }
                return error("${response.code()} ${response.message()}")
            } catch (e: Exception) {
                return error(e.message ?: e.toString())
            }
        }
        return error("No internet connection !")
    }

    private fun <T> error(errorMessage: String): NetworkResult<T> =
        NetworkResult.Error("Network call failed : $errorMessage")

}