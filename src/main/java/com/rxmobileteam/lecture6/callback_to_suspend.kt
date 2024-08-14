package com.rxmobileteam.lecture6

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random
import kotlinx.coroutines.runBlocking

private class GetDataRequestException(cause: Throwable?) : RuntimeException(cause)

private sealed interface GetDataRequestState {
  data object Idle : GetDataRequestState

  data class Running(
    val executor: ExecutorService,
    val future: Future<Any?>,
    val onCancel: () -> Unit,
  ) : GetDataRequestState

  data class Completed(val result: Result<Any?>) : GetDataRequestState

  data object Cancelled : GetDataRequestState
}

private class GetDataRequest<V>(
  private val block: () -> V,
) {
  // @GuardedBy("this")
  private var state: GetDataRequestState = GetDataRequestState.Idle

  @Synchronized
  fun start(
    onCancel: () -> Unit,
    onResult: (Result<V>) -> Unit,
  ) {
    if (state !is GetDataRequestState.Idle) {
      return
    }

    println("Started...")
    val executor = Executors.newSingleThreadExecutor()
    state = GetDataRequestState.Running(
      executor = executor,
      future = executor.submit(Callable {
        val result = try {
          block().let { Result.success(it) }.also {
            if (Thread.interrupted()) {
              throw InterruptedException()
            }
          }
        } catch (e: InterruptedException) {
          throw e
        } catch (e: Throwable) {
          Result.failure(GetDataRequestException(e))
        }

        synchronized(this@GetDataRequest) { state = GetDataRequestState.Completed(result) }
        executor.shutdown()
        onResult(result)

        result
      }),
      onCancel = onCancel,
    )
  }

  @Synchronized
  fun cancel() = runCatching {
    when (val currentState = state) {
      is GetDataRequestState.Idle,
      is GetDataRequestState.Cancelled,
      -> return

      is GetDataRequestState.Running -> {
        currentState.future.cancel(true)
        currentState.onCancel()
        currentState.executor.shutdown()
        state = GetDataRequestState.Cancelled
        println("Cancelled...")
      }

      is GetDataRequestState.Completed -> return
    }
  }.let { }
}

// TODO: Implement the following extension function (convert callback-based API to suspend function)
// Hint: use kotlin.coroutines.Continuation<T>
private suspend fun <V> GetDataRequest<V>.startAndAwait(): Result<V> = suspendCoroutine { continuation ->
  start(
    onCancel = { continuation.resumeWithException(CancellationException("Operation cancelled")) },
    onResult = { result: Result<V> -> continuation.resume(result) },
  )
}

fun main() {
  GetDataRequest {
    Thread.sleep(500)
    println(">>> after delay")
    if (Random.nextBoolean()) {
      throw RuntimeException("Failed to get data")
    }
    "Data from callback function" // result
  }.start(
    onCancel = { println(">>> onCancel") },
    onResult = { result: Result<String> -> println(">>> onResult: $result") },
  )

  Thread.sleep(1000)
  println("-".repeat(80))

  runBlocking {
    val result = GetDataRequest {
      Thread.sleep(500)
      println(">>> after delay")
      if (Random.nextBoolean()) {
        throw RuntimeException("Failed to get data")
      }
      "Data from suspend function" // result
    }.startAndAwait()

    println(">>> result: $result")
  }
}
