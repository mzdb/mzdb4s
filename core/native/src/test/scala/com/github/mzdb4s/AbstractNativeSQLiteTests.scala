package com.github.mzdb4s

trait AbstractNativeSQLiteTests extends AbstractSQLiteTests {

  def initSQLiteFactory(): Unit = {
    com.github.sqlite4s.SQLiteFactoryRegistry.setFactory(
      com.github.sqlite4s.SQLiteFactory
    )
  }

}
