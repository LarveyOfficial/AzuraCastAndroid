package com.larvey.azuracastplayer.api

import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.runtime.MutableState
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_RADIO_STATION
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.larvey.azuracastplayer.classes.StationJSON
import okhttp3.OkHttpClient
import retrofit2.http.GET
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Path


interface RetroFitStatic {
  @GET("/api/nowplaying_static/{shortCode}.json")
  fun getStaticJSON(
    @Path("shortCode") shortCode: String,
  ): Call<StationJSON>
}

fun updateSongData(
  staticDataMap: MutableMap<Pair<String, String>, StationJSON>,
  url: String, shortCode: String,
  uri: String?,
  reset: Boolean,
  mediaPlayer: Player? = null,
  staticData: MutableState<StationJSON?>
) {
  val okHttpClient = OkHttpClient.Builder().build()

  val retroFit = Retrofit.Builder()
    .baseUrl("https://$url")
    .client(okHttpClient)
    .addConverterFactory(GsonConverterFactory.create())
    .build()

  val retroFitAPI = retroFit.create(RetroFitStatic::class.java)

  val staticCall: Call<StationJSON> = retroFitAPI.getStaticJSON(shortCode)

  staticCall!!.enqueue(object : Callback<StationJSON?> {
    @OptIn(UnstableApi::class)
    override fun onResponse(
      call: Call<StationJSON?>,
      response: Response<StationJSON?>
    ) {
      if (response.isSuccessful) {
        val data = response.body()
        if (data == null) return
        staticData.value = data
        staticDataMap.put(Pair(url, shortCode), data)
        val metaData = MediaMetadata.Builder()
          .setDisplayTitle(data.nowPlaying.song.title)
          .setArtist(data.nowPlaying.song.artist)
          .setMediaType(MEDIA_TYPE_RADIO_STATION)
          .setAlbumTitle(data.nowPlaying.song.album)
          .setArtworkUri(Uri.parse(data.nowPlaying.song.art))
          .setDurationMs(1) // Forces the cool squiggly line
          .setGenre(data.nowPlaying.song.genre)
          .build()

        val newMedia = MediaItem.Builder()
          .setMediaId(uri.toString())
          .setMediaMetadata(metaData)
          .build()

        mediaPlayer?.replaceMediaItem(0, newMedia)

        if (reset == true) {
          mediaPlayer?.prepare()
          mediaPlayer?.play()
        }

      }
    }

    override fun onFailure(
      call: Call<StationJSON?>, t: Throwable
    ) {
      Log.d("DEBUG", "Fuck")
      return
    }
  })


}

