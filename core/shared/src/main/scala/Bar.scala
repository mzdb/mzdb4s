import java.io.{File, PrintWriter}

import com.github.mzdb4s.MzDbReader
import com.github.sqlite4s._

import scala.collection.mutable

object Bar {
  val a = 42

  def create_dia_histogram(mzDbFile: File, outputFile: File, binSize: Float)(implicit sf: ISQLiteFactory) {

    /*
    val db = new com.almworks.sqlite4java.SQLiteConnection(new File("/mnt/d/LCMS/UPS1_50-5_small_runs/OVEMB150205_12.raw.mzDB"))
    db.open(false)

    val st = db.prepare("SELECT * FROM bounding_box")
    try {
      while (st.step()) {
        println(st.columnInt(0))
      }
    } finally {
      st.dispose()
    }
    db.dispose()*/

    val mzDbFileForTest =  new File("/mnt/d/LCMS/quebec_dataset/UP_high_N4.raw.mzDB")
    //val mzDbFileForTest = new File("/mnt/d/LCMS/UPS1_50-5_small_runs/OVEMB150205_12.raw.mzDB")

    val mzDb = new MzDbReader(mzDbFileForTest, true)

    println("spectra count: "+mzDb.getSpectraCount())

    //val specIter = mzDb.getSpectrumIterator()
    implicit val mzDbCtx = mzDb.getMzDbContext()

    /*def getBoundingBoxIterator(msLevel: Int): com.github.mzdb4s.io.reader.iterator.BoundingBoxIterator = {
      /*val stmt = mzDb.getConnection().prepare("SELECT bounding_box.* FROM bounding_box, spectrum WHERE spectrum.id = bounding_box.first_spectrum_id AND spectrum.ms_level= ?", false)
      stmt.bind(1, msLevel)*/
      val stmt = mzDb.getConnection().prepare("SELECT * FROM bounding_box")
      new com.github.mzdb4s.io.reader.iterator.BoundingBoxIterator(
        mzDb.getSpectrumHeaderReader(), mzDb.getDataEncodingReader(), stmt
      )
    }

    println("test3")

    val bbIter2 = getBoundingBoxIterator(2)
    var i = 0
    while (bbIter2.hasNext) {
      //print(".")
      bbIter2.next()
      //specIter2.releaseSpectrumData(spectrum.header.id)

      i += 1
    }*/

    /*val specIter2 = new com.github.mzdb4s.io.reader.iterator2.SpectrumIterator(mzDb)

    try {
      var i = 0
      while (specIter2.hasNext()) {
        //print(".")
        val spectrum = specIter2.next()
        //specIter2.releaseSpectrumData(spectrum.header.id)

        i += 1
      }
      println(i)
    } catch {
      case t: Throwable => {
        println("caught error: " + t.getMessage)
        Thread.sleep(2000)
        throw t
      }
    }*/

    /*val runSliceIter2 = new com.github.mzdb4s.io.reader.iterator.LcMsRunSliceIterator(mzDb)

    try {
      var i = 0
      while (runSliceIter2.hasNext()) {
        val runSlice = runSliceIter2.next()

        i += 1
      }
    } catch {
      case t: Throwable => {
        println("caught error: " + t.getMessage)
        Thread.sleep(2000)
        throw t
      }
    }*/


    val printWriter = new PrintWriter(outputFile)

    val diaWindows = mzDb.getDIAIsolationWindows()
    println(s"Found ${diaWindows.length} DIA windows")

    val ticByMzIdx = new mutable.LongMap[Float]()

    for (diaWindow <- diaWindows) {
      val rsIter = mzDb.getLcMsnRunSliceIterator(diaWindow.getMinMz(),diaWindow.getMaxMz())

      ticByMzIdx.clear()

      while (rsIter.hasNext()) {

        val runSlice = rsIter.next()
        val rsData = runSlice.getData()
        val spectrumSlices = rsData.getSpectrumSliceList()

        for( spectrumSlice <- spectrumSlices ) {
          val sd = spectrumSlice.data
          val peaksCount = sd.peaksCount

          var i = 0
          while (i < peaksCount) {
            val mz = sd.mzList(i)
            val intensity = sd.intensityList(i)
            val peakIdx = (mz / binSize).toInt

            // Update TIC value
            val newTic = ticByMzIdx.get(peakIdx).map( _ + intensity ).getOrElse(0f)
            ticByMzIdx( peakIdx ) = newTic

            i += 1
          }
        }
      }

      for (mzIdx <- ticByMzIdx.keys.toArray.sorted) {
        printWriter.println( diaWindow.getMinMz+ "\t" +diaWindow.getMaxMz + "\t" + mzIdx + "\t" + ticByMzIdx(mzIdx) )
      }
      println("Exported DIA window: " + diaWindow.getMinMz + "\t" +diaWindow.getMaxMz)
    }

    printWriter.close()

    mzDb.close()

  }
}
