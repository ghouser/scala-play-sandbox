name := """scala-play-sandbox"""
organization := "Graham Houser"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.8"

libraryDependencies ++= Seq(
  guice,
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" %  "test",
  "org.postgresql"  %  "postgresql"                   % "42.3.2",
  "org.scalikejdbc" %% "scalikejdbc"                  % "3.5.0",
  "org.scalikejdbc" %% "scalikejdbc-config"           % "3.5.0",
  "org.scalikejdbc" %% "scalikejdbc-play-initializer" % "2.8.0-scalikejdbc-3.5",
)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "Graham Houser.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "Graham Houser.binders._"
