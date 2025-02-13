package com.larvey.azuracastplayer.api

import android.util.Log
import com.larvey.azuracastplayer.classes.data.StationJSON
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface RetroFitPlaying {
  @GET("/api/nowplaying")
  fun getNowPlaying(): Call<List<StationJSON>>
}

fun findHostsStations(stationData: MutableMap<String, List<StationJSON>>, url: String) {
  val okHttpClient = OkHttpClient.Builder().build()
  val retroFit = Retrofit.Builder()
    .baseUrl("https://$url")
    .client(okHttpClient)
    .addConverterFactory(GsonConverterFactory.create())
    .build()

  val retroFitAPI = retroFit.create(RetroFitPlaying::class.java)

  val nowPlayingCall: Call<List<StationJSON>> = retroFitAPI.getNowPlaying()

  nowPlayingCall!!.enqueue(object : Callback<List<StationJSON>?> {
    override fun onResponse(
      call: Call<List<StationJSON>?>,
      response: Response<List<StationJSON>?>
    ) {
      if (response.isSuccessful) {
        val nowPlayingData: List<StationJSON> = response.body()!!
        if (nowPlayingData.isEmpty()) return

        stationData.put(
          url,
          nowPlayingData
        )
      }
    }

    override fun onFailure(
      call: Call<List<StationJSON>?>,
      t: Throwable
    ) {
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