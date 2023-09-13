ThisBuild / tlBaseVersion := "0.10"
ThisBuild / startYear := Some(2017)
ThisBuild / description := "Yet another Typesafe Config decoder"
ThisBuild / developers := List(
  Developer("jonas", "Jonas Fonseca", "jonas.fonseca@gmail.com", url("https://github.com/jonas"))
)
val scala212 = "2.12.18"
val scala213 = "2.13.11"
val scala3 = "3.3.1"
ThisBuild / scalaVersion := scala213
ThisBuild / crossScalaVersions := Seq(scala212, scala213, scala3)
ThisBuild / circeRootOfCodeCoverage := Some("root")
ThisBuild / tlCiReleaseBranches := Seq("master")
ThisBuild / tlFatalWarnings := false
ThisBuild / tlMimaPreviousVersions ++= Set(
  // manually added because tags are not v-prefixed
  "0.3.0",
  "0.4.0",
  "0.4.1",
  "0.5.0",
  "0.6.0",
  "0.6.1",
  "0.7.0",
  "0.7.0-M1",
  "0.8.0"
)

val Versions = new {
  val circe = "0.14.6"
  val config = "1.4.2"
  val munit = "0.7.29"
  val disciplineMunit = "1.0.9"
  val munitCatsEffect = "1.0.7"
}

lazy val root = tlCrossRootProject.aggregate(config)

lazy val config = project
  .in(file("config"))
  .settings(
    name := "circe-config",
    libraryDependencies ++= Seq(
      "com.typesafe" % "config" % Versions.config,
      "io.circe" %% "circe-core" % Versions.circe,
      "io.circe" %% "circe-parser" % Versions.circe,
      "io.circe" %% "circe-generic" % Versions.circe % Test,
      "io.circe" %% "circe-testing" % Versions.circe % Test,
      "org.scalameta" %% "munit" % Versions.munit % Test,
      "org.typelevel" %% "discipline-munit" % Versions.disciplineMunit % Test,
      "org.typelevel" %% "munit-cats-effect-3" % Versions.munitCatsEffect % Test
    ),
    doctestTestFramework := DoctestTestFramework.Munit,
    doctestMarkdownEnabled := true,
    tlVersionIntroduced := Map(
      "2.12" -> "0.3.0",
      "2.13" -> "0.7.0",
      "3" -> "0.10.0"
    )
  )
