package com.github.mzdb4s

import utest._

import com.github.sqlite4s.SQLite
import com.github.sqlite4s.c.util.CUtils

object SQLite4sTest extends AbstractNativeSQLiteTests {

  val tests = Tests {
    'checkSQLite - checkSQLite()
  }

  def checkSQLite(): Unit = {
    try {
      println("SQLite version: " + SQLite.getSQLiteVersion + " #" + SQLite.getSQLiteVersionNumber + " lib #" + CUtils.fromCString(SQLite.libVersion))
      // FIXME: call to SQLite.getSQLiteCompileOptions crashes
      //System.err.println("SQLite compilation options: " + SQLite.getSQLiteCompileOptions)
    } catch {
      case e: Exception =>
        System.err.println("SQLite library is not loaded: " + e.getMessage)
    }
  }
}
