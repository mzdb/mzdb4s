/*import scala.compat.Platform

object Foo {
  def main(args: Array[String]): Unit = {
    println(s"Hello from ${Platform.currentTime}")
    println(Bar.a)
  }
}*/

import java.io.{File, PrintWriter}

import scala.collection.mutable
import scala.compat.Platform
import com.github.mzdb4s.MzDbReader
import com.github.sqlite4s._

object Foo {

  implicit val sf: ISQLiteFactory = SQLiteFactory

  def main(args: Array[String]): Unit = {

    //println(Bar.a)

    println(s"Starting at ${Platform.currentTime}")

    Bar.create_dia_histogram(
      //new File("/mnt/d/Dev/wsl/ammonite/171121FFSWATH2000pg01.mzDB"),
      new File("/mnt/d/LCMS/quebec_dataset/UP_high_N4.raw.mzDB"),
      //new File("/mnt/d/LCMS/UPS1_50-5_small_runs/OVEMB150205_12.raw.mzDB"),
      new File("/mnt/d/Dev/wsl/scala-native/mzdb4s/dia_map_jvm.tsv"),
      0.1f
    )

    println(s"Ending at ${Platform.currentTime}")
  }

  /*def create_dia_histogram(mzDbFile: File, outputFile: File, binSize: Float) {

    val printWriter = new PrintWriter(outputFile)

    val mzDb = new MzDbReader(mzDbFile, true)
    val diaWindows = mzDb.getDIAIsolationWindows()
    println(s"Found ${diaWindows.length} DIA windows")

    for (diaWindow <- diaWindows) {
      val rsIter = mzDb.getLcMsnRunSliceIterator(diaWindow.getMinMz(),diaWindow.getMaxMz())

      val ticByMzIdx = new mutable.LongMap[Float]()
      while (rsIter.hasNext()) {

        val runSlice = rsIter.next()
        val rsData = runSlice.getData()
        val spectrumSlices = rsData.getSpectrumSliceList()

        for( spectrumSlice <- spectrumSlices; peak <- spectrumSlice.toPeaks() ) {
          val peakIdx = (peak.getMz() / binSize).toInt

          // Update TIC value
          val newTic = ticByMzIdx.get(peakIdx).map( _ + peak.getIntensity() ).getOrElse(0f)
          ticByMzIdx( peakIdx ) = newTic
        }
      }

      for (mzIdx <- ticByMzIdx.keys.toArray.sorted) {
        printWriter.println( diaWindow.getMinMz+ "\t" +diaWindow.getMaxMz + "\t" + mzIdx + "\t" + ticByMzIdx(mzIdx) )
      }
      println("Exported DIA window: " + diaWindow.getMinMz + "\t" +diaWindow.getMaxMz)
    }

    printWriter.close()

    mzDb.close()
  }*/
}

