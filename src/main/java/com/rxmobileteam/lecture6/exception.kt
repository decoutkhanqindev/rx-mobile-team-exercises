package com.rxmobileteam.lecture6

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.*

private val count = AtomicInteger()

suspend fun maybeFailedFunction(): Int {
  delay(100)

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
  val scope = CoroutineScope(Dispatchers.Default + Job())

  fun doSomething() {
    // TODO: Implement exception handling when calling maybeFailedFunction()
    // Call logger.log() with the result of maybeFailedFunction()
    // Call logger.logError() with the exception and a message
    // Note: You must preserve the cancellation semantics of the coroutine

    val handler = CoroutineExceptionHandler { _, throwable ->
      logger.logError(throwable, "maybeFailedFunction() is failed")
    }

    scope.launch(handler) {
      try {
        val result = maybeFailedFunction()
        logger.log(result.toString())
      } catch (e: Exception) {
        logger.logError(e,"Unexpected error")
      }
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
  delay(100)
  model.cancelAndJoinBlocking()

  delay(5_000)
  // At this point, the coroutine should have been cancelled
  // Nothing should be printed to the console
}
