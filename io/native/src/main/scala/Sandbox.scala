
import java.io.File

import com.github.mzdb4s.db.model._
import com.github.mzdb4s.db.model.params.ParamTree
import com.github.mzdb4s.db.model.params.param.UserParam
import com.github.mzdb4s.io.mzml.MzMLParser
import com.github.mzdb4s.io.writer.MzDbWriter
import com.github.mzdb4s.io.writer.NativeMzDbWriter
import com.github.mzdb4s.msdata._
import com.github.mzdb4s.util.collection._
import com.github.sqlite4s.{ISQLiteFactory, SQLiteFactory}

object Sandbox {

  implicit val sf: ISQLiteFactory = SQLiteFactory

  def main(args: Array[String]): Unit = {
    println("Hello from IO Native")

    val mzMLFileNameOpt = args.headOption
    require(mzMLFileNameOpt.isDefined, "An input file name must be provided")

    val mzMLFileName = mzMLFileNameOpt.get
    //val mzMLFileName = "/mnt/d/Dev/wsl/scala-native/mzdb4s/data/OVEMB150205_12.mzML"
    //val mzMLFileName = "/mnt/d/Dev/wsl/scala-native/mzdb4s/data/test.mzML"

    val writtenMzDbFile = new File(mzMLFileName.split('.').head + ".mzML.mzDB")
    writtenMzDbFile.delete()

    println("Opening mzML file located at: " + mzMLFileName)
    val mzMLParser = new MzMLParser(mzMLFileName)
    val mzMLMetaData = mzMLParser.getMetaData()

    val mzDbMetaData = mzMLMetaData.toMzDbMetaData(
      mzDbHeader = new MzDbHeader(
        version = "0.7",
        creationTimestamp = new java.util.Date().getTime.toInt / 1000,
        paramTree = ParamTree(
          userParams = List(
            UserParam(name = "origin_file_format", value = "Thermo RAW format", `type` = "xsd:string")
          )
        )
      ),
      dataEncodings = Seq.empty[DataEncoding] // should be determined dynamically
    )

    println("Writing mzDB file located at: " + writtenMzDbFile.getAbsolutePath)

    val mzDbWriter = new MzDbWriter(writtenMzDbFile, mzDbMetaData, DefaultBBSizes(), isDIA = false)
    mzDbWriter.open()

    mzMLParser.forEachSpectrum { (spectrum: Spectrum, metaDataAsText: SpectrumXmlMetaData, dataEncoding: DataEncoding) =>
      //val i = spectrum.header.id

      mzDbWriter.insertSpectrum(spectrum, metaDataAsText, dataEncoding)

      //if (i == 20) false else true

      val cycle = spectrum.getHeader.getCycle
      //if (cycle % 100 == 0) System.gc()

      true
    }

    mzDbWriter.close()
    mzMLParser.close()

  }

}