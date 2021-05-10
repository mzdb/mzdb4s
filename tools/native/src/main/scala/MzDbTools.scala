import mainargs._

object MzDbTools extends AbstractMzDbTools {

  @main
  def mzdb2mgf(
    @arg(short = 'i', doc = "Path to the mzDB input file")
    mzdb: String,
    @arg(short = 'o', name = "mgf", doc = "Path to the MGF output file")
    mgfOpt: Option[String]
  ): Unit = {
    this._mzdb2mgf(mzdb,mgfOpt.getOrElse(mzdb + ".mgf"))
  }

  @main
  def thermo2mzdb(
    @arg(short = 'i', doc = "Path to the raw input file")
    raw: String,
    @arg(short = 'o', name = "mzdb", doc = "Path to the mzDB output file")
    mzdbOpt: Option[String]
  ): Unit = {
    this._thermo2mzdb(raw,mzdbOpt.getOrElse(raw + ".mzDB"))
  }

  @main
  def tdf2mzdb(
    @arg(short = 'i', name = "tdf-dir", doc = "Path to the TDF input directory")
    tdfDir: String,
    @arg(short = 'o', doc = "Path to the mzDB output file")
    mzdbOpt: Option[String]
  ): Unit = {
    this._tdf2mzdb(tdfDir,mzdbOpt.getOrElse(tdfDir + ".mzDB"))

    //System.err.println("Not yet implemented!")
    //System.exit(-1)
  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args)
}
