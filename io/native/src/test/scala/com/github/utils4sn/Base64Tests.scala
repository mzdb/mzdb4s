package com.github.utils4sn

import scala.scalanative.unsafe._

import com.github.sqlite4s.c.util.CUtils
import bindings.Base64Lib._
import utest._

object Base64Tests extends TestSuite {

  val tests = Tests {
    'testLibBase64 - Zone { implicit z => testLibBase64() }
  }

  def testLibBase64()(implicit z: Zone ): Unit = {

    // encoding
    val word = "salut"
    val wordLen = word.length
    val cWord = CUtils.toCString(word)

    val base64Len = calcEncodedStrLen(wordLen)
    println(base64Len)

    //val resEnc = malloc(base64Len * sizeof[CChar]).cast[CString]
    val resEnc = alloc[CChar](base64Len)

    val retVal = encode(resEnc,cWord, wordLen)
    //println("retVal: " + retVal)
    println("test: " + CUtils.fromCString(resEnc))
    //printf("encoded result (%d): %s\n", retVal, resEnc)

    val decodedStrLen = calcDecodedStrLen(resEnc)
    println("decodedStrLen: " + decodedStrLen)

    val resDecode = alloc[CChar](decodedStrLen)
    val actualSize = decode(resDecode, resEnc)
    println("actualSize: " + actualSize)

    println("final: " + CUtils.fromCString(resDecode))

    assert(CUtils.fromCString(resDecode) == word)
  }
}
