package com.larvey.azuracastplayer.api

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.larvey.azuracastplayer.classes.data.StationJSON
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path


interface RetroFitInitial {
  @GET("/api/nowplaying_static/{shortCode}.json")
  fun getStaticJSON(
    @Path("shortCode") shortCode: String,
  ): Call<StationJSON>
}

fun fetchStationData(
  staticDataMap: MutableMap<Pair<String, String>, StationJSON>,
  url: String,
  shortCode: String
) {
  val okHttpClient = OkHttpClient.Builder().build()

  val retroFit = Retrofit.Builder()
    .baseUrl("https://$url")
    .client(okHttpClient)
    .addConverterFactory(GsonConverterFactory.create())
    .build()

  val retroFitAPI = retroFit.create(RetroFitInitial::class.java)

  val staticCall: Call<StationJSON> = retroFitAPI.getStaticJSON(shortCode)

  staticCall!!.enqueue(object : Callback<StationJSON?> {
    @OptIn(UnstableApi::class)
    override fun onResponse(
      call: Call<StationJSON?>,
      response: Response<StationJSON?>
    ) {
      if (response.isSuccessful) {
        val data = response.body() ?: return
        staticDataMap[Pair(
          url,
          shortCode
        )] = data

      }
    }

    override fun onFailure(
      call: Call<StationJSON?>, t: Throwable
    ) {
      Log.d(
        "DEBUG",
        "Fuck"
      )
      Log.d(
        "DEBUG",
        t.toString()
      )
      return
    }
  })


}

