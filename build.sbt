name := """modicio-insights"""

organization := "de.modicio"
maintainer := "modicio github organisation"
version := "0.1"
scalaVersion := "2.13.7"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

resolvers += "Atlassian Releases" at "https://maven.atlassian.com/public/"

libraryDependencies ++= Seq(
  guice,
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % "test"
)

val circeVersion = "0.14.1"

libraryDependencies ++= Seq(
  "io.circe"  %% "circe-core"     % circeVersion,
  "io.circe"  %% "circe-generic"  % circeVersion,
  "io.circe"  %% "circe-parser"   % circeVersion
)