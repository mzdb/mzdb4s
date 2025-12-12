package com.github.mzdb4s.util.lang

import scala.language.implicitConversions

object String {

  class LazySplit(str: String, separator: Char) extends scala.collection.immutable.IndexedSeq[String] {

    private val _strLen = str.length

    // Count separators
    private var separatorCount = _countSeparators()

    def _countSeparators(): Int = {
      var count = 0

      var i: Int = 0
      while (i < _strLen) {
        if (str.charAt(i) == separator) {
          count += 1
        }
        i += 1
      }

      count
    }

    private val _beginIndices = new Array[Int](separatorCount + 1)
    private val _endIndices = new Array[Int](separatorCount + 1)

    def _indexString(): Unit = {

      var sepIdx = 0
      var charIdx: Int = 0
      _beginIndices(sepIdx) = charIdx

      while (charIdx < _strLen) {
        if (str.charAt(charIdx) == separator) {
          _endIndices(sepIdx) = charIdx - 1
          sepIdx += 1
          _beginIndices(sepIdx) = charIdx + 1
        }
        charIdx += 1
      }

      _endIndices(sepIdx) += _strLen - 1
    }

    _indexString()

    private val _nSubstrings = _beginIndices.length

    def length: Int = _nSubstrings

    /** Get the element at the specified index. */
    def apply(i: Int): String = {
      if (i < 0 || i >= _nSubstrings)
        throw new java.lang.IndexOutOfBoundsException(i.toString)
      str.substring(_beginIndices(i), _endIndices(i) + 1)
    }

  }

  implicit class RichString(val str: String) extends AnyVal {
    def lazySplit(separator: Char): LazySplit = new LazySplit(str, separator)
  }
}

