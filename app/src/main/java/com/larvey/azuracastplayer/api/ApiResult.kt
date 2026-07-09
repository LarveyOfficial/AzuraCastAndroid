package com.larvey.azuracastplayer.api

import retrofit2.Response
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * The outcome of a network call made through [AzuraCastRepository].
 *
 * Callers are expected to handle both branches with an exhaustive `when`:
 * - [Success] carries the parsed response body.
 * - [Failure] distinguishes *what went wrong* so callers can react (or log)
 *   appropriately:
 *   - [Failure.Http] — the server answered, but not with a usable 2xx body
 *     (non-2xx status, or a 2xx with an empty body).
 *   - [Failure.Network] — the request never completed (no connectivity,
 *     timeout, DNS failure, connection reset, …).
 *   - [Failure.Unexpected] — anything else, e.g. a malformed JSON body that
 *     Gson could not parse.
 */
sealed interface ApiResult<out T> {

  data class Success<T>(val data: T) : ApiResult<T>

  sealed interface Failure : ApiResult<Nothing> {

    /** The server responded with a non-usable status ([code]) for this call. */
    data class Http(val code: Int, val message: String) : Failure

    /** The request failed at the transport layer and never got a response. */
    data class Network(val cause: IOException) : Failure

    /** An error outside the expected HTTP/transport failure modes. */
    data class Unexpected(val cause: Throwable) : Failure
  }
}

/**
 * Executes [block] and maps the [Response] into an [ApiResult].
 *
 * [CancellationException] is deliberately rethrown rather than wrapped:
 * swallowing it would break structured concurrency (a cancelled caller must
 * observe its own cancellation).
 */
internal suspend fun <T> safeApiCall(block: suspend () -> Response<T>): ApiResult<T> {
  return try {
    val response = block()
    val body = response.body()
    when {
      !response.isSuccessful -> ApiResult.Failure.Http(
        code = response.code(),
        message = response.message()
      )

      body == null -> ApiResult.Failure.Http(
        code = response.code(),
        message = "Empty response body"
      )

      else -> ApiResult.Success(body)
    }
  } catch (e: CancellationException) {
    throw e
  } catch (e: IOException) {
    ApiResult.Failure.Network(e)
  } catch (e: Throwable) {
    ApiResult.Failure.Unexpected(e)
  }
}
