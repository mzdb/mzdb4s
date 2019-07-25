package com.github.mzdb4s.util.primitive

/**
  * Provides functions to easily convert short, int, string and float into/from byte[]
  * <UL>
  * .
  * <LI>short -- 2 bytes
  * <LI>int -- 4 bytes
  * <LI>float -- 4 bytes
  * <LI>long -- 8 bytes
  * <LI>double -- 8 bytes
  * <LI>string -- when stored in byte[], the first cell is the length of the string. (Also notice that string
  * longer than 255 will be truncated.
  * </UL>
  */
object BytesUtils {

  val BYTE_LEN = 1
  val SHORT_LEN = 2
  val INT_LEN = 4
  val FLOAT_LEN = 4
  val LONG_LEN = 8
  val DOUBLE_LEN = 8

  /**
    * translate int into bytes, stored in byte array starting from startIndex
    *
    * @param num
    * the integer to be translated
    * @param bytes
    * [] the byte array
    * @param startIndex
    * starting to store in this index
    * @ret the index of the cell after storing the number.
    */
  def intToBytes(num: Int, bytes: Array[Byte], startIndex: Int): Int = {
    bytes(startIndex) = (num & 0xff).toByte
    bytes(startIndex + 1) = ((num >> 8) & 0xff).toByte
    bytes(startIndex + 2) = ((num >> 16) & 0xff).toByte
    bytes(startIndex + 3) = ((num >> 24) & 0xff).toByte
    startIndex + 4
  }

  /**
    * Given a byte array, restore it as an int
    *
    * @param bytes
    * the byte array
    * @param startIndex
    * the starting index of the place the int is stored
    */
  def bytesToInt(bytes: Array[Byte], startIndex: Int): Int = (bytes(startIndex).toInt & 0xff) | ((bytes(startIndex + 1).toInt & 0xff) << 8) | ((bytes(startIndex + 2).toInt & 0xff) << 16) | ((bytes(startIndex + 3).toInt & 0xff) << 24)

  /**
    * translate float into bytes, stored in byte array starting from startIndex
    *
    * @param fnum
    * the float to be translated
    * @param bytes
    * [] the byte array
    * @param startIndex
    * starting to store in this index
    * @ret the index of the cell after storing the number.
    */
  def floatToBytes(fnum: Float, bytes: Array[Byte], startIndex: Int): Int = intToBytes(java.lang.Float.floatToIntBits(fnum), bytes, startIndex)

  def bytesToFloat(bytes: Array[Byte], startIndex: Int): Float = java.lang.Float.intBitsToFloat(bytesToInt(bytes, startIndex))

  /**
    * translate short into bytes, stored in byte array starting from startIndex
    *
    * @param num
    * the short to be translated
    * @param bytes
    * [] the byte array
    * @param startIndex
    * starting to store in this index
    * @ret the index of the cell after storing the number.
    */
  def shortToBytes(num: Short, bytes: Array[Byte], startIndex: Int): Int = {
    bytes(startIndex) = (num & 0xff).toByte
    bytes(startIndex + 1) = ((num >> 8) & 0xff).toByte
    startIndex + 2
  }

  /**
    * Given a byte array, restore it as a short
    *
    * @param bytes
    * the byte array
    * @param startIndex
    * the starting index of the place the int is stored
    */
  def bytesToShort(bytes: Array[Byte], startIndex: Int): Short = ((bytes(startIndex).toInt & 0xff) | ((bytes(startIndex + 1).toInt & 0xff) << 8)).toShort

  /**
    * Give a String less than 255 bytes, store it as byte array, starting with the length of the string. If
    * the length of the String is longer than 255, a warning is generated, and the string will be truncated.
    *
    * @param str
    * the string that is less than 255 bytes
    * @param bytes
    * the byte array
    * @param startIndex
    * the starting index where the string will be stored.
    * @ret the index of the array after storing this string
    */
  def stringToBytes(str: String, bytes: Array[Byte], startIndex: Int): Int = {
    var offset = startIndex + 1

    val len = str.length
    val temp = str.getBytes

    if (len > 255) {
      System.err.println("String has more than 255 bytes in \"stringToBytes\", it will be truncated.")
      bytes(startIndex) = 255.toByte
      System.arraycopy(temp, 0, bytes, offset, 255)
      offset + 255
    }
    else {
      bytes(startIndex) = len.toByte
      System.arraycopy(temp, 0, bytes, offset, len)
      offset + len
    }
  }

  /**
    * Given a byte array, restore a String out of it. the first cell stores the length of the String
    *
    * @param bytes
    * the byte array
    * @param startIndex
    * the starting index where the string is stored, the first cell stores the length
    * @ret the string out of the byte array.
    */
  def bytesToString(bytes: Array[Byte], startIndex: Int): String = {
    val len = bytes(startIndex).toInt & 0xff
    new String(bytes, startIndex + 1, len)
  }

  /**
    * Given a long, convert it into a byte array
    *
    * @param lnum
    * the long given to convert
    * @param bytes
    * the bytes where to store the result
    * @param startIndex
    * the starting index of the array where the result is stored.
    * @ret the index of the array after storing this long
    */
  def longToBytes(lnum: Long, bytes: Array[Byte], startIndex: Int): Int = {
    var i = 0
    while (i < 8) {
      bytes(startIndex + i) = ((lnum >> (i * 8)) & 0xff).toByte
      i += 1
    }
    startIndex + 8
  }

  /**
    * Given an array of bytes, convert it to a long, least significant byte is stored in the beginning.
    *
    * @param bytes
    * the byte array
    * @param startIndex
    * the starting index of the array where the long is stored.
    * @ret the long result.
    */
  def bytesToLong(bytes: Array[Byte], startIndex: Int): Long = { // the lower 4 bytes
    // long temp = (long)bytesToInt(bytes, startIndex) & (long)0xffffffff;
    // return temp | ((long)bytesToInt(bytes, startIndex+4) << 32);
    (bytes(startIndex).toLong & 0xff) | ((bytes(startIndex + 1).toLong & 0xff) << 8) | ((bytes(startIndex + 2).toLong & 0xff) << 16) | ((bytes(startIndex + 3).toLong & 0xff) << 24) | ((bytes(startIndex + 4).toLong & 0xff) << 32) | ((bytes(startIndex + 5).toLong & 0xff) << 40) | ((bytes(startIndex + 6).toLong & 0xff) << 48) | ((bytes(startIndex + 7).toLong & 0xff) << 56)
  }

  /**
    * Given a double, convert it into a byte array
    *
    * @param dnum
    * the double given to convert
    * @param bytes
    * the bytes where to store the result
    * @param startIndex
    * the starting index of the array where the result is stored.
    * @ret the index of the array after storing this double
    */
  def doubleToBytes(dnum: Double, bytes: Array[Byte], startIndex: Int): Int = longToBytes(java.lang.Double.doubleToLongBits(dnum), bytes, startIndex)

  /**
    * Given an array of bytes, convert it to a double, least significant byte is stored in the beginning.
    *
    * @param bytes
    * the byte array
    * @param startIndex
    * the starting index of the array where the long is stored.
    * @ret the double result.
    */
  def bytesToDouble(bytes: Array[Byte], startIndex: Int): Double = java.lang.Double.longBitsToDouble(bytesToLong(bytes, startIndex))
}