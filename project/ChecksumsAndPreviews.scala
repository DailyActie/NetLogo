import sbt._
import Def.spaceDelimited
import Keys._
import Extensions.extensions
import scala.util.Try

object ChecksumsAndPreviews {

  // sbt already has a command called "checksums", so we prepend "all-" - ST 6/23/12

  lazy val checksum = InputKey[Try[Unit]]("checksum", "update one model checksum")
  lazy val allChecksums = InputKey[Try[Unit]]("all-checksums", "update all model checksums")
  lazy val preview = InputKey[Try[Unit]]("preview", "update one model preview image")
  lazy val allPreviews = InputKey[Try[Unit]]("all-previews", "update all model preview images")
  lazy val checksumExport = InputKey[Try[Unit]]("checksumExport", "run export on preview commands for one model")
  lazy val allChecksumsExport = InputKey[Try[Unit]]("allChecksumsExport", "run export on preview commands for all models")


  val settings = Seq(
    Def.setting(checksum, makeTask("--checksum")),
    Def.setting(allChecksums, makeTask("--checksums")),
    Def.setting(preview, makeTask("--preview")),
    Def.setting(allPreviews, makeTask("--previews")),
    Def.setting(checksumExport, makeTask("--checksum-export")),
    Def.setting(allChecksumsExport, makeTask("--checksum-exports"))
  )

  private def makeTask(flag: String): Def.Initialize[InputTask[Try[Unit]]] =
    Def.inputTask {
      val args = spaceDelimited("").parsed
      val runner = new ForkRun(ForkOptions()
        .withWorkingDirectory(Some(baseDirectory.value.getParentFile))
        .withRunJVMOptions(Vector("-Dorg.nlogo.is3d=" + System.getProperty("org.nlogo.is3d"))))
      runner.run("org.nlogo.headless.ChecksumsAndPreviews",
        (fullClasspath in Compile).value.map(_.data), flag +: args, streams.value.log)
    }.dependsOn(compile in Compile, extensions)

}
