package com.github.mzdb4s.io.reader.bb

import java.nio.{ByteBuffer, ByteOrder}

object ByteArrayWrapper {
  def apply(bytes: Array[Byte], byteOrder: ByteOrder): IByteArrayWrapper = {
    new ByteBufferWrapper(ByteBuffer.wrap(bytes).order(byteOrder))
  }
}

class ByteBufferWrapper(val byteBuffer: ByteBuffer) extends AnyVal with IByteArrayWrapper {
  def position(pos: Int): Unit = byteBuffer.position(pos)
  def getDouble(index: Int): Double = byteBuffer.getDouble(index)
  def getFloat(index: Int): Float = byteBuffer.getFloat(index)
  def getInt(index: Int): Int = byteBuffer.getInt(index)
  def getLong(index: Int): Long = byteBuffer.getLong(index)
  def getDouble(): Double = byteBuffer.getDouble()
  def getFloat(): Float = byteBuffer.getFloat()
  def getInt(): Int = byteBuffer.getInt()
  def getLong(): Long = byteBuffer.getLong()
}