package com.github.mzdb4s.io.timsdata

import com.github.mzdb4s.io.timsdata.TimsReaderLibrary.OnEachFrameCallback
import jnr.ffi.Pointer
import jnr.ffi.byref.PointerByReference


object TimsDataReader extends  ITimsDataReader {

  def forEachMergedSpectrum(timsDataDir: String, onEachMergedSpectrum: ForEachMergedSpectrumCb): Unit = {
    val rustLib = TimsReaderLibrary.getLibrary()
    assert(rustLib != null, "the library is not loaded")

    val nullCbContext: Pointer = null
    val errMsgPtr = new PointerByReference()//Memory.allocateDirect(runtime, 1024)

    val rc = rustLib.timsreader_for_each_merged_spectrum(
      timsDataDir,
      nullCbContext,
      new Object with OnEachFrameCallback {
        /*
        firstScan: CInt, lastScan: CInt,
            mzValuesPtr: Ptr[CDouble], intensityValuesPtr: Ptr[CFloat], peaksCount: CInt, context: Ptr[Unit]
         */
        def onResult(
          frameId: Long, firstScan: Int, lastScan: Int,
          mzValuesPtr: Pointer, intensityValuesPtr: Pointer, peaksCount: Int, context: Pointer
        ): Unit = {
          //println("peaksCount: " + peaksCount)

          val mzValues = new Array[Double](peaksCount)
          val intensityValues = new Array[Float](peaksCount)

          for (i <- 0 until peaksCount) {
            mzValues(i) = mzValuesPtr.getDouble(i * 8)
            intensityValues(i) = intensityValuesPtr.getFloat(i * 4)
          }

          onEachMergedSpectrum(frameId, firstScan, lastScan, mzValues, intensityValues)
        }

        /*def onResult(frameId: Long, peaksCount: Int, mzValues: Array[Double], intensityValues: Array[Float]): Unit = {
          onEachFrame(frameId, mzValues, intensityValues)
        }*/
      },
      errMsgPtr
    )

    if (rc != 0) {
      // FIXME: free memory of rustErrorPtr
      val errMsg = Option(_cstringPtrToString(errMsgPtr.getValue)).getOrElse("unknown error when calling timsreader_for_each_merged_frame")
      throw new Exception(errMsg)
      println("err msg: "+ errMsg)
      //println(rustErrorPtr.getInt(0), rustErrorPtr.getString(4))
    }
  }

  private def _cstringPtrToString(cstr: Pointer): String = {
    if (cstr == null) return null

    var curChar: Char = cstr.getByte(0).toChar

    var sb = new StringBuilder()
    var offset = 1
    while (curChar != '\u0000') {
      sb += curChar
      curChar = cstr.getByte(offset).toChar
      offset += 1
    }

    sb.toString()
  }
}
