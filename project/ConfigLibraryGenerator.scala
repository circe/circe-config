import sbt._
import Keys._
import sbt.io.Path.rebase

import scala.io.Source

object ConfigLibraryGenerator extends AutoPlugin {


  override def projectSettings: Seq[Setting[_]] =
    inConfig(Compile)(sourceGenerators += sourceGeneratorTask) ++ inConfig(Test)(sourceGenerators += sourceGeneratorTask) ++
      Seq(
        Test / unmanagedResourceDirectories ++= (LocalRootProject / Test / unmanagedResourceDirectories).value
      )


  object autoImport {

    val configLibrary =
      settingKey[ConfigLibrary]("Defines metadata of Typesafe Config API Compatible library to build against.")

    final case class ConfigLibrary(targetPackage: String,
                                   targetShortPackage: String,
                                   targetName: String,
                                   libraryPackage: String,
                                   libraryDocUrl: String)

  }

  def sourceGeneratorTask: Def.Initialize[Task[Seq[File]]] = Def.task {
    val streams = Keys.streams.value
    val inCache = Difference.inputs(streams.cacheStoreFactory.make("sconfig-in"), FileInfo.lastModified)
    val outCache = Difference.inputs(streams.cacheStoreFactory.make("sconfig-out"), FileInfo.exists)
    val library = autoImport.configLibrary.value
    val sources = (LocalRootProject / Keys.unmanagedSources).value.toSet
    val mappings = (sources pair rebase((LocalRootProject / sourceDirectories).value, sourceManaged.value)).toMap

    streams.log.debug(s"Sources used for ${library.targetName} sources generation:${System.lineSeparator}${sources.mkString(System.lineSeparator)}")

    inCache(sources) { inReport =>
      outCache { outReport =>
        if (outReport.checked.nonEmpty && inReport.modified.isEmpty && outReport.modified.isEmpty)
          outReport.checked
        else inReport.checked.map { f =>
          val out = {
            val source = Source.fromFile(f)
            try source.mkString finally source.close()
          }.replaceAll("io[.]circe[.]config", library.targetPackage)
            .replaceFirst("package config", "package " + library.targetShortPackage)
            .replaceFirst("package object config", "package object " + library.targetShortPackage)
            .replaceAll("com[.]typesafe[.]config", library.libraryPackage)
            .replaceAll("import config[.]", s"import ${library.targetShortPackage}.")
            .replaceAll("""private\[config] """, s"private[${library.targetShortPackage}] ")
            .replaceAll("circe-config", library.targetName)
            .replace("[[https://github.com/lightbend/config Typesafe config]]", library.libraryDocUrl)
          val target = mappings(f)

          IO.delete(target)
          IO.write(target, out.getBytes)
          target
        }
      }
    }.toSeq
  }
}
