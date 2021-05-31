name := "circe-config"
description := "Yet another Typesafe Config decoder"
apiURL := Some(url("https://circe.github.io/circe-config/"))

mimaPreviousArtifacts := {
  val versions = Set("0.3.0", "0.4.0", "0.4.1", "0.5.0", "0.6.0", "0.7.0", "0.8.0")
  val versionFilter: String => Boolean = CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 12)) => _ => true
    case Some((2, 13)) => _ => false
    case _             => _ => false
  }

  versions.filter(versionFilter).map("io.circe" %% "circe-config" % _)
}

unmanagedSourceDirectories in Compile += {
  val sourceDir = (sourceDirectory in Compile).value
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, n)) if n <= 12 => sourceDir / "scala-2.13-"
    case _                       => sourceDir / "scala-2.13+"
  }
}

enablePlugins(GitPlugin)
versionWithGit
git.useGitDescribe := true
git.remoteRepo := "git@github.com:circe/circe-config.git"

enablePlugins(ReleasePlugin)
releaseCrossBuild := true
releaseVcsSign := true
releasePublishArtifactsAction := PgpKeys.publishSigned.value
releaseTagName := (version in ThisBuild).value
releaseVersionFile := target.value / "unused-version.sbt"
releaseProcess := {
  import ReleaseTransformations._
  Seq[ReleaseStep](
    checkSnapshotDependencies, { st: State =>
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
  val catsEffect = "2.5.1"
  val circe = "0.14.1"
  val config = "1.4.1"
  val discipline = "1.1.5"
  val scalaCheck = "1.15.4"
  val scalaTest = "3.2.9"
  val scalaTestPlus = "3.2.2.0"
  val sconfig = "1.3.6"
}

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % Versions.config,
  "io.circe" %%% "circe-core" % Versions.circe,
  "io.circe" %%% "circe-parser" % Versions.circe,
  "io.circe" %%% "circe-generic" % Versions.circe % Test,
  "io.circe" %%% "circe-testing" % Versions.circe % Test,
  "org.typelevel" %%% "cats-effect" % Versions.catsEffect % Test,
  "org.typelevel" %%% "discipline-core" % Versions.discipline % Test,
  "org.scalacheck" %%% "scalacheck" % Versions.scalaCheck % Test,
  "org.scalatest" %%% "scalatest" % Versions.scalaTest % Test,
  "org.scalatestplus" %%% "scalacheck-1-14" % Versions.scalaTestPlus % Test
)

enablePlugins(GhpagesPlugin, SiteScaladocPlugin)
ghpagesNoJekyll := true
siteSubdirName in SiteScaladoc := ""
doctestTestFramework := DoctestTestFramework.ScalaTest
doctestMarkdownEnabled := true
scalacOptions in(Compile, doc) := Seq(
  "-groups",
  "-implicits",
  "-doc-source-url",
  scmInfo.value.get.browseUrl + "/tree/master€{FILE_PATH}.scala",
  "-sourcepath",
  baseDirectory.in(LocalRootProject).value.getAbsolutePath
)

scalacOptions in(Compile, console) --= Seq("-Ywarn-unused-import", "-Ywarn-unused:imports")
scalacOptions in(Test, console) := (scalacOptions in(Compile, console)).value

publishMavenStyle := true
pomIncludeRepository := { _ =>
  false
}

inThisBuild(
  Seq(
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-feature",
      "-language:postfixOps",
      "-language:higherKinds",
      "-unchecked",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-unused:imports"),
    scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 12)) =>
          Seq(
            "-Xfatal-warnings",
            "-Yno-adapted-args",
            "-Xfuture"
          )
        case _ =>
          Nil
      }
    },
    publishArtifact in Test := false,
    publishTo := Some {
      if (isSnapshot.value) Opts.resolver.sonatypeSnapshots else Opts.resolver.sonatypeStaging
    },
    organization := "io.circe",
    homepage := Some(url("https://github.com/circe/circe-config")),
    licenses += "Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html"),
    scmInfo := Some(ScmInfo(url("https://github.com/circe/circe-config"), "scm:git:git@github.com:circe/circe-config.git")),
    autoAPIMappings := true,
    developers := List(
      Developer("jonas", "Jonas Fonseca", "jonas.fonseca@gmail.com", url("https://github.com/jonas"))
    )
  )
)

lazy val `circe-config` =
  (project in file("."))

lazy val `circe-sconfig` =
  crossProject(JVMPlatform, JSPlatform)
    .withoutSuffixFor(JVMPlatform)
    .in(file(".sconfig"))
    .enablePlugins(ConfigLibraryGenerator)
    .settings(
      description := "Yet another Typesafe Config AST decoder",
      configLibrary := ConfigLibrary(
        targetPackage = "io.circe.sconfig",
        targetShortPackage = "sconfig",
        targetName = "circe-sconfig",
        libraryPackage = "org.ekrich.config",
        libraryDocUrl = "[[https://github.com/ekrich/sconfig SConfig]]"),
      mimaPreviousArtifacts := {
        val unavailable = Set("0.3.0", "0.4.0", "0.4.1", "0.5.0", "0.6.0", "0.7.0", "0.8.0")
        (LocalRootProject / mimaPreviousArtifacts).value.collect {
          case prev if !unavailable.contains(prev.revision) => prev.organization %%% "circe-sconfig" % prev.revision
        }
      },
      libraryDependencies ++= (LocalRootProject / libraryDependencies).value,
      libraryDependencies += "org.ekrich" %%% "sconfig" % Versions.sconfig,
      libraryDependencies -= "com.typesafe" % "config" % Versions.config
    )
    .jvmSettings(
      doctestTestFramework := (LocalRootProject / doctestTestFramework).value
    )

aggregateProjects(`circe-sconfig`.jvm, `circe-sconfig`.js)
