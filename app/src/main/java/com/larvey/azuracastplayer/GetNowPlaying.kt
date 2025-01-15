package com.larvey.azuracastplayer

import android.util.Log
import retrofit2.Callback
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface RetroFitPlaying {
  @GET("/api/nowplaying")
  fun getNowPlaying(): Call<List<StationJSON>>
}

fun getNowPlaying(stationData: MutableMap<String, StationJSON>, url: String) {
  val okHttpClient = OkHttpClient.Builder().build()

  val retroFit = Retrofit.Builder()
    .baseUrl(url)
    .client(okHttpClient)
    .addConverterFactory(GsonConverterFactory.create())
    .build()

  val retroFitAPI = retroFit.create(RetroFitPlaying::class.java)

  val nowPlayingCall: Call<List<StationJSON>> = retroFitAPI.getNowPlaying()

  nowPlayingCall!!.enqueue(object : Callback<List<StationJSON>?> {
    override fun onResponse(
      call: Call<List<StationJSON>?>,
      response: retrofit2.Response<List<StationJSON>?>
    ) {
      if (response.isSuccessful) {
        val nowPlayingData: List<StationJSON> = response.body()!!
        if (nowPlayingData.isEmpty()) return

        stationData.put(url, nowPlayingData[0])
      }

    }

    override fun onFailure(
      call: Call<List<StationJSON>?>,
      t: Throwable
    ) {
      Log.d("DEBUG", "Fuck")
      return
    }
  })

  }