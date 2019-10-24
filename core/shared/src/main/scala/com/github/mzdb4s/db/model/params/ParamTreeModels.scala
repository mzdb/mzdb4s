package com.github.mzdb4s.db.model.params

import param._
import thermo._

//import javax.xml.bind.annotation._

//@XmlRootElement(name = "activation")
class Activation() extends AbstractParamTree

//@XmlType(name="AnalyzerComponent")
class AnalyzerComponent(order: Int) extends Component(order)

class Component(
  //@XmlAttribute
  var order: Int = 0
) extends AbstractParamTree {
  def getOrder(): Int = order
}

//@XmlRootElement(name = "componentList")
class ComponentList(
  //@XmlElements(Array(Array(new XmlElement(name = "detector", required = true, `type` = classOf[DetectorComponent]), new XmlElement(name = "analyzer", required = true, `type` = classOf[AnalyzerComponent]), new XmlElement(name = "source", required = true, `type` = classOf[SourceComponent]))))
  //@XmlElementWrapper
  var components: Seq[Component]
) extends AbstractParamTree {

  //@XmlAttribute(required = true)
  //@XmlSchemaType(name = "nonNegativeInteger")
  def count: Int = components.length
}

//@XmlType(name = "DetectorComponent")
class DetectorComponent(order: Int) extends Component(order) {}

//@XmlRootElement(name = "isolationWindow")
class IsolationWindowParamTree() extends AbstractParamTree

//@XmlRootElement(name = "params")
object ParamTree {
  def apply(
    cvParams: Seq[CVParam] = Seq.empty[CVParam],
    userParams: Seq[UserParam] = Seq.empty[UserParam],
    userTexts: Seq[UserText] = Seq.empty[UserText]
  ): ParamTree = {
    val paramTree = new ParamTree()
    paramTree.setCVParams(cvParams)
    paramTree.setUserParams(userParams)
    paramTree.setUserTexts(userTexts)
    paramTree
  }
}
class ParamTree() extends AbstractParamTree

//@XmlRootElement(name = "precursor")
class Precursor {
  //@XmlAttribute(required = true)
  protected var spectrumRef: String = null
  //@XmlElement(name = "isolationWindow")
  protected var isolationWindow: IsolationWindowParamTree = null
  //@XmlElement(name = "selectedIonList")
  protected var selectedIonList: SelectedIonList = null
  //@XmlElement(name = "activation")
  protected var activation: Activation = null

  def getSpectrumRef(): String = spectrumRef

  def getIsolationWindow(): IsolationWindowParamTree = isolationWindow

  def getActivation(): Activation = activation

  def getSelectedIonList(): SelectedIonList = selectedIonList

  def parseFirstSelectedIonMz(): Double = {
    val sil = this.getSelectedIonList()
    val si = sil.getSelectedIons().head
    val precMzAsStr = si.getCVParam(PsiMsCV.SELECTED_ION_MZ).getValue
    precMzAsStr.toDouble
  }
}

//@XmlRootElement(name = "scanList")
class ScanList() extends AbstractParamTree {
  //@XmlAttribute(required = true)
  //@XmlSchemaType(name = "nonNegativeInteger")
  protected var count = 0
  //@XmlElement(name = "scan")
  protected var scans: Seq[ScanParamTree] = _

  def this(c: Int) {
    this()
    this.count = c
  }

  def getScans(): Seq[ScanParamTree] = scans
}

class ScanParamTree() extends AbstractParamTree {
  //@XmlElementWrapper
  protected var scanWindowList: Seq[ScanWindowList] = _

  def getScanWindowList(): Seq[ScanWindowList] = scanWindowList

  def getThermoMetaData(): ThermoScanMetaData = {
    val filterStringCvParam = this.getCVParam(PsiMsCV.FILTER_STRING)
    if (filterStringCvParam == null) return null
    ThermoScanMetaData(filterStringCvParam.getValue)
  }
}

//@XmlRootElement(name = "scanWindow")
class ScanWindow extends AbstractParamTree

//@XmlRootElement(name = "scanWindowList")
class ScanWindowList() extends AbstractParamTree {
  //@XmlAttribute(required = true)
  //@XmlSchemaType(name = "nonNegativeInteger")
  protected var count = 0
  //@XmlElementWrapper(name = "scanWindow")
  protected var scanWindows: Seq[ScanWindow] = _

  def this(c: Int) {
    this()
    this.count = c
  }
}

class SelectedIon() extends AbstractParamTree

class SelectedIonList() extends AbstractParamTree {
  //@XmlAttribute(required = true)
  //@XmlSchemaType(name = "nonNegativeInteger")
  protected var count = 0
  //@XmlElement(name = "selectedIon")
  protected var selectedIons: Seq[SelectedIon] = _

  def this(c: Int) {
    this()
    this.count = c
  }

