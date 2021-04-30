package com.github.mzdb4s.io.timsdata

import scala.scalanative.unsafe._
//import scala.scalanative.unsigned._

@link("timsdata")
@link("timsdatareader")
@extern
object TimsReaderLibrary {

  def timsreader_init_logger(): Unit = extern

  def timsreader_print_text(): Unit = extern

  type timsreader_for_each_merged_spectrum_cb = CFuncPtr7[CLong, CInt, CInt, Ptr[CDouble], Ptr[CFloat], CInt, Ptr[Unit], Unit]

  def timsreader_for_each_merged_spectrum(
    tims_data_dir: CString,
    cb_context: Ptr[Unit],
    cb: timsreader_for_each_merged_spectrum_cb,
    err_msg_ptr: Ptr[CString]
  ): Int = extern

}