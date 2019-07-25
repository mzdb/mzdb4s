package com.github.mzdb4s

import utest._

import com.github.mzdb4s.io.MzDbContext
import com.github.mzdb4s.io.reader.cache.MzDbEntityCache
import com.github.mzdb4s.io.reader.iterator.SpectrumIterator
import com.github.mzdb4s.msdata.{AcquisitionMode, BBSizes}

abstract class AbstractMzDbReaderTests extends AbstractSQLiteTests {

  protected val filename__0_9_7 = "OVEMB150205_12.raw.0.9.7.mzDB"
  protected val filename__0_9_8 = "OVEMB150205_12.raw.0.9.8.mzDB"

  protected val FLOAT_EPSILON = 1E-4f
  protected val expectedBBSizes_OVEMB150205_12 = BBSizes(5, 10000, 15, 15)
  protected val expectedBBCount_OVEMB150205_12 = 3406
  protected val expectedCycleCount_OVEMB150205_12 = 158
  protected val expectedRunSliceCount_OVEMB150205_12 = 161
  protected val expectedSpectrumCount_OVEMB150205_12 = 1193
  protected val expectedDataEncodingCount_OVEMB150205_12 = 3
  protected val expectedMaxMSLevel_OVEMB150205_12 = 2
  protected val expectedCvParamsCount_OVEMB150205_12__0_9_7 = 0
  protected val expectedCvParamsCount_OVEMB150205_12__0_9_8 = 1
  protected val expectedLastRTTime_OVEMB150205_12 = 240.8635f
  protected val expectedModelVersion_OVEMB150205_12_0_9_7 = "0.6"
  protected val expectedModelVersion_OVEMB150205_12_0_9_8 = "0.7"
  protected val expectedAcquisitionMode_OVEMB150205_12__0_9_7 = AcquisitionMode.UNKNOWN
  protected val expectedAcquisitionMode_OVEMB150205_12__0_9_8 = AcquisitionMode.DDA
  protected val expectedDiaIsolationWindows_OVEMB150205_12 = Array()
  protected val minMz_OVEMB150205_12 = 400f
  protected val maxMz_OVEMB150205_12 = 600f
  protected val minRt_OVEMB150205_12 = 100f
  protected val maxRt_OVEMB150205_12 = 200f
  protected val expectedSpectrumSlicesCount_OVEMB150205_12 = 63
  protected val expectedSumIntensities_OVEMB150205_12__0_9_7 = 2.543672190435547E9
  protected val expectedSumIntensities_OVEMB150205_12__0_9_8 = 2.5717392830078125E9
  protected val expectedSumMz_OVEMB150205_12__0_9_7 = 3.868285366432487E7
  protected val expectedSumMz_OVEMB150205_12__0_9_8 = 3.867483975354004E7

  // TODO: do not divide and put the real number
  protected val expectedNbIntensities_OVEMB150205_12__0_9_7 = 155874 / 2
  protected val expectedNbIntensities_OVEMB150205_12__0_9_8 = 155838 / 2
  protected val expectedNbPeaks_OVEMB150205_12__0_9_7 = expectedNbIntensities_OVEMB150205_12__0_9_7
  protected val expectedNbPeaks_OVEMB150205_12__0_9_8 = expectedNbIntensities_OVEMB150205_12__0_9_8

  val tests = Tests {
    'readerTest_OVEMB150205_12__0_9_7 - readerTest_OVEMB150205_12__0_9_7()
    'readerTest_OVEMB150205_12__0_9_8 - readerTest_OVEMB150205_12__0_9_8()
  }

  /**
    * Non regression test date: jul 27th 2015
    */
  def readerTest_OVEMB150205_12__0_9_7(): Unit = {
    this.readerTest(
      filename__0_9_7,
      expectedModelVersion_OVEMB150205_12_0_9_7,
      expectedSumIntensities_OVEMB150205_12__0_9_7,
      expectedSumMz_OVEMB150205_12__0_9_7,
      expectedNbIntensities_OVEMB150205_12__0_9_7,
      expectedNbPeaks_OVEMB150205_12__0_9_7,
      expectedCvParamsCount_OVEMB150205_12__0_9_7,
      expectedAcquisitionMode_OVEMB150205_12__0_9_7
    )
    println(" OK")
  }

