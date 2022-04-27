package com.udavpit.fp.cats.effect.concurrency

import cats.effect.kernel.Outcome.{Canceled, Errored, Succeeded}
import cats.effect.{IO, IOApp}

import scala.concurrent.duration._
import com.udavpit.fp.cats.effect.util._

object FibersExercises extends IOApp.Simple {

  /** Exercises:
    *   1. Write a function that runs an IO on another thread, and, depending on the result of the fiber
    *      - return the result in an IO
    *      - if errored or cancelled, return a failed IO
    *
    * 2. Write a function that takes two IOs, runs them on different fibers and returns an IO with a tuple
    * containing both results.
    *   - if both IOs complete successfully, tuple their results
    *   - if the first IO returns an error, raise that error (ignoring the second IO's result/error)
    *   - if the first IO doesn't error but second IO returns an error, raise that error
    *   - if one (or both) canceled, raise a RuntimeException
    *
    * 3. Write a function that adds a timeout to an IO:
    *   - IO runs on a fiber
    *   - if the timeout duration passes, then the fiber is canceled
    *   - the method returns an IO[A] which contains
    *     - the original value if the computation is successful before the timeout signal
    *     - the exception if the computation is failed before the timeout signal
    *     - a RuntimeException if it times out (i.e. cancelled by the timeout)
    */

  // 1
  def processResultsFromFiber[A](io: IO[A]): IO[A] = {
    val ioResult = for {
      fib    <- io.debug.start
      result <- fib.join
    } yield result

    ioResult.flatMap {
      case Succeeded(fa) => fa
      case Errored(e)    => IO.raiseError(e)
      case Canceled()    => IO.raiseError(new RuntimeException("Computation canceled."))
    }
  }

  def testEx1() = {
    val aComputation = IO("starting").debug >> IO.sleep(1.second) >> IO("done!").debug >> IO(42)
    processResultsFromFiber(aComputation).void
  }

  // 2
  def tupleIOs[A, B](ioa: IO[A], iob: IO[B]): IO[(A, B)] = {
    val result = for {
      fiba    <- ioa.start
      fibb    <- iob.start
      resulta <- fiba.join
      resultb <- fibb.join
    } yield (resulta, resultb)

    result.flatMap {
      case (Succeeded(fa), Succeeded(fb)) =>
        for {
          a <- fa
          b <- fb
        } yield (a, b)
      case (Errored(e), _) => IO.raiseError(e)
      case (_, Errored(e)) => IO.raiseError(e)
      case _               => IO.raiseError(new RuntimeException("Some computation canceled."))
    }
  }

  def testEx2() = {
    val firstIO  = IO.sleep(2.seconds) >> IO(1).debug
    val secondIO = IO.sleep(3.seconds) >> IO(2).debug
    tupleIOs(firstIO, secondIO).debug.void
  }

  // 3
  def timeout[A](io: IO[A], duration: FiniteDuration): IO[A] = {
    val computation = for {
      fib    <- io.start
      _      <- (IO.sleep(duration) >> fib.cancel).start // careful - fibers can leak
      result <- fib.join
    } yield result

    computation.flatMap {
      case Succeeded(fa) => fa
      case Errored(e)    => IO.raiseError(e)
      case Canceled()    => IO.raiseError(new RuntimeException("Computation canceled."))
    }
  }

  def testEx3() = {
    val aComputation = IO("starting").debug >> IO.sleep(1.second) >> IO("done!").debug >> IO(42)
    timeout(aComputation, 500.millis).debug.void
  }

  override def run: IO[Unit] = testEx3()
}
