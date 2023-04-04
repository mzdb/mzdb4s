package com.github.mzdb4s.io.timsdata

import jnr.ffi._
import jnr.ffi.annotations.{Delegate, Out}
import jnr.ffi.byref.PointerByReference
import jnr.ffi.types.size_t

trait OnEachFrameCallback {
  @Delegate def onResult(
    frameId: Long,
    @size_t firstScan: Int,
    @size_t lastScan: Int,
    @Out mzValues: Pointer,
    @Out intensityValues: Pointer,
    @size_t peaksCount: Int,
    context: Pointer
  ): Unit
  //@Delegate def onResult(frameId: Long, @size_t peaksCount: Int, @Out mzValues: Array[Double], @Out intensityValues: Array[Float]): Unit
}

/*final class TimsReaderError(val runtime: Runtime) extends Struct(runtime) {
  final val code = new Signed32()
  final val message = new String()
}*/

class TimsDataReaderConfigAsStruct(val runtime: Runtime) extends Struct(runtime) {
  val ms1Only = new Boolean()
  val mzTolPPM = new Signed32()
  val minDataPointsCount = new Unsigned64()
  val nThreads = new Unsigned64()
  val nTimsdataDllThreads = new Unsigned64()

  def this(config: TimsDataReaderConfig, runtime: Runtime) {
    this(runtime)

    ms1Only.set(config.ms1Only)
    mzTolPPM.set(config.mzTolPPM)
    minDataPointsCount.set(config.minDataPointsCount)
    nThreads.set(config.nThreads)
    nTimsdataDllThreads.set(config.nTimsdataDllThreads)
  }
}

trait TimsReaderLibrary {
  def timsreader_init_logger(): Unit
  def timsreader_print_text(): Unit
  def timsreader_for_each_merged_spectrum(
    timsDataDir: String,
    cbContext: Pointer,
    cb: OnEachFrameCallback,
    error: PointerByReference
  ): Int
  def timsreader_for_each_merged_spectrum_v2(
    timsDataDir: String,
    config: TimsDataReaderConfigAsStruct,
    cbContext: Pointer,
    cb: OnEachFrameCallback,
    error: PointerByReference
  ): Int
}

/*object TDFLibrary {
  def getInstance(libFullPath: String): TDFLibrary = {
    val instance = Native.load(libFullPath, classOf[TDFLibrary]).asInstanceOf[TDFLibrary]
    instance
  }

  trait MsMsCallback extends Callback {
    def invoke(precursor_id: Long, num_peaks: Int, pMz: Pointer, pIntensites: Pointer): Unit
  }

}

trait TDFLibrary extends Library {
  def tims_open(analysis_dir: String, use_recalib: Long): Long

  def tims_close(handle: Long): Long

  def tims_get_last_error_string(error: Array[Byte], len: Long): Long

  def tims_read_scans_v2(handle: Long, frameId: Long, scanBegin: Long, scanEnd: Long, scanBuffer: Array[Byte], len: Long): Long

  def tims_index_to_mz(handle: Long, frameId: Long, index: Array[Double], mz: Array[Double], len: Long): Long

  def tims_scannum_to_oneoverk0(handle: Long, frameId: Long, scannum: Array[Double], oneOverK0: Array[Double], len: Long): Long

  def tims_read_pasef_msms(handle: Long, precursors: Array[Long], num_precursors: Long, my_callback: TDFLibrary.MsMsCallback): Long

  def tims_read_pasef_msms_for_frame(handle: Long, frameId: Long, my_callback: TDFLibrary.MsMsCallback): Long
}
*/