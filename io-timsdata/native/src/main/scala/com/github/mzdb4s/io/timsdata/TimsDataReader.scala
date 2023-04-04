package com.github.mzdb4s.io.timsdata

import scala.scalanative.runtime.{Intrinsics, fromRawPtr, toRawPtr}
import scala.scalanative.unsafe._

import com.github.sqlite4s.c.util.CUtils

object TimsDataReader extends ITimsDataReader {

  //type ForEachMergedFrameCb = (Long, Int, Array[Double], Array[Float]) => Unit

  def forEachMergedSpectrum(timsDataDir: String, config: Option[TimsDataReaderConfig], onEachMergedSpectrum: ForEachMergedSpectrumCb): Unit = {

    val errMsgPtr: Ptr[CString] = stackalloc[CString]()

    val contextPtr = fromRawPtr[Unit](Intrinsics.castObjectToRawPtr(onEachMergedSpectrum))

    Zone { implicit z =>
      val timsDataDirCStr = toCString(timsDataDir)

      val rc = TimsReaderLibrary.timsreader_for_each_merged_spectrum(
        timsDataDirCStr,
        contextPtr,
        (frameId: CLong, firstScan: CInt, lastScan: CInt, mzValuesPtr: Ptr[CDouble], intensityValuesPtr: Ptr[CFloat], peaksCount: CInt, context: Ptr[Unit]) => {
          //println("peaksCount: " + peaksCount)

          val onEachMergedSpectrumCb = Intrinsics.castRawPtrToObject(toRawPtr(context)).asInstanceOf[ForEachMergedSpectrumCb]

          val mzValues = CUtils.doublePtr2DoubleArray(mzValuesPtr, peaksCount)
          val intensityValues = CUtils.floatPtr2FloatArray(intensityValuesPtr, peaksCount)

          onEachMergedSpectrumCb(frameId, firstScan, lastScan, mzValues, intensityValues)
          ()
        },
        errMsgPtr
      )

      if (rc != 0) {
        // FIXME: free memory of errMsgPtr
        val errMsg = Option(_cstringPtrToString(errMsgPtr)).getOrElse("unknown error when calling timsreader_for_each_merged_frame")
        throw new Exception(errMsg)
        println("err msg: "+ errMsg)
        //println(rustErrorPtr.getInt(0), rustErrorPtr.getString(4))
      }

    }

  }

  private def _cstringPtrToString(cstr: Ptr[CString]): String = {
    if (cstr == null) return null

    fromCString(!cstr)
  }
}
