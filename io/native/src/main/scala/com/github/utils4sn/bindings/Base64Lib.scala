package com.github.utils4sn.bindings

import scala.scalanative.unsafe._

@extern
@link("base64")
object Base64Lib {

  // int Base64encode_len(int len);
  @name("Base64encode_len")
  def calcEncodedStrLen(rawStrLen: CInt): CInt = extern

  // int Base64encode(char * coded_dst, const char *plain_src,int len_plain_src);
  @name("Base64encode")
  def encode(dest: CString, src: CString, srcLen: CInt): CInt = extern

  // int Base64decode_len(const char * coded_src);
  @name("Base64decode_len")
  def calcDecodedStrLen(codedSrc: CString): CInt = extern

  // int Base64decode(char * plain_dst, const char *coded_src);
  @name("Base64decode")
  def decode(dest: CString, src: CString): CInt = extern

}

