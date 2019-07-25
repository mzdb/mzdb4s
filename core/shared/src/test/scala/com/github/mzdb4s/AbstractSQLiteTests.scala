package com.github.mzdb4s

import java.io.File

import utest._

import com.github.sqlite4s._

trait AbstractSQLiteTests extends TestSuite {

  def initSQLiteFactory(): Unit

  /*com.github.sqlite4s.SQLiteFactoryRegistry.setFactory(
    com.github.sqlite4s.SQLiteFactory
  )*/

  initSQLiteFactory()

  println(s"New test is starting...")
  implicit val sqliteFactory: ISQLiteFactory = com.github.sqlite4s.SQLiteFactoryRegistry.getFactory().orNull
  Predef.assert( sqliteFactory != null, "sqliteFactory is null")

  // TODO: use this trick also for the JVM implementation?
  // FIXME: find a better solution
  protected def getResourceDir(): File = {
    val currentDirectory = new java.io.File(".").getCanonicalPath
    println(currentDirectory)
    val resourceDir = currentDirectory + "/core/shared/src/test/resources"
    new File(resourceDir)
  }

  /** Some assertion utils **/

  @inline
  protected def assertThis[T](
    valueExtractor: => T,
    valueAssertion: T => Boolean,
    extractorErrorMsg: => String,
    mkAssertErrorMsg: T => String
  ): T = {
    val obtainedValue: T = try valueExtractor
    catch {
      case e: Exception => throw new Exception(extractorErrorMsg, e)
    }

    Predef.assert(valueAssertion(obtainedValue), mkAssertErrorMsg(obtainedValue))

    obtainedValue
  }

  @inline
  protected def assertEquals[T](
    valueExtractor: => T,
    refValue: T,
    extractorErrorMsg: => String,
    assertErrorMsg: => String
  ): T = {
    assertThis(valueExtractor, { v: T => v == refValue }, extractorErrorMsg, { v: T => s"$assertErrorMsg: got $v but expected $refValue" })
  }

  @inline
  protected def assertNotNull[T](
    valueExtractor: => T,
    extractorErrorMsg: => String,
    assertErrorMsg: => String
  ): T = {
    assertThis(valueExtractor, { v: T => v != null }, extractorErrorMsg, { v: T => assertErrorMsg } )
  }

  /*
  val tests = Tests {
    /*'test1 - {
      throw new Exception("test1")
    }*/
    'test2 - {
      val conn = factory.newSQLiteConnection()
      println(conn)

      conn.open()
      conn.exec("CREATE TABLE PERSON (ID INTEGER);")

      conn.dispose()

      2 //MemoryMappedFileInScala.run()
    }
    /*'test3 - {
      val a = List[Byte](1, 2)
      a(10)
    }*/
  }*/


}
