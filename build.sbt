name := "reactive"

version := "1.0"

scalaVersion := "2.12.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.11",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.11" % "test",
  "org.scalatest" %% "scalatest" % "3.0.0" % "test")