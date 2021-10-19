package com.udavpit.fp.cats.effect.concurrency

import cats.effect.{IO, IOApp}

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import scala.util.Try

import com.udavpit.fp.cats.effect.util._

object AsyncIOs extends IOApp.Simple {

  // IOs can run asynchronously on fibers, without having to manually manage the fiber lifecycle
  val threadPool                    = Executors.newFixedThreadPool(8)
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(threadPool)
  type Callback[A] = Either[Throwable, A] => Unit

  def computeMeaningOfLife(): Int = {
    Thread.sleep(1000)
    println(s"[${Thread.currentThread().getName}] computing the meaning of life on some other thread...")
    42
  }

  def computeMeaningOfLifeEither(): Either[Throwable, Int] = Try {
    computeMeaningOfLife()
  }.toEither

  def computeMolOnThreadPool(): Unit =
    threadPool.execute(() => computeMeaningOfLife())

  // lift computation to an IO
  // async is a FFI
  val asyncMolIO: IO[Int] = IO.async_ {
    (cb: Callback[Int]) => // CE thread blocks (semantically) until this cb is invoked (by some other thread)
      threadPool.execute { () => // computation not managed by CE
        val result = computeMeaningOfLifeEither()
        cb(result) // CE thread is notified with the result
      }
  }

  override def run: IO[Unit] = asyncMolIO.debug >> IO(threadPool.shutdown())
}
