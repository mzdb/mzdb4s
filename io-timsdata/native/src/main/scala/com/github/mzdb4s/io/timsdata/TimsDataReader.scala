package com.github.mzdb4s.io.timsdata

import scala.scalanative.runtime.{Intrinsics, fromRawPtr, toRawPtr}
import scala.scalanative.unsafe._
import scala.scalanative.unsigned._

import com.github.sqlite4s.c.util.CUtils

object TimsDataReader extends ITimsDataReader {

  //type ForEachMergedFrameCb = (Long, Int, Array[Double], Array[Float]) => Unit

  private var lastContextPtr: Ptr[Unit] = _
  //private var lastContext: ForEachMergedSpectrumCb = _

  def forEachMergedSpectrum(timsDataDir: String, config: Option[TimsDataReaderConfig], onEachMergedSpectrum: ForEachMergedSpectrumCb): Unit = {

    val errMsgPtr: Ptr[CString] = stackalloc[CString]()

    lastContextPtr = fromRawPtr[Unit](Intrinsics.castObjectToRawPtr(onEachMergedSpectrum))
    //lastContext = onEachMergedSpectrum

    Zone { implicit z =>
      val timsDataDirCStr = toCString(timsDataDir)

      val rustCb: TimsReaderLibrary.timsreader_for_each_merged_spectrum_v2_cb = (frameId: CLong, firstScan: CUnsignedInt, lastScan: CUnsignedInt, spectrumIdx: CUnsignedLong, mzValuesPtr: Ptr[CDouble], intensityValuesPtr: Ptr[CFloat], peaksCount: CSize, contextPtr: Ptr[Unit]) => {
        //println("peaksCount: " + peaksCount)

        try {
          //val onEachMergedSpectrumCb = TimsDataReader.lastContext
          val onEachMergedSpectrumCb = Intrinsics.castRawPtrToObject(toRawPtr(contextPtr)).asInstanceOf[ForEachMergedSpectrumCb]

          val mzValues = CUtils.doublePtr2DoubleArray(mzValuesPtr, peaksCount.toInt)
          val intensityValues = CUtils.floatPtr2FloatArray(intensityValuesPtr, peaksCount.toInt)
          //val mzValues = new NativeWrapper.DoublePtrWrapper(mzValuesPtr, peaksCount)
          //val intensityValues = new NativeWrapper.FloatPtrWrapper(intensityValuesPtr, peaksCount)

          onEachMergedSpectrumCb(frameId, firstScan.toInt, lastScan.toInt, spectrumIdx.toInt, mzValues, intensityValues)
        } catch {
          case t: Throwable => {
            System.err.println("An error occurred within the callback of timsreader_for_each_merged_frame: " + t.getMessage)
          }
        }

        ()
      }

      val rc = if (config.isEmpty) {
        throw new IllegalArgumentException("config must be defined")
        /*TimsReaderLibrary.timsreader_for_each_merged_spectrum(
          timsDataDirCStr,
          lastContextPtr,
          rustCb,
          errMsgPtr
        )*/
      } else {
        val cfg = config.get
        val configAsStructPtr = stackalloc[TimsReaderLibrary.timsreader_spectrum_merging_config]()
        val configAsStruct = !configAsStructPtr
        configAsStruct._1 = cfg.ms1Only
        configAsStruct._2 = cfg.mzTolPPM.toUInt
        configAsStruct._3 = cfg.minDataPointsCount.toULong
        configAsStruct._4 = cfg.nThreads.toULong
        configAsStruct._5 = cfg.nTimsdataDllThreads.toULong

        TimsReaderLibrary.timsreader_for_each_merged_spectrum_v2(
          timsDataDirCStr,
          configAsStructPtr,
          lastContextPtr,
          rustCb,
          errMsgPtr
        )
      }

      /*val rc = TimsReaderLibrary.timsreader_for_each_merged_spectrum(
        timsDataDirCStr,
        lastContextPtr,
        ,
        errMsgPtr
      )*/

      if (rc != 0) {
        // FIXME: free memory of errMsgPtr
        val errMsg = Option(_cstringPtrToString(errMsgPtr)).getOrElse("unknown error when calling timsreader_for_each_merged_frame")
        throw new Exception(errMsg)
        //System.err.println(errMsg)
        //println(rustErrorPtr.getInt(0), rustErrorPtr.getString(4))
      }

    }

  }

  private def _cstringPtrToString(cstr: Ptr[CString]): String = {
    if (cstr == null) return null

    fromCString(!cstr)
  }
}


object NativeWrapper {
  class FloatPtrWrapper(values: Ptr[Float], len: CSize) extends scala.collection.immutable.IndexedSeq[Float] {
    def length: Int = len.toInt

    /** Get the element at the specified index. */
    def apply(i: Int): Float = {
      if (i < 0 || i >= len.toInt)
        throw new ArrayIndexOutOfBoundsException(i)

      values.apply(i)
    }
  }

  class DoublePtrWrapper(values: Ptr[Double], len: CSize) extends scala.collection.immutable.IndexedSeq[Double] {
    def length: Int = len.toInt

    /** Get the element at the specified index. */
    def apply(i: Int): Double = {
      if (i < 0 || i >= len.toInt)
        throw new ArrayIndexOutOfBoundsException(i)

      values.apply(i)
    }
  }
}