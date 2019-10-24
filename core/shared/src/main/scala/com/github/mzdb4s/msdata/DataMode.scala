package com.github.mzdb4s.msdata

object DataMode extends Enumeration {
  /** The profile. */
  val PROFILE: Value = Value(-1,"profile")

  /** The centroid. */
  val CENTROIDED: Value = Value(12,"centroided")

  /** The fitted. */
  val FITTED: Value = Value(20,"fitted")
}