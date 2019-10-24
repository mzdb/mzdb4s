import java.io.File

import com.github.mzdb4s.util.collection._

import com.github.mzdb4s.MzDbReader
import com.github.mzdb4s.io.serialization.XmlSerializer
import com.github.mzdb4s.io.writer.MzDbWriter
import com.github.mzdb4s.msdata._
import com.github.mzdb4s.util.collection._
import com.github.sqlite4s.{ISQLiteFactory, SQLiteFactory}

object Sandbox {

  implicit val sf: ISQLiteFactory = SQLiteFactory

  def main(args: Array[String]): Unit = {
    println("Hello from IO JVM")

    convert()

    /*val mzDbFileForTest = new File(System.getProperty("sqlite4java.library.path")+"/data/OVEMB150205_12_new_from_mzDB.raw.mzDB")
    //val mzDbFileForTest = new File("/mnt/d/LCMS/UPS1_50-5_small_runs/OVEMB150205_12.raw.mzDB")
    val mzDbReader = new MzDbReader(mzDbFileForTest, true)

    implicit val mzDbCtx = mzDbReader.getMzDbContext()

    val specIter = mzDbReader.getSpectrumIterator()
    //mzDbReader.getSpectrum(1)

    var i = 0
    while (specIter.hasNext()) {
      i += 1

      val spectrum = specIter.next()
      println(spectrum.getHeader.getId)
      //println(spectrum.getHeader.getTitle)

      if (i != spectrum.getHeader.getId) println("missing spec id=" +i)
    }

    println(i)


    mzDbReader.close()*/
  }


  def convert() : Unit = {
    //println(System.getProperty("sqlite4java.library.path"))

    val mzDbFileForTest = new File("/mnt/d/Dev/wsl/scala-native/mzdb4s/data/OVEMB150205_12.raw.mzDB")
    //val mzDbFileForTest = new File(System.getProperty("sqlite4java.library.path")+"/data/OVEMB150205_12_new_from_mzDB.raw.mzDB")
    val mzDbReader = new MzDbReader(mzDbFileForTest, true)
    implicit val mzDbCtx = mzDbReader.getMzDbContext()

    println("spectra count in opened mzDB file: " +mzDbReader.getSpectraCount())

    val mzDbMetaData = mzDbReader.getMetaData()

    val writtenMzDbFile = new File(System.getProperty("sqlite4java.library.path")+"/data/OVEMB150205_12_new_from_mzDB.raw.mzDB") // _v2
    writtenMzDbFile.delete()

    val mzDbWriter = new MzDbWriter(writtenMzDbFile, mzDbMetaData, DefaultBBSizes(), isDIA = false)
    mzDbWriter.open()

    val dataEncBySpecId = mzDbReader.getDataEncodingBySpectrumId()
    val spectrumXmlMetaDataById = mzDbReader.getSpectraXmlMetaData().mapByLong(_.spectrumId)
    val specIter = mzDbReader.getSpectrumIterator()

    var cycle = 0
    while (specIter.hasNext()) {
      val spectrum = specIter.next()

      val sh = spectrum.getHeader

      /*val metaDataAsText = SpectrumMetaDataAsText(
        title = sh.title,
        paramTree = sh.getParamTree().map(XmlSerializer.serializeParamTree).getOrElse(""),
        scanList = "", //sh.getOrLoadScanList(),
        precursorList = None, //sh.getOrLoadPrecursor(),
        productList = None,
        activationType = sh.activationType.map(_.toString)
      )*/

      mzDbWriter.insertSpectrum(spectrum, spectrumXmlMetaDataById(sh.id), dataEncBySpecId(sh.id))

      cycle += 1
    }

    mzDbWriter.close()
    mzDbReader.close()
  }

}