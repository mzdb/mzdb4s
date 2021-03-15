package com.github.mzdb4s.io.reader.bb

trait IByteArrayWrapper extends Any {

  def position(pos: Int): Unit

  def getDouble(index: Int): Double
  def getFloat(index: Int): Float
  def getInt(index: Int): Int
  def getLong(index: Int): Long

  def getDouble(): Double
  def getFloat(): Float
  def getInt(): Int
  def getLong(): Long

}
