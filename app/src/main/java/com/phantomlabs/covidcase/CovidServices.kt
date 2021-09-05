package com.phantomlabs.covidcase

import retrofit2.Call
import retrofit2.http.GET

interface CovidServices {

    @GET("us/daily.json")
    fun getNationalData() : Call<List<CovidData>>

    @GET("state/daily.json")
    fun getStateData() : Call<List<CovidData>>

}