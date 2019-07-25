package com.github.mzdb4s.util.ms

object MsUtils {

  /** The proton mass. */
  val protonMass = 1.007276466812

  /**
    *
    * @param mz
    * @param errorInPPM
    * @return
    */
  @inline def ppmToDa(mz: Double, errorInPPM: Double): Double = mz * errorInPPM / 1e6

  /**
    *
    * @param mz
    * @param d
    * @return
    */
  @inline def DaToPPM(mz: Double, d: Double): Double = d * 1e6 / mz
}
