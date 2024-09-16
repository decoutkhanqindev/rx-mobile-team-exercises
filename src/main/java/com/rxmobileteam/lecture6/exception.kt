package com.rxmobileteam.lecture6

import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.EmptyCoroutineContext.fold
import kotlinx.coroutines.*

private val count = AtomicInteger()

suspend fun maybeFailedFunction(): Int {
  delay(1000)

  if (count.incrementAndGet() % 2 == 0) {
    throw RuntimeException("Failed")
  } else {
    return 42
  }
}

class Logger {
  fun log(message: String) {
    println(message)
  }

  fun logError(throwable: Throwable, message: String) {
    println("$message: $throwable")
  }
}

class DemoModel(
  private val logger: Logger = Logger(),
) {
  val scope = CoroutineScope(Dispatchers.Default + Job() + CoroutineExceptionHandler { coroutineContext, throwable ->
    logger.logError(throwable, "maybeFailedFunction()")
  }) // + CoroutineExceptionHandler ... -> general exception handling

  fun doSomething() {
    // TODO: Implement exception handling when calling maybeFailedFunction()
    // Call logger.log() with the result of maybeFailedFunction()
    // Call logger.logError() with the exception and a message
    // Note: You must preserve the cancellation semantics of the coroutine

    /* normal style handle exceptions
    scope.launch { // handle special exceptions -> flexible
      try {
        val result: Int = maybeFailedFunction()
        logger.log("Result = $result")
      } catch (cancel: CancellationException) {
        throw cancel
      } catch (e: Exception) {
        logger.log(e.message.toString())
      }
    }
    */

    // or
    // functional style handle exceptions
    scope.launch {
      runCatching { maybeFailedFunction() }
        .fold(
          onSuccess = { result: Int -> logger.log("Result = $result") },
          onFailure = { e: Throwable -> {
              if (e is CancellationException) throw e
              logger.log(e.toString())
            }
          }
        )
    }
  }

  fun cancelAndJoinBlocking() {
    runBlocking {
      scope.coroutineContext.job.cancelAndJoin()
    }
    logger.log("Cancelled")
  }
}

fun main() = runBlocking {
  val model = DemoModel()

  model.doSomething()
  delay(1000)
  model.cancelAndJoinBlocking()

  delay(5_000)
  // At this point, the coroutine should have been cancelled
  // Nothing should be printed to the console
}
