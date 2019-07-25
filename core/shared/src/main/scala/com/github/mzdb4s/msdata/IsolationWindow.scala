package com.github.mzdb4s.msdata

import scala.beans.BeanProperty

case class IsolationWindow(
  @BeanProperty minMz: Double,
  @BeanProperty maxMz: Double
) {
  //TODO: DBO => remove ne (not necessary anymore with a case class)
  /*override def hashCode: Int = {
    val prime = 31
    var result = 1
    var temp = 0L
    temp = Double.doubleToLongBits(maxMz)
    result = prime * result + (temp ^ (temp >>> 32)).toInt
    temp = Double.doubleToLongBits(minMz)
    result = prime * result + (temp ^ (temp >>> 32)).toInt
    result
  }

  override def equals(obj: Any): Boolean = {
    if (this eq obj) return true
    if (obj == null) return false
    if (getClass ne obj.getClass) return false
    val other = obj.asInstanceOf[IsolationWindow]
    if (Double.doubleToLongBits(maxMz) != Double.doubleToLongBits(other.maxMz)) return false
    if (Double.doubleToLongBits(minMz) != Double.doubleToLongBits(other.minMz)) return false
    true
  }*/

  override def toString(): String = s"IsolationWindow [minMz=$minMz maxMz=$maxMz]"
}