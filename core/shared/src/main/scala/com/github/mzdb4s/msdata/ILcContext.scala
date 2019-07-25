package com.github.mzdb4s.msdata

trait ILcContext {
  def getSpectrumId(): Long
  def getElutionTime(): Float
}