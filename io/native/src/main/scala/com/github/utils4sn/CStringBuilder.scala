package com.github.utils4sn

import scala.scalanative.unsafe._

import bindings.StrBuilderLib
import bindings.StrBuilderLib._

class CStringBuilderInstance(val sb: Ptr[StrBuilderLib.str_builder_t]) extends AnyVal {

  def addString(string: CString, length: CSize): CStringBuilderInstance = { str_builder_add_str(sb, string, length); this }
  def addChar(c: CChar): CStringBuilderInstance = { str_builder_add_char(sb, c); this }
  def addInt(value: CInt): CStringBuilderInstance = { str_builder_add_int(sb, value); this }

  def clear(): CStringBuilderInstance = { str_builder_clear(sb); this }
  def destroy(): Unit = str_builder_destroy(sb)

  def length(): CSize = str_builder_len(sb)
  def drop(length: CSize): CStringBuilderInstance = { str_builder_drop(sb, length); this }
  def truncate(length: CSize): CStringBuilderInstance = { str_builder_truncate(sb, length); this }

  def underlyingString(): CString = str_builder_peek(sb)
  def copyString(strLenPtr: Ptr[CSize]): CString = str_builder_dump(sb, strLenPtr)
}

object CStringBuilder {

  def create(): CStringBuilderInstance = {
    new CStringBuilderInstance(str_builder_create())
  }

}