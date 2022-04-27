package com.udavpit.fp.cats.effect.coordination

import cats.effect.kernel.{Deferred, Ref}
import cats.effect.{IO, IOApp}
import cats.syntax.traverse._

import scala.concurrent.duration._

object Defers extends IOApp.Simple {

  import com.udavpit.fp.cats.effect.util._

  // deferred is a primitive for waiting for an effect, while some other effect completes with a value

  val aDeferred: IO[Deferred[IO, Int]]    = Deferred[IO, Int]
  val aDeferred_v2: IO[Deferred[IO, Int]] = IO.deferred[Int] // same

  // get blocks the calling fiber (semantically) until some other fiber completes the Deferred with a value
  val reader: IO[Int] = aDeferred.flatMap { signal =>
    signal.get // blocks the fiber
  }

  val writer = aDeferred.flatMap { signal =>
    signal.complete(42)
  }

  def demoDeferred(): IO[Unit] = {
    def consumer(signal: Deferred[IO, Int]) = for {
      _             <- IO("[consumer] waiting for result...").debug
      meaningOfLife <- signal.get // blocker
      _             <- IO(s"[consumer] got the result: $meaningOfLife").debug
    } yield ()

    def producer(signal: Deferred[IO, Int]) = for {
      _             <- IO("[producer] crunching numbers...").debug
      _             <- IO.sleep(1.second)
      _             <- IO("[producer] complete: 42").debug
      meaningOfLife <- IO(42)
      _             <- signal.complete(meaningOfLife)
    } yield ()

    for {
      signal      <- Deferred[IO, Int]
      fibConsumer <- consumer(signal).start
      fibProducer <- producer(signal).start
      _           <- fibProducer.join
      _           <- fibConsumer.join
    } yield ()
  }

  // simulate downloading some content
  val fileParts = List("I ", "love S", "cala", " with Cat", "s Effect!<EOF>")

  def fileNotifierWithRef(): IO[Unit] = {
    def downloadFile(contentRef: Ref[IO, String]): IO[Unit] =
      fileParts
        .map { part =>
          IO(s"[downloader] got '$part'").debug >> IO.sleep(1.second) >> contentRef.update(currentContent =>
            currentContent + part
          )
        }
        .sequence
        .void

    def notifyFileComplete(contentRef: Ref[IO, String]): IO[Unit] = for {
      file <- contentRef.get
      _ <-
        if (file.endsWith("<EOF>")) IO("[notifier] File download complete").debug
        else
          IO("[notifier] downloading...").debug >> IO.sleep(500.millis) >> notifyFileComplete(
            contentRef
          ) // busy wait!
    } yield ()

    for {
      contentRef    <- Ref[IO].of("")
      fibDownloader <- downloadFile(contentRef).start
      notifier      <- notifyFileComplete(contentRef).start
      _             <- fibDownloader.join
      _             <- notifier.join
    } yield ()
  }

  override def run: IO[Unit] = fileNotifierWithRef()
}
