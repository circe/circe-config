name := "circe-config"
organization := "io.circe"
description := "Yet another Typesafe Config decoder"
homepage := Some(url("https://github.com/circe/circe-config"))
licenses += "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")
apiURL := Some(url("https://circe.github.io/circe-config/api/"))

mimaPreviousArtifacts := Set("io.circe" %% "circe-config" % "0.3.0")

enablePlugins(GitPlugin)
versionWithGit
git.useGitDescribe := true
git.remoteRepo := "git@github.com:circe/circe-config.git"

enablePlugins(ReleasePlugin)
releaseCrossBuild := true
releasePublishArtifactsAction := PgpKeys.publishSigned.value
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
    pushChanges,
    releaseStepTask(ghpagesPushSite)
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

enablePlugins(GhpagesPlugin, SiteScaladocPlugin)
autoAPIMappings := true
ghpagesNoJekyll := true
siteSubdirName in SiteScaladoc := ""
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

publishMavenStyle := true
publishArtifact in Test := false
pomIncludeRepository := { _ => false }
publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}
credentials ++= (
  for {
    username <- Option(System.getenv().get("SONATYPE_USERNAME"))
    password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
  } yield Credentials(
    "Sonatype Nexus Repository Manager",
    "oss.sonatype.org",
    username,
    password
  )
).toSeq

scmInfo := Some(
  ScmInfo(
    url("https://github.com/circe/circe-config"),
    "scm:git:git@github.com:circe/circe-config.git"
  )
)

developers := List(
  Developer("jonas", "Jonas Fonseca", "jonas.fonseca@gmail.com", url("https://github.com/jonas"))
)