  def getSelectedIons(): Seq[SelectedIon] = selectedIons
}

//@XmlType(name = "SourceComponent")
class SourceComponent(order: Int) extends Component(order)


/*
sealed trait IComponent extends IParamTree {
  /** The order. */
  def order: Int
}

case class AnalyzerComponent(
  order: Int,
  cvParams: List[CVParam],
  userParams: List[UserParam],
  userTexts: List[UserText]
) extends IComponent

case class DetectorComponent(
  order: Int,
  cvParams: List[CVParam],
  userParams: List[UserParam],
  userTexts: List[UserText]
) extends IComponent

case class SourceComponent(
  order: Int,
  cvParams: List[CVParam],
  userParams: List[UserParam],
  userTexts: List[UserText]
) extends IComponent

class ComponentListBuilder() {
  private var order = 0
  private val components = new collection.mutable.ArrayBuffer[IComponent]()

  def addAnalyzerComponent(params: ParamTree): this.type = {
    order += 1
    AnalyzerComponent(order, params.cvParams, params.userParams, params.userTexts)
    this
  }

  def addDetectorComponent(params: ParamTree): this.type = {
    order += 1
    DetectorComponent(order, params.cvParams, params.userParams, params.userTexts)
    this
  }

  def addSourceComponent(params: ParamTree): this.type = {
    order += 1
    SourceComponent(order, params.cvParams, params.userParams, params.userTexts)
    this
  }

  def toComponentList(): ComponentList = ComponentList(components)
}

case class ComponentList(components: Seq[IComponent]) {
  //@XmlElements(Array(Array(new XmlElement(name = "detector", required = true, `type` = classOf[Nothing]), new XmlElement(name = "analyzer", required = true, `type` = classOf[AnalyzerComponent]), new XmlElement(name = "source", required = true, `type` = classOf[Nothing]))))
  //@XmlElementWrapper protected var components: util.List[Nothing] = null
  //@XmlAttribute(required = true)
  //@XmlSchemaType(name = "nonNegativeInteger")

  def count: Int = components.length
}


case class ScanList(
  scans: List[ScanParamTree],
  cvParams: List[CVParam],
  userParams: List[UserParam],
  userTexts: List[UserText]
) extends IParamTree {
  /*@XmlAttribute(required = true)
  @XmlSchemaType(name = "nonNegativeInteger") protected var count = 0
  @XmlElement(name = "scan")
  protected var scans: util.List[ScanParamTree] = null*/

  def count: Int = scans.length
}

case class ScanParamTree(
  scanWindowList: List[ScanWindowList],
  cvParams: List[CVParam],
  userParams: List[UserParam],
  userTexts: List[UserText]
) extends IParamTree {

  def getThermoMetaData(): Option[String] = {
    val searchedAC = CVEntry.FILTER_STRING.toString
    val filterStringCvParamOpt = this.cvParams.find(_.accession == searchedAC)
    filterStringCvParamOpt.map(_.value)
  }
}

case class ScanWindow(
  cvParams: List[CVParam],
  userParams: List[UserParam],
  userTexts: List[UserText]
) extends IParamTree


case class ScanWindowList(
  scanWindows: List[ScanWindow],
  cvParams: List[CVParam],
  userParams: List[UserParam],
  userTexts: List[UserText]
) extends IParamTree {
  //@XmlAttribute(required = true)
  //@XmlSchemaType(name = "nonNegativeInteger") protected var count = 0
  //@XmlElementWrapper(name = "scanWindow")
  //protected var scanWindows: util.List[ScanWindow] = null

  def count: Int = scanWindows.length
}


case class Activation(
  cvParams: List[CVParam],
  userParams: List[UserParam],
  userTexts: List[UserText]
) extends IParamTree

case class IsolationWindowParamTree(
  cvParams: List[CVParam],
  userParams: List[UserParam],
  userTexts: List[UserText]
) extends IParamTree

case class SelectedIon(
  cvParams: List[CVParam],
  userParams: List[UserParam],
  userTexts: List[UserText]
) extends IParamTree

case class SelectedIonList(
  selectedIons: List[SelectedIon],
  cvParams: List[CVParam],
  userParams: List[UserParam],
  userTexts: List[UserText]
) extends IParamTree {

  def count: Int = selectedIons.length
}

case class Precursor(
  spectrumRef: String,
  isolationWindow: IsolationWindowParamTree,
  selectedIonList: SelectedIonList,
  activation: Activation
) {

  def parseFirstSelectedIonMz(): Option[Double] = {
    val sil = this.selectedIonList
    val si = sil.selectedIons(0)
    val cvAC = CVEntry.SELECTED_ION_MZ.toString
    si.cvParams.find(_.accession == cvAC).map(_.value.toDouble)
  }
}

*/