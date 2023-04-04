
import java.io.File
import mainargs._

object MzDbTools extends AbstractMzDbTools {

  // TODO: put in utils (also used in ThermoToMzDB.scala)
  import scala.scalanative.posix.limits
  import scala.scalanative.posix.unistd.readlink
  import scala.scalanative.unsafe._
  import scala.scalanative.unsigned._

  private def _readLink(link: CString)(implicit z: Zone): CString = {
    val buffer: CString = alloc[Byte](limits.PATH_MAX)
    readlink(link, buffer, limits.PATH_MAX - 1.toUInt) match {
      case -1 =>
        null
      case read =>
        // readlink doesn't null-terminate the result.
        buffer(read) = 0.toByte
        buffer
    }
  }

  // See: https://stackoverflow.com/questions/4517425/how-to-get-program-path/10734140#10734140
  protected def getAssemblyDir(): File = {
    if (scalanative.meta.LinktimeInfo.isWindows) {
      val dir = new File(WinApiHelper.getProgramLocation()).getParentFile
      dir
    }
    else {
      Zone { implicit z =>
        val path = _readLink(c"/proc/self/exe")
        require(path != null, "can't determine executable location")
        new File(fromCString(path)).getParentFile
      }
    }

  }

  @main
  def mzdb2mgf(
    @arg(short = 'i', name = "mzdb", doc = "Path to the mzDB input file")
    mzdb: String,
    @arg(short = 'o', name = "mgf", doc = "Path to the MGF output file")
    mgfOpt: Option[String]
  ): Unit = {
    this._mzdb2mgf(mzdb,mgfOpt.getOrElse(mzdb + ".mgf"))
  }

  @main
  def thermo2mzdb(
    @arg(short = 'i', name = "raw", doc = "Path to the raw input file")
    raw: String,
    @arg(short = 'o', name = "mzdb", doc = "Path to the mzDB output file")
    mzdbOpt: Option[String],
    @arg(short = 's', name = "split-faims", doc = "Split RAW file in multiple mzDB files (for each FAIMS CV value)")
    splitFaims: Flag
  ): Unit = {
    this._thermo2mzdb(raw,mzdbOpt.getOrElse(raw + ".mzDB"),splitFaims.value)
  }

  @main
  def tdf2mzdb(
    @arg(short = 'i', name = "tdf-dir", doc = "Path to the TDF input directory")
    tdfDir: String,
    @arg(short = 'o', name = "mzdb", doc = "Path to the mzDB output file")
    mzdbOpt: Option[String],
    @arg(name = "mzdist", doc = "Minimum distance between consecutive m/z values (PPM unit)")
    mzdistOpt: Option[Int]
  ): Unit = {
    this._tdf2mzdb(
      tdfDir,
      mzdbOpt.getOrElse(tdfDir + ".mzDB"),
      Some(com.github.mzdb4s.io.timsdata.TimsDataReaderConfig(
        ms1Only = true,
        mzTolPPM = mzdistOpt.getOrElse(10),
        minDataPointsCount = 1,
        nThreads = 4,
        nTimsdataDllThreads = 4,
      ))
    )
  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args)
}
