package org.example.geoapp.util

import javafx.application.Platform
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import retrofit2.Call

// Расширение для преобразования Call в Deferred (suspend)
suspend fun <T> Call<T>.awaitVoid(): T = suspendCancellableCoroutine { continuation ->
    enqueue(object : retrofit2.Callback<T> {
        override fun onResponse(call: Call<T>, response: retrofit2.Response<T>) {
            if (response.isSuccessful) {
                continuation.resume(response.body()!!)
            } else {
                continuation.resumeWithException(RuntimeException("HTTP ${response.code()}"))
            }
        }
        override fun onFailure(call: Call<T>, t: Throwable) {
            continuation.resumeWithException(t)
        }
    })
    continuation.invokeOnCancellation { this@awaitVoid.cancel() }
}

// Запуск корутины с автоматическим переключением на JavaFX thread
fun runOnFx(block: suspend CoroutineScope.() -> Unit): Job {
    return CoroutineScope(Dispatchers.Main).launch {
        block()
    }
}

// Общая версия await для Call<T>
suspend fun <T> Call<T>.await(): T = suspendCancellableCoroutine { continuation ->
    enqueue(object : retrofit2.Callback<T> {
        override fun onResponse(call: Call<T>, response: retrofit2.Response<T>) {
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    continuation.resume(body)
                } else {
                    // Для случаев с пустым телом (например, Void) - вернуть Unit через исключение
                    continuation.resumeWithException(RuntimeException("Empty response body"))
                }
            } else {
                continuation.resumeWithException(RuntimeException("HTTP ${response.code()}"))
            }
        }

        override fun onFailure(call: Call<T>, t: Throwable) {
            continuation.resumeWithException(t)
        }
    })
    continuation.invokeOnCancellation { this@await.cancel() }
}