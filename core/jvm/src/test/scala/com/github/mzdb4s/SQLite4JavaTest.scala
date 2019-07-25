package com.github.mzdb4s

import utest._

import com.almworks.sqlite4java.SQLite

object SQLite4JavaTest extends AbstractJvmSQLiteTests {

  val tests = Tests {
    'checkSQLite - checkSQLite()
  }

  def checkSQLite(): Unit = try {
    println("SQLite version: " + SQLite.getSQLiteVersion + " #" + SQLite.getSQLiteVersionNumber + " lib #" + SQLite.getLibraryVersion)
    println("SQLite compilation options: " + SQLite.getSQLiteCompileOptions)
  } catch {
    case e: Exception =>
      System.err.println("SQLite library is not loaded: " + e.getMessage)
      System.err.println("if running test under Eclipse EDI, please add 'VM Argument' = '-Djava.library.path=/path/to/sqlite.library' in JUnit test run configuration")
      System.err.println("for Windows OS library version is sqlite4java-win32-x64-1.0.392.dll")
      System.err.println("Target resources may be used with the path './target/lib'")
      System.err.println("Maven resources may be used with a path like '{user_home}\\.m2\\repository\\com\\almworks\\sqlite4java\\sqlite4java-win32-x64\\1.0.392'")
  }
}