package de.thm.moie.compiler
import java.nio.file.Path
import de.thm.moie.utils.ProcessUtils._

class OMCompiler(compilerFlags:List[String], executableName:String="omc") extends ModelicaCompiler {
  override def compile(files: List[Path]): Seq[CompilerError] = {
    val pathes = files.map(_.toAbsolutePath.toString)
    val cmd = executableName :: (compilerFlags ::: pathes)
    val (status, stdout, error) = runCommand(cmd)
    List.fill(5)(CompilerError("test.mo", 2, 5, "Dummy error"))
  }
}
