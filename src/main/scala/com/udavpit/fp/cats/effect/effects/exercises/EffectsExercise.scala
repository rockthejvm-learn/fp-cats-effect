package com.udavpit.fp.cats.effect.effects.exercises

import scala.io.StdIn

object EffectsExercise {

  case class MyIO[A](unsafeRun: () => A) {
    def map[B](f: A => B): MyIO[B] =
      MyIO(() => f(unsafeRun()))

    def flatMap[B](f: A => MyIO[B]): MyIO[B] =
      MyIO(() => f(unsafeRun()).unsafeRun())
  }

  /**
   *  Exercises
   *  1. An IO which returns the current time of the system
   *  2. An IO which measures the duration of a computation (hint: use ex 1)
   *  3. An IO which prints something to the console
   *  4. An IO which reads a line (a string) from the std input
   */

  // 1
  val clock: MyIO[Long] = MyIO(() => System.currentTimeMillis())

  // 2
  def measure[A](computation: MyIO[A]): MyIO[Long] = for {
    startTime <- clock
    _ <- computation
    finishTime <- clock
  } yield finishTime - startTime

  /*
    Deconstruction:
    clock.flatMap(startTime => computation.flatMap(_ => clock.map(finishTime => finishTime - startTime)))

    Part 3:
    clock.map(finishTime => finishTime - startTime) = MyIO(() => System.currentTimeMillis() - startTime)
    => clock.flatMap(startTime => computation.flatMap(_ => MyIO(() => System.currentTimeMillis() - startTime)))

    Part 2:
    computation.flatMap(lambda) = MyIO(() => lambda(___COMP___).unsafeRun())
                                = MyIO(() => MyIO(() => System.currentTimeMillis() - startTime)).unsafeRun())
                                = MyIO(() => System.currentTimeMillis_after_computation() - startTime)

    Part 1:
    clock.flatMap(startTime => MyIO(() => System.currentTimeMillis_after_computation() - startTime))
    = MyIO(() => MyIO(() => System.currentTimeMillis_after_computation() - System.currentTimeMillis()).unsafeRun())
    = MyIO(() => System.currentTimeMillis_after_computation() - System.currentTimeMillis_at_start())

    Conclusion:
    Deconstructing effects manually is hard. Scala & pure FP free up mental space for us to write complex code quickly.
    Cats Effect will simply be a set of tools to do that easily.
   */

  def testTimeIO(): Unit = {
    val test = measure(MyIO(() => Thread.sleep(1000)))
    println(test.unsafeRun())
  }

  // 3
  def putStrLn(line: String): MyIO[Unit] = MyIO(() => println(line))

  // 4
  val read: MyIO[String] = MyIO(() => StdIn.readLine())

  def testConsole(): Unit = {
    val program: MyIO[Unit] = for {
      line1 <- read
      line2 <- read
      _ <- putStrLn(line1 + line2)
    } yield ()

    program.unsafeRun()
  }

  def main(args: Array[String]): Unit = {
    testConsole()
  }
}
