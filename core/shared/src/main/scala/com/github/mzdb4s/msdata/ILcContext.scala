package com.github.mzdb4s.msdata

// TODO: move to lcmsdata package?
trait ILcContext {
  def getSpectrumId(): Long
  def getElutionTime(): Float
}