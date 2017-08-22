name := "circe-config"
organization := "io.circe"
description := "Yet another Typesafe Config decoder"
homepage := Some(url("https://github.com/circe/circe-config"))
licenses += "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")
apiURL := Some(url("https://circe.github.io/circe-config/api/"))

crossScalaVersions := Seq("2.11.11", "2.12.3")
scalaVersion := crossScalaVersions.value.last

enablePlugins(GitPlugin)
versionWithGit
git.useGitDescribe := true

enablePlugins(BintrayPlugin)
bintrayRepository := "maven"
bintrayOrganization := Some("fonseca")
publishArtifact in Test := false
publishMavenStyle := true

enablePlugins(ReleasePlugin)
releaseCrossBuild := true
releaseTagName := (version in ThisBuild).value
releaseVersionFile := target.value / "unused-version.sbt"
releaseProcess := {
  import ReleaseTransformations._
  Seq[ReleaseStep](
    checkSnapshotDependencies,
    { st: State =>
      val v = (version in ThisBuild).value
      st.put(ReleaseKeys.versions, (v, v))
    },
    runTest,
    setReleaseVersion,
    tagRelease,
    publishArtifacts,
    pushChanges
  )
}

val Versions = new {
  val circe = "0.8.0"
  val config = "1.3.1"
  val discipline = "0.8"
  val scalaCheck = "0.13.5"
  val scalaTest = "3.0.4"
}

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % Versions.config,
  "io.circe" %% "circe-core" % Versions.circe,
  "io.circe" %% "circe-parser" % Versions.circe,
  "io.circe" %% "circe-generic" % Versions.circe % Test,
  "io.circe" %% "circe-testing" % Versions.circe % Test,
  "org.typelevel" %% "discipline" % Versions.discipline % Test,
  "org.scalacheck" %% "scalacheck" % Versions.scalaCheck % Test,
  "org.scalatest" %% "scalatest" % Versions.scalaTest % Test
)

autoAPIMappings := true
doctestTestFramework := DoctestTestFramework.ScalaTest
doctestMarkdownEnabled := true
doctestWithDependencies := false
scalacOptions in (Compile, doc) := Seq(
  "-groups",
  "-implicits",
  "-doc-source-url", scmInfo.value.get.browseUrl + "/tree/master€{FILE_PATH}.scala",
  "-sourcepath", baseDirectory.in(LocalRootProject).value.getAbsolutePath
)

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:postfixOps",
  "-unchecked",
  "-Xfuture",
  "-Xfatal-warnings",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-unused-import"
)

scalacOptions in (Compile, console) ~= { _.filterNot(Set("-Ywarn-unused-import")) }
scalacOptions in (Test, console) := (scalacOptions in (Compile, console)).value

scmInfo := Some(
  ScmInfo(
    url("https://github.com/circe/circe-config"),
    "scm:git:git@github.com:circe/circe-config.git"
  )
)

developers := List(
  Developer("jonas", "Jonas Fonseca", "jonas.fonseca@gmail.com", url("https://github.com/jonas"))
)
