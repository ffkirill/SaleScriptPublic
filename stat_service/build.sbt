name := "salescript-stat-service"

organization := "com.salescript"

version := "0.0.1"

scalaVersion := "2.12.1"
scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")


val circeVersion = "0.6.1"

libraryDependencies ++= Seq(
  "com.chuusai" %% "shapeless" % "2.3.2",
  "com.typesafe.akka" %% "akka-http" % "10.0.0",
  "de.heikoseeberger" %% "akka-http-circe" % "1.11.0",

  "com.typesafe.slick" %% "slick" % "3.2.0-M2",
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "com.github.tminglei" %% "slick-pg" % "0.15.0-M3",
  "org.flywaydb" % "flyway-core" % "3.2.1",

  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser"  % circeVersion,
  "io.circe" %% "circe-java8"   % circeVersion,

  "org.slf4j" % "slf4j-nop" % "1.6.4",

  "com.zaxxer" % "HikariCP" % "2.4.5"
)
enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)