  def readerTest_OVEMB150205_12__0_9_8(): Unit = {
    this.readerTest(
      filename__0_9_8,
      expectedModelVersion_OVEMB150205_12_0_9_8,
      expectedSumIntensities_OVEMB150205_12__0_9_8,
      expectedSumMz_OVEMB150205_12__0_9_8,
      expectedNbIntensities_OVEMB150205_12__0_9_8,
      expectedNbPeaks_OVEMB150205_12__0_9_8,
      expectedCvParamsCount_OVEMB150205_12__0_9_8,
      expectedAcquisitionMode_OVEMB150205_12__0_9_8
    )
    println(" OK")
  }

  def readerTest(
    filename: String,
    expectedModelVersion: String,
    expectedSumIntensities: Double,
    expectedSumMz: Double,
    expectedNbIntensities: Int,
    expectedNbPeaks: Int,
    expectedCvParamsCount: Int,
    expectedAcquisitionMode: AcquisitionMode.Value
  ): Unit = {
    println(s"Non Regression test reading mzDB file '$filename':")

    def assertMzDbFileValue[T](
      valueExtractor: => T,
      refValue: T,
      valueName: String
    ): T = {
      assertEquals(
        valueExtractor,
        refValue,
        s"$valueName exception for $filename",
        s"invalid $valueName for $filename"
      )
    }

    def assertMzDbFileNumber[@specialized T](
      valueExtractor: => T,
      refValue: T,
      numberTol: Double,
      valueName: String
    )(implicit num: Numeric[T]): T = {
      assertThis(
        valueExtractor, { v: T => math.abs(num.toDouble(num.minus(refValue, v))) <= numberTol },
        s"$valueName exception for $filename",
        { v: T => s"invalid $valueName for $filename: got $v but expected $refValue" }
      )
    }

    val resDir = this.getResourceDir()

    val mzDbFile = new java.io.File(resDir + "/" + filename)
    println(mzDbFile)
    Predef.assert(mzDbFile.exists(), "can't find mzDbFile at: " + mzDbFile.getAbsolutePath)

      // create Reader
    val mzDb = assertNotNull(
      new MzDbReader(mzDbFile, entityCache = Some(new MzDbEntityCache()), logConnections = true),
      "MzDB reader instantiation exception for " + filename,
      "Reader cannot be created"
    )

    print(".")

    // Bounding boxes size
    val bbSizes = assertMzDbFileValue(
      mzDb.getBBSizes(), expectedBBSizes_OVEMB150205_12, "BBSize"
    )

    println("BBSizes = " + bbSizes)

    print(".")

    // Bounding boxes count
    assertMzDbFileValue(
      mzDb.getBoundingBoxesCount(), expectedBBCount_OVEMB150205_12, "BBCount"
    )

    print(".")

    // Cycle count
    assertMzDbFileValue(
      mzDb.getCyclesCount(), expectedCycleCount_OVEMB150205_12, "CycleCount"
    )

    print(".")

    // Run slice count
    assertMzDbFileValue(
      mzDb.getRunSlicesCount(), expectedRunSliceCount_OVEMB150205_12, "RunSliceCount"
    )

    print(".")

    // Spectrum count
    assertMzDbFileValue(
      mzDb.getSpectraCount(), expectedSpectrumCount_OVEMB150205_12, "SpectrumCount"
    )

    print(".")

    // Data Encoding count
    assertMzDbFileValue(
      mzDb.getDataEncodingsCount(), expectedDataEncodingCount_OVEMB150205_12, "DataEncodingCount"
    )

    print(".")

    // Max MS Level
    assertMzDbFileValue(
      mzDb.getMaxMsLevel(), expectedMaxMSLevel_OVEMB150205_12, "MaxMSLevel"
    )

    print(".")

    assertMzDbFileNumber(
      mzDb.getLastTime(), expectedLastRTTime_OVEMB150205_12, FLOAT_EPSILON, "LastTime"
    )

    print(".")

    // read Model Version
    assertMzDbFileValue(
      mzDb.getModelVersion(), expectedModelVersion, "ModelVersion"
    )

    print(".")

    // read Acquisition Mode
    assertMzDbFileValue(
      mzDb.getAcquisitionMode(), expectedAcquisitionMode, "AcquisitionMode"
    )

    print(".")

    // read DIA Isolation Window
    // FIXME: test has
    // try {
    // IsolationWindow[] diaIsolationWindows = mzDb.getDIAIsolationWindows();
    // // println(diaIsolationWindows.length);
    // // for (IsolationWindow w : diaIsolationWindows) {
    // // println("-------------------------------------------");
    // // println(w.getMinMz());
    // // println(w.getMaxMz());
    // // }
    // Assert.assertArrayEquals("AcquisitionMode " + filename + " invalid", new IsolationWindow[] {},
    // diaIsolationWindows);
    // } catch (SQLiteException e) {
    // Assert.fail("version exception " + e.getMessage() + " for " + filename);
    // }

    println("read spectrumSlices")
    print(".")

    try {
      val spectrumSlices = mzDb.getMsSpectrumSlices(minMz_OVEMB150205_12, maxMz_OVEMB150205_12, minRt_OVEMB150205_12, maxRt_OVEMB150205_12)
      assert(spectrumSlices != null)
      assert(spectrumSlices.length == expectedSpectrumSlicesCount_OVEMB150205_12)

      var nbIntensities = 0
      var nbPeaks = 0
      var sumIntensities = 0.0
      var sumMz = 0.0

      for (spectrumSlice <- spectrumSlices) {
        for (intensity <- spectrumSlice.getData.intensityList) {
          sumIntensities += intensity
        }
        for (mz <- spectrumSlice.getData.mzList) {
          sumMz += mz
        }
        nbIntensities += spectrumSlice.getData.intensityList.length
        nbPeaks += spectrumSlice.getData.peaksCount
      }

      assert(math.abs(expectedSumIntensities - sumIntensities) <= 1)
      assert(math.abs(expectedSumMz - sumMz) <= 1E-2)
      assert(nbIntensities == expectedNbIntensities)
      assert(nbPeaks == expectedNbPeaks)

    } catch {
      case e: Exception =>
        throw new Exception("spectrum slices extraction throws exception", e)
    }

    println("read Isolation Window")
    print(".")

    // read Isolation Window
    try {
      val runs = mzDb.getRuns()
      assert(runs.length == 1)

      val samples = mzDb.getSamples()
      assert(samples.length == 1)
      // println(diaIsolationWindows.length);

      for (run <- runs) {
        assert(run.getName == "OVEMB150205_12")
        assert(run.getId == 1)
        assert(run.getCVParams().length == expectedCvParamsCount)
        assert(run.getUserParams().isEmpty)
        assert(run.getUserTexts().isEmpty)
      }

      assert(samples.head.getName == "UPS1 5fmol R1")

      implicit val mzDbCtx: MzDbContext = mzDb.getMzDbContext()
      val iterator = new SpectrumIterator(mzDb, 1)

      var spectrumIndex = 0
      while (iterator.hasNext()) {
        val spectrum = iterator.next()
        val data = spectrum.getData
        val s = data.intensityList.length
        assert(data.intensityList.length == s)
        assert(data.mzList.length == s)
        assert(data.leftHwhmList.length == s)
        assert(data.rightHwhmList.length == s)
        spectrumIndex += 1
      }
      assert(spectrumIndex == expectedCycleCount_OVEMB150205_12)

    } catch {
      case e: Exception =>
        throw new Exception(s"version exception for $filename", e)
    }

    print(".")

    mzDb.close()
  }
}