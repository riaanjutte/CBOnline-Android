package io.github.riaanjutte.cbonline.data

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

/**
 * Runs the call and returns its body; throws `IOException("HTTP <code>")` for an error answer. Cancelling the
 * coroutine cancels the request, even mid-body: the body is read in OkHttp's callback, never by the caller.
 */
internal suspend fun Call.bodyOrThrow(): String = suspendCancellableCoroutine { cont ->
    cont.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = cont.resumeWith(Result.failure(e))

        override fun onResponse(call: Call, response: Response) = cont.resumeWith(
            runCatching {
                response.use {
                    if (!it.isSuccessful) throw IOException("HTTP ${it.code}")
                    it.body?.string().orEmpty()
                }
            }
        )
    })
}
