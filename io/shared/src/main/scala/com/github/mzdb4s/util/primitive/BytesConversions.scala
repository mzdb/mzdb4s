package com.github.mzdb4s.util.primitive


/*
object Bytes2Hex {
  private val hexArray = "0123456789ABCDEF".toCharArray

  def convert(bytes: Array[Byte]): String = {
    val hexChars = new Array[Char](bytes.length * 2)

    var j = 0
    while (j < bytes.length) {
      val v = bytes(j) & 0xFF
      hexChars(j * 2) = hexArray(v >>> 4)
      hexChars(j * 2 + 1) = hexArray(v & 0x0F)

      j += 1
    }

    new String(hexChars)
  }
}*/

// Source: https://www.daniweb.com/programming/software-development/code/216874/primitive-types-as-byte-arrays
// TODO: remove this file? using ByteBuffer seems to be more elegant
// See: https://stackoverflow.com/questions/2905556/how-can-i-convert-a-byte-array-into-a-double-and-back
object PrimitivesToBytes {

  def apply(data: Int): Array[Byte] = Array[Byte](
    ((data >> 0) & 0xff).toByte,
    ((data >> 8) & 0xff).toByte,
    ((data >> 24) & 0xff).toByte,
    ((data >> 16) & 0xff).toByte
  )

  // BIG ENDIAN
  /*def apply(data: Int): Array[Byte] = Array[Byte](
    ((data >> 24) & 0xff).toByte,
    ((data >> 16) & 0xff).toByte,
    ((data >> 8) & 0xff).toByte,
    ((data >> 0) & 0xff).toByte
  )*/

  def apply(data: Array[Int]): Array[Byte] = {
    if (data == null) return null

    val bytes = new Array[Byte](data.length * 4)

    var i = 0
    while (i < data.length) {
      System.arraycopy(apply(data(i)), 0, bytes, i * 4, 4)
      i += 1
    }

    bytes
  }

  /* ========================= */
  def apply(data: Long): Array[Byte] = Array[Byte](
    ((data >> 0) & 0xff).toByte,
    ((data >> 8) & 0xff).toByte,
    ((data >> 16) & 0xff).toByte,
    ((data >> 24) & 0xff).toByte,
    ((data >> 32) & 0xff).toByte,
    ((data >> 40) & 0xff).toByte,
    ((data >> 48) & 0xff).toByte,
    ((data >> 56) & 0xff).toByte
  )

  // BIG ENDIAN
  /*def apply(data: Long): Array[Byte] = Array[Byte](
    ((data >> 56) & 0xff).toByte,
    ((data >> 48) & 0xff).toByte,
    ((data >> 40) & 0xff).toByte,
    ((data >> 32) & 0xff).toByte,
    ((data >> 24) & 0xff).toByte,
    ((data >> 16) & 0xff).toByte,
    ((data >> 8) & 0xff).toByte,
    ((data >> 0) & 0xff).toByte
  )*/

  def apply(data: Array[Long]): Array[Byte] = {
    if (data == null) return null

    val bytes = new Array[Byte](data.length * 8)

    var i = 0
    while (i < data.length) {
      System.arraycopy(apply(data(i)), 0, bytes, i * 8, 8)
      i += 1
    }

    bytes
  }

  /* ========================= */

  /*
  // THIS CODE SEEMS TO BE BROKEN
  //def apply(data: Float): Array[Byte] = apply(java.lang.Float.floatToRawIntBits(data))
  */

  def apply(data: Float): Array[Byte] = {
    val intBits = java.lang.Float.floatToIntBits(data)

    Array[Byte](
      (intBits >> 0).toByte,
      (intBits >> 8).toByte,
      (intBits >> 16).toByte,
      (intBits >> 24).toByte
    )
  }

  def apply(data: Array[Float]): Array[Byte] = {
    if (data == null) return null

    val bytes = new Array[Byte](data.length * 4)

    var i = 0
    while (i < data.length) {
      System.arraycopy(apply(data(i)), 0, bytes, i * 4, 4)
      i += 1
    }

    bytes
  }

  /* ========================= */
  def apply(data: Double): Array[Byte] = apply(java.lang.Double.doubleToRawLongBits(data))

  def apply(data: Array[Double]): Array[Byte] = {
    if (data == null) return null

    val bytes = new Array[Byte](data.length * 8)

    var i = 0
    while (i < data.length) {
       System.arraycopy(apply(data(i)), 0, bytes, i * 8, 8)
      i += 1
    }

    bytes
  }

}