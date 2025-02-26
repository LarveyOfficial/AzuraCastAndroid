package com.larvey.azuracastplayer.api

import android.util.Log
import androidx.compose.runtime.MutableState
import com.larvey.azuracastplayer.classes.data.DiscoveryJSON
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface RetroFitDiscovery {
  @GET("azuracastDiscovery.json")
  fun getDiscoveryJSON(): Call<DiscoveryJSON>
}

fun fetchDiscoveryJSON(discoveryData: MutableState<DiscoveryJSON?>) {
  val okHttpClient = OkHttpClient.Builder().build()

  val retroFit = Retrofit.Builder()
    .baseUrl("https://owcramer.github.io/AzuraCast-Discovery-API/")
    .client(okHttpClient)
    .addConverterFactory(GsonConverterFactory.create())
    .build()

  val retroFitAPI = retroFit.create(RetroFitDiscovery::class.java)

  val discoveryCall: Call<DiscoveryJSON> = retroFitAPI.getDiscoveryJSON()

  discoveryCall.enqueue(object : Callback<DiscoveryJSON?> {
    override fun onResponse(
      call: Call<DiscoveryJSON?>,
      response: Response<DiscoveryJSON?>
    ) {
      if (response.isSuccessful) {
        response.body()?.let {
          discoveryData.value = it
        }
      }
    }

    override fun onFailure(call: Call<DiscoveryJSON?>, t: Throwable) {
      Log.d(
        "DEBUG",
        t.toString()
      )
      Log.d(
        "DEBUG",
        "Fuck"
      )
      return
    }

  })
}