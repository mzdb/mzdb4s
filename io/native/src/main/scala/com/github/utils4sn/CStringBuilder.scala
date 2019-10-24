package com.github.utils4sn

import scala.scalanative.unsafe._

import bindings.StrBuilderLib
import bindings.StrBuilderLib._

class CStringBuilderInstance(val sb: Ptr[StrBuilderLib.str_builder_t]) extends AnyVal {

  def addString(string: CString, length: CSize): Unit = str_builder_add_str(sb, string, length)
  def addChar(c: CChar): Unit = str_builder_add_char(sb, c)
  def addInt(value: CInt): Unit = str_builder_add_int(sb, value)

  def clear(): Unit = str_builder_clear(sb)
  def destroy(): Unit = str_builder_destroy(sb)

  def length(): CSize = str_builder_len(sb)
  def drop(length: CSize): Unit = str_builder_drop(sb, length)
  def truncate(length: CSize): Unit = str_builder_truncate(sb, length)

  def underlyingString(): CString = str_builder_peek(sb)
  def copyString(strLenPtr: Ptr[CSize]): CString = str_builder_dump(sb, strLenPtr)
}

object CStringBuilder {

  def create(): CStringBuilderInstance = {
    new CStringBuilderInstance(str_builder_create())
  }

}