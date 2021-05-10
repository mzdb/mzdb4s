package com.github.mzdb4s.io.mgf

import com.github.mzdb4s.io.reader.MgfReader
import utest._

object MgfTests extends TestSuite {

  val tests = Tests {
    'readBrukerMgfFile - readBrukerMgfFile()
  }

  def readBrukerMgfFile(): Unit = {
    val mgfFile = "io/shared/src/test/resources/single_spectrum_bruker.mgf"
    //val mgfFile = "E:/LCMS/hela_timstof_rennes/20210421_dHeLa_200ng_Slot1-5_1_2915.d/20210421_dHeLa_200ng_Slot1-5_1_2915_DA5.3.236_1.mgf"
    val reader = new MgfReader(mgfFile)

    //val spectrumById = new collection.mutable.LongMap[MgfSpectrum]

    var i = 0
    reader.foreachMgfSpectrum { spectrum =>
      i += 1

      //spectrumById.put(i,spectrum)

      val title = spectrum.mgfHeader.entries.find(_.field == MgfField.TITLE).get.toString
      /*val len = title.length

      val cmpId = new StringBuffer()
      var j = 0
      while (j < len) {
        val char = title(j)
        if (char == ',') j = len
        else if (char.isDigit) {
          cmpId.append(char)
        }
        j += 1
      }*/

      //println("cmpId=" + cmpId)

      /*if (i % 1000 == 0) {
        println(s"read $i spectra")
      }*/

      false
    }

    utest.assert(i == 264001)
  }

}
