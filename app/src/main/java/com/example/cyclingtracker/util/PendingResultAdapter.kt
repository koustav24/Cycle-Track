package com.example.cyclingtracker.util

import com.google.maps.PendingResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Awaits the result of a Google Maps Services API PendingResult without blocking a thread.
 *
 * This is a standard coroutine wrapper for a callback-based API.
 */
suspend fun <T> PendingResult<T>.await(): T {
    return suspendCancellableCoroutine { continuation ->
        setCallback(object : PendingResult.Callback<T> {
            override fun onResult(result: T) {
                if (continuation.isActive) {
                    continuation.resume(result)
                }
            }

            override fun onFailure(e: Throwable) {
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }
        })

        continuation.invokeOnCancellation {
            cancel()
        }
    }
}
