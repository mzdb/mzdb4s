package com.github.mzdb4s.io.timsdata

import jnr.ffi._
import jnr.ffi.annotations.{Delegate, Out}
import jnr.ffi.byref.PointerByReference
import jnr.ffi.types.size_t

object TimsReaderLibrary {

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

  trait TimsReaderInterface {
    def timsreader_init_logger(): Unit
    def timsreader_print_text(): Unit
    def timsreader_for_each_merged_spectrum(
      timsDataDir: String,
      cbContext: Pointer,
      cb: OnEachFrameCallback,
      error: PointerByReference
    ): Int
  }

  //println(new File("../target/debug/libproteomics_fasta.dll").isFile)
  private var rustLib: TimsReaderInterface = _
  private var runtime: jnr.ffi.Runtime = _

  def loadLibrary(libraryName: String): Unit = {
    rustLib = LibraryLoader.create(classOf[TimsReaderInterface]).load(libraryName)
    runtime = jnr.ffi.Runtime.getRuntime(rustLib)
  }

  def getLibrary(): TimsReaderInterface = rustLib

  def printText(): Unit = {
    assert(rustLib != null, "the library is not loaded")
    rustLib.timsreader_print_text()
  }

  def initLogger(): Unit = {
    assert(rustLib != null, "the library is not loaded")
    rustLib.timsreader_init_logger()
  }



  /*val err: Pointer = null
  err.getInt(0)
  err.getString(4)*/

  //err.
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