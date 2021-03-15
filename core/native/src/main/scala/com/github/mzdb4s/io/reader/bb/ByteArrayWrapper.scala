package com.github.mzdb4s.io.reader.bb

import java.nio.ByteOrder

import scala.scalanative.unsafe._
import scala.scalanative.runtime.ByteArray

object ByteArrayWrapper {
  def apply(bytes: Array[Byte], byteOrder: ByteOrder): IByteArrayWrapper = {
    assert(byteOrder == ByteOrder.LITTLE_ENDIAN, "BIG ENDIAN not supported yet, please use the JVM implementation")
    new BytePtrWrapper(bytes.asInstanceOf[ByteArray].at(0), bytes.length)
  }
}

// TODO: check Ptr boundaries ?
class BytePtrWrapper(bytePtr: Ptr[Byte], length: Int) extends IByteArrayWrapper {

  private var curPos = 0

  def position(pos: Int): Unit = { curPos = pos }

  def getDouble(index: Int): Double = (bytePtr + index).asInstanceOf[Ptr[Double]].apply(0)
  def getFloat(index: Int): Float = (bytePtr + index).asInstanceOf[Ptr[Float]].apply(0)
  def getInt(index: Int): Int = (bytePtr + index).asInstanceOf[Ptr[Int]].apply(0)
  def getLong(index: Int): Long = (bytePtr + index).asInstanceOf[Ptr[Long]].apply(0)

  def getDouble(): Double = {
    val value = (bytePtr + curPos).asInstanceOf[Ptr[Double]].apply(0)
    curPos += 8
    value
  }

  def getFloat(): Float = {
    val value = (bytePtr + curPos).asInstanceOf[Ptr[Float]].apply(0)
    curPos += 4
    value
  }

  def getInt(): Int = {
    val value = (bytePtr + curPos).asInstanceOf[Ptr[Int]].apply(0)
    curPos += 4
    value
  }

  def getLong(): Long = {
    val value = (bytePtr + curPos).asInstanceOf[Ptr[Long]].apply(0)
    curPos += 8
    value
  }
}

/*class BytePtrWrapper(bytePtr: Ptr[Byte], length: Int) extends IByteArrayWrapper {

  private var curPtr: Ptr[Byte] = bytePtr

  def position(pos: Int): Unit = { curPtr = bytePtr + pos }

  def getDouble(index: Int): Double = (bytePtr + index).asInstanceOf[Ptr[Double]].apply(0)
  def getFloat(index: Int): Float = (bytePtr + index).asInstanceOf[Ptr[Float]].apply(0)
  def getInt(index: Int): Int = (bytePtr + index).asInstanceOf[Ptr[Int]].apply(0)
  def getLong(index: Int): Long = (bytePtr + index).asInstanceOf[Ptr[Long]].apply(0)

  def getDouble(): Double = {
    val value = curPtr.asInstanceOf[Ptr[Double]].apply(0)
    curPtr += sizeof[Double]
    value
  }

  def getFloat(): Float = {
    val value = curPtr.asInstanceOf[Ptr[Float]].apply(0)
    curPtr += sizeof[Float]
    value
  }

  def getInt(): Int = {
    val value = curPtr.asInstanceOf[Ptr[Int]].apply(0)
    curPtr += sizeof[Int]
    value
  }

  def getLong(): Long = {
    val value = curPtr.asInstanceOf[Ptr[Long]].apply(0)
    curPtr += sizeof[Long]
    value
  }
}
*/