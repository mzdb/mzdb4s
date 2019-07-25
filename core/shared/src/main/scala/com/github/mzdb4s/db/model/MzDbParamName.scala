package com.github.mzdb4s.db.model

sealed trait IMzDbParamName {
  def ms1BBTimeWidthParamName: String
  def msnBBTimeWidthParamName: String
  def ms1BBMzWidthParamName: String
  def msnBBMzWidthParamName: String
  def lossStateParamName: String
}

case object MzDbParamName_0_8 extends IMzDbParamName {
  val ms1BBTimeWidthParamName = "BB_width_ms1"
  val msnBBTimeWidthParamName = "BB_height_ms1"
  val ms1BBMzWidthParamName = "BB_height_msn"
  val msnBBMzWidthParamName = "BB_height_msn"
  val lossStateParamName = "is_no_loss"
}

case object MzDbParamName_0_9 extends IMzDbParamName {
  val ms1BBTimeWidthParamName = "ms1_bb_time_width"
  val msnBBTimeWidthParamName = "msn_bb_time_width"
  val ms1BBMzWidthParamName = "ms1_bb_mz_width"
  val msnBBMzWidthParamName = "msn_bb_mz_width"
  val lossStateParamName = "is_lossless"
}