package com.polypulse.app.data.remote

import com.polypulse.app.data.remote.dto.AlertResponse
import retrofit2.http.GET

interface BackendApi {
    @GET("alerts")
    suspend fun getAlerts(): AlertResponse
}
