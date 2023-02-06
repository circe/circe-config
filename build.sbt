ThisBuild / tlBaseVersion := "0.8"
ThisBuild / startYear := Some(2017)
ThisBuild / organizationName := "Jonas Fonseca"
ThisBuild / description := "Yet another Typesafe Config decoder"
ThisBuild / developers := List(
  Developer("jonas", "Jonas Fonseca", "jonas.fonseca@gmail.com", url("https://github.com/jonas"))
)
val scala212 = "2.12.15"
val scala213 = "2.13.8"
ThisBuild / scalaVersion := scala213
ThisBuild / crossScalaVersions := Seq(scala212, scala213)
ThisBuild / tlCiReleaseBranches := Seq("master")
ThisBuild / tlFatalWarningsInCi := false

val Versions = new {
  val catsEffect = "2.5.4"
  val circe = "0.14.1"
  val config = "1.4.2"
  val discipline = "1.4.0"
  val scalaCheck = "1.15.4"
  val scalaTest = "3.2.11"
  val scalaTestPlus = "3.2.11.0"
}

lazy val root = tlCrossRootProject.aggregate(config)

lazy val config = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("config"))
  .settings(
    name := "circe-config",
    libraryDependencies ++= Seq(
      "com.typesafe" % "config" % Versions.config,
      "io.circe" %% "circe-core" % Versions.circe,
      "io.circe" %% "circe-parser" % Versions.circe,
      "io.circe" %% "circe-generic" % Versions.circe % Test,
      "io.circe" %% "circe-testing" % Versions.circe % Test,
      "org.typelevel" %% "cats-effect" % Versions.catsEffect % Test,
      "org.typelevel" %% "discipline-core" % Versions.discipline % Test,
      "org.scalacheck" %% "scalacheck" % Versions.scalaCheck % Test,
      "org.scalatest" %% "scalatest" % Versions.scalaTest % Test,
      "org.scalatestplus" %% "scalacheck-1-15" % Versions.scalaTestPlus % Test
    ),
    doctestTestFramework := DoctestTestFramework.ScalaTest,
    doctestMarkdownEnabled := true
  )
