package com.github.mzdb4s.db.model.params.thermo

import scala.beans.BeanProperty

case class ThermoFragmentationTarget(
  /** The msLevel. */
  @BeanProperty msLevel: Int,

  /** The mz. */
  @BeanProperty mz: Double,

  /** The peak encoding. */
  @BeanProperty activationType: String,

  /** The collisionEnergy. */
  @BeanProperty collisionEnergy: Float
) /*{
  def getMsLevel(): Int = msLevel

  def getMz: Double = mz

  def getActivationType: String = activationType

  def getCollisionEnergy: Float = collisionEnergy
}*/