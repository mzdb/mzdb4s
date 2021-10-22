import java.nio.charset.StandardCharsets
import scala.scalanative.unsafe._
import scala.scalanative.unsigned._
import scala.scalanative.windows._

@link("Kernel32")
@extern()
object WinAPI {

  /**
    * Retrieves the fully qualified path for the file that contains the specified module.
    * The module must have been loaded by the current process.
    *
    * If the function succeeds, the return value is the length of the string that is copied to the buffer,
    * in characters, not including the terminating null character.
    * If the buffer is too small to hold the module name,
    * the string is truncated to nSize characters including the terminating null character,
    * the function returns nSize, and the function sets the last error to ERROR_INSUFFICIENT_BUFFER.
    *
    * @param hModule A handle to the loaded module whose path is being requested.
    * @param lpFilename A pointer to a buffer that receives the fully qualified path of the module.
    * @param nSize The size of the lpFilename buffer, in TCHARs.
    * @return
    */
  def GetModuleFileNameA(
    hModule: Ptr[Byte],
    lpFilename: CWString,
    nSize: DWord
  ): DWord = extern
}

object WinApiHelper {

  // https://stackoverflow.com/questions/4517425/how-to-get-program-path/10734140#10734140
  def getProgramLocation(): String = {
    // FIXME: PATH_MAX should be implemented
    // val bufLen = scala.scalanative.posix.limits.PATH_MAX
    val bufLen: CSize = 1024.toUInt
    val strBuffer = stackalloc[WChar](bufLen + 1.toUInt)

    WinAPI.GetModuleFileNameA(null, strBuffer, bufLen.toUInt)
    val str = fromCWideString(strBuffer, StandardCharsets.UTF_8)

    str.substring(0, str.length() - 1)
  }

}
