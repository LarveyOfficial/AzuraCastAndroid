package com.larvey.azuracastplayer.api

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.Response
import java.io.IOException

/**
 * Verifies the [safeApiCall] mapping from Retrofit [Response]s (and thrown
 * exceptions) to [ApiResult] values.
 */
class ApiResultTest {

  @Test
  fun `2xx with body maps to Success`() = runTest {
    val result = safeApiCall { Response.success("payload") }

    assertThat(result).isEqualTo(ApiResult.Success("payload"))
  }

  @Test
  fun `2xx with null body maps to Http failure`() = runTest {
    val result = safeApiCall { Response.success<String>(null) }

    assertThat(result).isInstanceOf(ApiResult.Failure.Http::class.java)
    assertThat((result as ApiResult.Failure.Http).code).isEqualTo(200)
  }

  @Test
  fun `non-2xx maps to Http failure with status code`() = runTest {
    val result = safeApiCall {
      Response.error<String>(
        404,
        "not found".toResponseBody()
      )
    }

    assertThat(result).isInstanceOf(ApiResult.Failure.Http::class.java)
    assertThat((result as ApiResult.Failure.Http).code).isEqualTo(404)
  }

  @Test
  fun `IOException maps to Network failure`() = runTest {
    val boom = IOException("no route to host")

    val result = safeApiCall<String> { throw boom }

    assertThat(result).isInstanceOf(ApiResult.Failure.Network::class.java)
    assertThat((result as ApiResult.Failure.Network).cause).isSameInstanceAs(boom)
  }

  @Test
  fun `unexpected exception maps to Unexpected failure`() = runTest {
    val boom = IllegalStateException("malformed body")

    val result = safeApiCall<String> { throw boom }

    assertThat(result).isInstanceOf(ApiResult.Failure.Unexpected::class.java)
    assertThat((result as ApiResult.Failure.Unexpected).cause).isSameInstanceAs(boom)
  }

  @Test
  fun `CancellationException is rethrown, not wrapped`() = runTest {
    // Swallowing cancellation would break structured concurrency — a cancelled
    // caller must observe its own cancellation.
    assertThrows(CancellationException::class.java) {
      kotlinx.coroutines.runBlocking {
        safeApiCall<String> { throw CancellationException("cancelled") }
      }
    }
  }
}
