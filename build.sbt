ThisBuild / organization := "com.udavpit.fp.cats"
ThisBuild / version      := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.0.1"
ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-language:postfixOps"
)

lazy val root = (project in file("."))
  .settings(
    name := "fp-cats-effect",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "3.2.2"
    )
  )
