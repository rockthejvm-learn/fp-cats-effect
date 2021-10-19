package com.udavpit.fp.cats.effect.polymorphic

import cats.{Applicative, Monad}
import cats.effect.{IO, IOApp}

object MonadCancellation extends IOApp.Simple {

  trait MyApplicativeError[F[_], E] extends Applicative[F] {
    def raiseError[A](error: E): F[A]
    def handleErrorWith[A](fa: F[A])(f: E => F[A]): F[A]
  }

  trait MyMonadError[F[_], E] extends MyApplicativeError[F, E] with Monad[F]

  // MonadCancel describes the capability to cancel & prevent cancellation

  override def run: IO[Unit] = ???
}
