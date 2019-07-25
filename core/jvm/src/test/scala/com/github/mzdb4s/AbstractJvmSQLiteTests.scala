package com.github.mzdb4s

import java.io.File

trait AbstractJvmSQLiteTests extends AbstractSQLiteTests {

  def initSQLiteFactory(): Unit = {
    com.github.sqlite4s.SQLiteFactoryRegistry.setFactory(
      com.github.sqlite4s.SQLiteFactory
    )
  }

  override protected def getResourceDir(): File = {
    val resources = this.getClass.getClassLoader.getResources("")
    val resourceDir = resources.nextElement().getFile.split("/target/scala-").head + "/../shared/src/test/resources"
    new File(resourceDir)
  }

}
