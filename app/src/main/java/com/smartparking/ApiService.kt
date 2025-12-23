package com.smartparking

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("api/add_parking") // Ensure the correct API route
    fun addParking(@Body parkingData: ParkingData): Call<ApiResponse>

}
