package de.thm.moie.compiler
import java.nio.file.{Files, Path}

import de.thm.moie.utils.ProcessExitCodes
import de.thm.moie.utils.ProcessUtils._

import scala.sys.process.Process

class OMCompiler(compilerFlags:List[String], executableName:String, outputDir:String) extends ModelicaCompiler {
  override def compile(files: List[Path]): Seq[CompilerError] = {
    val pathes = files.map(_.toAbsolutePath.toString)
    files.headOption match {
      case Some(path) =>
        val outputDir = createOutputDir(path.getParent)
        val compilerExec = executableName :: (compilerFlags ::: pathes)
        val cmd = Process(compilerExec, outputDir.toFile)
        val (status, stdout, error) = runCommand(cmd)
        if(status != ProcessExitCodes.SUCCESSFULL) {
          val _ = parseErrorMsg(stdout)
          List.fill(5)(CompilerError("test.mo", 2, 5, "Dummy error"))
        }
        else Seq[CompilerError]()
      case None => Seq[CompilerError]()
    }
  }

  private def parseErrorMsg(msg:String): Seq[CompilerError] = {
    ???
  }

  private def createOutputDir(path:Path): Path = {
    val outputPath = path.resolve(outputDir)
    if(!Files.exists(outputPath))
      Files.createDirectory(outputPath)
    outputPath
  }
}
