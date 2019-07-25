package com.github.mzdb4s.db.model

import com.github.mzdb4s.msdata._

import scala.beans.BeanProperty

// TODO: add CV terms, user terms and units
// TODO: add CommonInstrumentParams
// TODO: add Processing Methods
// TODO: add Shared Param Tree
case class MzDbMetaData(
  @BeanProperty mzDbHeader: MzDbHeader,
  @BeanProperty dataEncodings: Seq[DataEncoding],
  //@BeanProperty commonInstrumentParams: CommonInstrumentParams,
  @BeanProperty instrumentConfigurations: Seq[InstrumentConfiguration],
  //@BeanProperty processingMethods: Seq[ProcessingMethod],
  @BeanProperty runs: Seq[Run],
  @BeanProperty samples: Seq[Sample],
  @BeanProperty softwareList: Seq[Software],
  @BeanProperty sourceFiles: Seq[SourceFile]
)

/**
  * @author David Bouyssie
  *
  */
/*class MzDbMetaData() {

  private var bbSizes = null
  private var dataEncodings = null
  private var dataProcessingNames = null
  private var instrumentConfigurations = null
  private var mzDbHeader = null
  private var runs = null
  private var samples = null
  private var softwareList = null
  private var sourceFiles = null

  def getBBSizes(): Nothing = bbSizes

  def setBBSizes(bbSizes: Nothing): Unit = this.bbSizes = bbSizes

  def getDataEncodings: Array[Nothing] = dataEncodings

  def setDataEncodings(dataEncodings: Array[Nothing]): Unit = this.dataEncodings = dataEncodings

  def getDataProcessingNames: Optional[Array[String]] = dataProcessingNames

  def setDataProcessingNames(dataProcessingNames: Optional[Array[String]]): Unit = this.dataProcessingNames = dataProcessingNames

  def getInstrumentConfigurations: Optional[util.List[InstrumentConfiguration]] = instrumentConfigurations

  def setInstrumentConfigurations(instrumentConfigurations: Optional[util.List[InstrumentConfiguration]]): Unit = this.instrumentConfigurations = instrumentConfigurations

  def getMzDbHeader: MzDbHeader = mzDbHeader

  def setMzDbHeader(mzDbHeader: MzDbHeader): Unit = this.mzDbHeader = mzDbHeader

  def getRuns: Optional[util.List[Run]] = runs

  def setRuns(runs: Optional[util.List[Run]]): Unit = this.runs = runs

  def getSamples: Optional[util.List[Sample]] = samples

  def setSamples(samples: Optional[util.List[Sample]]): Unit = this.samples = samples

  def getSoftwareList: util.List[Software] = softwareList

  def setSoftwareList(softwareList: util.List[Software]): Unit = this.softwareList = softwareList

  def getSourceFiles: util.List[SourceFile] = sourceFiles

  def setSourceFiles(sourceFiles: util.List[SourceFile]): Unit = this.sourceFiles = sourceFiles
}*/