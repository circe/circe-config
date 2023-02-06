ThisBuild / tlBaseVersion := "0.8"
ThisBuild / startYear := Some(2017)
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
  val catsEffect = "2.5.4"
  val circe = "0.14.1"
  val config = "1.4.2"
  val discipline = "1.4.0"
  val scalaCheck = "1.15.4"
  val scalaTest = "3.2.11"
  val scalaTestPlus = "3.2.11.0"
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
      "org.typelevel" %% "cats-effect" % Versions.catsEffect % Test,
      "org.typelevel" %% "discipline-core" % Versions.discipline % Test,
      "org.scalacheck" %% "scalacheck" % Versions.scalaCheck % Test,
      "org.scalatest" %% "scalatest" % Versions.scalaTest % Test,
      "org.scalatestplus" %% "scalacheck-1-15" % Versions.scalaTestPlus % Test
    ),
    doctestTestFramework := DoctestTestFramework.ScalaTest,
    doctestMarkdownEnabled := true,
    headerLicense := Some(HeaderLicense.ALv2(s"${startYear.value.get}", "Jonas Fonseca")),
    tlVersionIntroduced := Map(
      "2.12" -> "0.3.0",
      "2.13" -> "0.7.0"
    )
  )
