package com.github.mzdb4s.io.thermo

object ThermoTests extends AbstractThermoTests {

  def initRawFileParserWrapper(): IRawFileParserWrapper = {

    RawFileParserWrapper.initialize(
      getProjectDir() + "/shared/src/main/resources/rawfileparser"
    )

    RawFileParserWrapper
  }


}