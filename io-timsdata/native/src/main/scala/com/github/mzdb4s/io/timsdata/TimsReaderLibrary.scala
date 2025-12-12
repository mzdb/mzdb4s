package com.github.mzdb4s.io.timsdata

import scala.scalanative.unsafe._
//import scala.scalanative.unsigned._

@link("timsdata")
@link("timsdatareader")
@extern
object TimsReaderLibrary {

  def timsreader_init_logger(): Unit = extern

  def timsreader_print_text(): Unit = extern

  type timsreader_for_each_merged_spectrum_cb = CFuncPtr7[CLong, CUnsignedInt, CUnsignedInt, Ptr[CDouble], Ptr[CFloat], CSize, Ptr[Unit], Unit]
  type timsreader_for_each_merged_spectrum_v2_cb = CFuncPtr8[CLong, CUnsignedInt, CUnsignedInt, CUnsignedLong, Ptr[CDouble], Ptr[CFloat], CSize, Ptr[Unit], Unit]

  def timsreader_for_each_merged_spectrum(
    tims_data_dir: CString,
    cb_context: Ptr[Unit],
    cb: timsreader_for_each_merged_spectrum_cb,
    err_msg_ptr: Ptr[CString]
  ): Int = extern

  type timsreader_spectrum_merging_config = CStruct5[CBool, CUnsignedInt, CSize, CSize, CSize]
  /*
  pub struct SpectrumMergingConfig {
    pub ms1_only: bool,
    pub mz_tol_ppm: u32,
    pub min_data_points_count: usize,
    pub n_threads: usize,
    pub n_timsdata_dll_threads: usize,
}
   */

  def timsreader_for_each_merged_spectrum_v2(
    tims_data_dir: CString,
    config: Ptr[timsreader_spectrum_merging_config],
    cb_context: Ptr[Unit],
    cb: timsreader_for_each_merged_spectrum_v2_cb,
    err_msg_ptr: Ptr[CString]
  ): Int = extern

}