package org.scalaide.buildtools

import java.io.File
import scala.io.Source
import java.io.FileWriter

object UpdateScalaIDEManifests {

  final val Usage = "app --root=<Scala IDE root folder>"

  final val PackagedManifestPath = "target/META-INF/MANIFEST.MF"
  final val BaseManifestPath = "META-INF/MANIFEST.MF"
  final val SavedManifestPath = "META-INF/MANIFEST.MF.original"

  final val RootOption = "--root=(.*)".r
  final val BundleVersion = "Bundle-Version: (.*)".r

  final val ScalaLibraryId = "org.scala-ide.scala.library"
  final val ScalaLibraryInManifest = ("(.*)" + ScalaLibraryId + "(,?.*)").r
  final val ScalaLibraryInManifestWithVersion = ("(.*)" + ScalaLibraryId + """;bundle-version="([^"]*)"(,?.*)""").r
  final val ScalaCompilerId = "org.scala-ide.scala.compiler"
  final val ScalaCompilerInManifest = ("(.*)" + ScalaCompilerId + "(,?.*)").r
  final val ScalaCompilerInManifestWithVersion = ("(.*)" + ScalaCompilerId + """;bundle-version="([^"]*)"(,?.*)""").r

  final val projectsToUpdate = List("org.scala-ide.sdt.core", "org.scala-ide.sdt.debug")

  def main(args: Array[String]) {
    // parse arguments

    val rootFolder = args.collectFirst {
      case RootOption(root) =>
        root
    }.getOrElse(System.getProperty("user.dir"))

    

    new UpdateScalaIDEManifests(rootFolder)()
  }

}

class UpdateScalaIDEManifests(root: String) {
  import UpdateScalaIDEManifests._

  val rootFolder = new File(root)

  def apply() {
    
    println("Build tools: Updating versions in Scala IDE manifests.")
    
    val scalaLibraryVersion = getPackagedBundleVersion(ScalaLibraryId)
    val scalaCompilerVersion = getPackagedBundleVersion(ScalaCompilerId)

    projectsToUpdate foreach {
      update(_, scalaLibraryVersion, scalaCompilerVersion)
    }
    
    println("Build tools: Updating versions in Scala IDE manifests - Done.")
  }

  /**
   * Returns the version contained in the generated manifest file
   */
  private def getPackagedBundleVersion(projectPath: String): String = {
    val projectFolder = new File(rootFolder, projectPath)
    val manifestFile = new File(projectFolder, PackagedManifestPath)

    // TODO: check if file exists

    val lines = Source.fromFile(manifestFile).getLines
    val version = lines.collectFirst {
      case BundleVersion(v) => v
    }

    // TODO: check if version found
    version.get
  }

  private def update(projectPath: String, scalaLibraryVersion: String, scalaCompilerVersion: String) {
    val projectFolder = new File(rootFolder, projectPath)
    val baseManifest = new File(projectFolder, BaseManifestPath)
    val savedManifest = new File(projectFolder, SavedManifestPath)

    // TODO: check if base file exists

    if (!savedManifest.exists()) {
      FileUtils.copyFile(baseManifest, savedManifest)
    }

    val lines = Source.fromFile(savedManifest).getLines
    val newLines = lines.map {
      case line @ ScalaLibraryInManifestWithVersion(_, version, _) =>
        warning("%s has already a version number defined: %s".format(ScalaLibraryId, version))
        line
      case ScalaLibraryInManifest(prefix, suffix) =>
        (prefix, suffix)
        "%s%s;bundle-version=%s%s".format(prefix, ScalaLibraryId, scalaLibraryVersion, suffix)
      case line @ ScalaCompilerInManifestWithVersion(_, version, _) =>
        warning("%s has already a version number defined: %s".format(ScalaCompilerId, version))
        line
      case ScalaCompilerInManifest(prefix, suffix) =>
        (prefix, suffix)
        "%s%s;bundle-version=%s%s".format(prefix, ScalaCompilerId, scalaCompilerVersion, suffix)
      case line =>
        line
    }

    val writer = new FileWriter(baseManifest)

    newLines foreach { s =>
      writer.write(s)
      writer.append('\n')
    }
    
    writer.flush()
    writer.close()

  }

  private def warning(message: String) {
    println("WARNING: %s".format(message))
  }

}