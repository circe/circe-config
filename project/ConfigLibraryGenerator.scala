import sbt._
import sbt.Keys._
import sbt.io.Path.rebase

object ConfigLibraryGenerator extends AutoPlugin {

  override def projectSettings: Seq[Setting[_]] =
    inConfig(Compile)(
      Seq(
        sourceGenerators += sourceGeneratorTask,
        resourceGenerators += resourceGeneratorTask
      )) ++ inConfig(Test)(
      Seq(
        sourceGenerators += sourceGeneratorTask,
        resourceGenerators += resourceGeneratorTask
      ))

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
    val library = autoImport.configLibrary.value
    val inCache = Difference.inputs(streams.value.cacheStoreFactory.make(s"${library.targetShortPackage}-in"), FileInfo.lastModified)
    val outCache = Difference.inputs(streams.value.cacheStoreFactory.make(s"${library.targetShortPackage}-out"), FileInfo.exists)
    val mappings = ((LocalRootProject / Keys.unmanagedSources).value pair rebase((LocalRootProject / sourceDirectories).value, sourceManaged.value)).toMap

    streams.value.log.debug(s"Mappings used for ${library.targetName} sources generation:${System.lineSeparator}${mappings.mkString(System.lineSeparator)}")

    inCache(mappings.keySet) { inReport =>
      outCache { outReport =>
        if (outReport.checked.nonEmpty && inReport.modified.isEmpty && outReport.modified.isEmpty)
          outReport.checked
        else inReport.checked.map { f =>
          val out = IO.read(f).replaceAll("io[.]circe[.]config", library.targetPackage)
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

  def resourceGeneratorTask: Def.Initialize[Task[Seq[File]]] = Def.task {
    val library = autoImport.configLibrary.value
    val inCache = Difference.inputs(streams.value.cacheStoreFactory.make(s"${library.targetShortPackage}-in"), FileInfo.lastModified)
    val outCache = Difference.inputs(streams.value.cacheStoreFactory.make(s"${library.targetShortPackage}-out"), FileInfo.exists)
    val mappings = ((LocalRootProject / unmanagedResources).value pair rebase((LocalRootProject / unmanagedResourceDirectories).value, resourceManaged.value)).toMap

    streams.value.log.debug(s"Mappings used for ${library.targetName} resource generation:${System.lineSeparator}${mappings.mkString(System.lineSeparator)}")

    inCache(mappings.keySet) { inReport =>
      outCache { outReport =>
        if (outReport.checked.nonEmpty && inReport.modified.isEmpty && outReport.modified.isEmpty)
          outReport.checked
        else inReport.checked.map { r =>
          val target = mappings(r)
          IO.copy(Seq(r -> target), io.CopyOptions.apply().withOverwrite(true))
          target
        }
      }
    }.toSeq
  }

}
