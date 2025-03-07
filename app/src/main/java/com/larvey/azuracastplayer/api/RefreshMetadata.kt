package com.larvey.azuracastplayer.api

import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.runtime.MutableState
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.larvey.azuracastplayer.classes.data.StationJSON
import com.larvey.azuracastplayer.utils.fixHttps
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path


interface RetroFitStatic {
  @GET("/api/nowplaying_static/{shortCode}.json")
  fun getStaticJSON(
    @Path("shortCode") shortCode: String,
  ): Call<StationJSON>
}

fun refreshMetadata(
  staticDataMap: MutableMap<Pair<String, String>, StationJSON>,
  url: String, shortCode: String,
  mountURI: String?,
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

  staticCall.enqueue(object : Callback<StationJSON?> {
    @OptIn(UnstableApi::class)
    override fun onResponse(
      call: Call<StationJSON?>,
      response: Response<StationJSON?>
    ) {
      if (response.isSuccessful) {
        val data = response.body() ?: return

        staticData.value = staticDataMap.put(
          Pair(
            url,
            shortCode
          ),
          data
        )

        val metaData = MediaMetadata.Builder()
          .setMediaType(MEDIA_TYPE_MUSIC) // idk if this does anything
          .setDisplayTitle(data.nowPlaying.song.title)
          .setSubtitle(data.nowPlaying.song.artist) // Android Auto for some reason uses Subtitle as the Artist
          .setArtist(data.nowPlaying.song.artist)
          .setAlbumTitle(data.nowPlaying.song.album)
          .setAlbumArtist(data.nowPlaying.song.artist)
          .setDescription(data.nowPlaying.song.album) // Android Auto for some reason uses Description as the Album name
          .setGenre(data.nowPlaying.song.genre)
          .setArtworkUri(data.nowPlaying.song.art.fixHttps().toUri())
          .setDurationMs(data.nowPlaying.duration.toLong() * 1000) // Gaming
          .build()

        val newMedia = MediaItem.Builder()
          .setMediaId(mountURI.toString())
          .setMediaMetadata(metaData)
          .build()

        val mediaItems = listOf(newMedia)

        val updatedMediaItems =
          mediaItems.map { it.buildUpon().setUri(it.mediaId).build() }.toMutableList()

        mediaPlayer?.replaceMediaItem(
          0,
          updatedMediaItems[0]
        )

        if (reset) {
          mediaPlayer?.prepare()
          mediaPlayer?.play()
        }
      }
    }

    override fun onFailure(
      call: Call<StationJSON?>, t: Throwable
    ) {
      Log.d(
        "DEBUG",
        "Fuck"
      )
      return
    }
  })


}

