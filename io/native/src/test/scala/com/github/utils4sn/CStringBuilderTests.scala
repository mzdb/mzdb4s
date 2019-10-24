package com.github.utils4sn

import scala.scalanative.unsafe._

import com.github.sqlite4s.c.util.CUtils
import bindings.Base64Lib._
import utest._

object CStringBuilderTests extends TestSuite {

  val tests = Tests {
    'testLibStrBuilder - Zone { implicit z => testLibStrBuilder() }
  }

  def testLibStrBuilder()(implicit zone: Zone): Unit = {

    var i = 0
    while (i < 5) {
      val sb = CStringBuilder.create()

      sb.addChar('c'.toByte)
      sb.addChar('a'.toByte)
      sb.addChar('s'.toByte)
      sb.addChar('e'.toByte)

      val result = CUtils.fromCString(sb.copyString(null))
      println("string: "+ result)

      assert(result == "case")

      sb.destroy()

      i += 1
    }

  }
}
