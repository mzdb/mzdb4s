package com.github.mzdb4s.db.model

import scala.beans.BeanProperty
import scala.collection.Seq

import com.github.mzdb4s.msdata._
import params._

// TODO: add CV terms, user terms and units
// TODO: add Shared Param Tree
case class MzDbMetaData(
  @BeanProperty mzDbHeader: MzDbHeader,
  @BeanProperty dataEncodings: Seq[DataEncoding],
  @BeanProperty commonInstrumentParams: CommonInstrumentParams,
  @BeanProperty instrumentConfigurations: Seq[InstrumentConfiguration],
  @BeanProperty processingMethods: Seq[ProcessingMethod],
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


// TODO: merge these models with the ones written in TableModels.scala
/*
case class InstrumentConfiguration(
  /** The id. */
  @BeanProperty id: Int,

  /** The name. */
  @BeanProperty name: String,

  /** The param tree. */
  @BeanProperty paramTree: Option[ParamTree],

  @BeanProperty componentList: ComponentList,

  /** The software id. */
  @BeanProperty softwareId: Int

  //@BeanProperty commonInstrumentParamTreeId: Int // i.e. shared_param_tree_id
)*/

case class CommonInstrumentParams(
  /** The id. */
  @BeanProperty id: Int,

  /** The param tree. */
  @BeanProperty paramTree: ParamTree
)

/*case class IsolationWindow(
  minMz: Float,
  maxMz: Float,
  targetMz: Float
)*/

/*
// TODO: add fileContent add contacts?
case class MzDbHeader(
  /** The version. */
  @BeanProperty version: String,

  /** The creation timestamp. */
  @BeanProperty creationTimestamp: Int, // encoded as time in secs since epoch

  /** The param tree. */
  @BeanProperty paramTree: ParamTree

) {
  def this(version: String, creationTimestamp: Int, paramTree: ParamTree, bbSizes: BBSizes) {
    this(
      version,
      creationTimestamp,
      ParamTree(
        paramTree.getCVParams(),
        List(
          UserParam(name="ms1_bb_mz_width", value = bbSizes.BB_MZ_HEIGHT_MS1.toString, `type` = "xsd:float"),
          UserParam(name="ms1_bb_time_width", value = bbSizes.BB_RT_WIDTH_MS1.toString, `type` = "xsd:float"),
          UserParam(name="msn_bb_mz_width", value = bbSizes.BB_MZ_HEIGHT_MSn.toString, `type` = "xsd:float"),
          UserParam(name="msn_bb_time_width", value = bbSizes.BB_RT_WIDTH_MSn.toString, `type` = "xsd:float")
        ),
        paramTree.getUserTexts()
      )
    )
  }
}
*/

case class ProcessingMethod(
  /** The id. */
  @BeanProperty id: Int,

  @BeanProperty number: Int,

  @BeanProperty dataProcessingName: String,

  /** The param tree. */
  @BeanProperty paramTree: Option[ParamTree],

  /** The software id. */
  @BeanProperty softwareId: Int
)

/*
case class Run(
  @BeanProperty id: Int,

  @BeanProperty name: String,

  @BeanProperty startTimestamp: java.util.Date, // FIXME: "java.time.Instant" encoded as ISO8601 compliant string

  @BeanProperty paramTree: Option[ParamTree]
)

case class Sample(
  @BeanProperty id: Int,

  /** The name. */
  @BeanProperty name: String,

  @BeanProperty paramTree: Option[ParamTree]
)

case class Software(
  @BeanProperty id: Int,

  /** The name. */
  @BeanProperty name: String,

  /** The version. */
  @BeanProperty version: String,

  @BeanProperty paramTree: Option[ParamTree]
)

case class SourceFile(
  @BeanProperty id: Int,

  /** The name. */
  @BeanProperty name: String,

  /** The location. */
  @BeanProperty location: String,

  @BeanProperty paramTree: Option[ParamTree]
)*/