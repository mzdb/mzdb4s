package com.github.mzdb4s.db.model.params

import scala.collection.Seq
import param._
import thermo._

//@XmlRootElement(name = "activation")
class Activation() extends AbstractParamTree

//@XmlType(name="AnalyzerComponent")
class AnalyzerComponent(order: Int) extends Component(order)

class Component(
  //@XmlAttribute
  var order: Int = 0
) extends AbstractParamTree {
  def getOrder(): Int = order

  // See: https://stackoverflow.com/questions/7370925/what-is-the-standard-idiom-for-implementing-equals-and-hashcode-in-scala
  override def canEqual(that: Any): Boolean = {
    that.isInstanceOf[Component]
  }

  override def equals(that: Any): Boolean = {
    super.equals(that) && {
      that match {
        case comp: Component =>
          ((this eq comp) // optional, but highly recommended sans very specific knowledge about this exact class implementation
            || (comp.canEqual(this) // optional only if this class is marked final
            && hashCode == comp.hashCode // optional, exceptionally execution efficient if hashCode is cached, at an obvious space inefficiency tradeoff
            && order == comp.order
            ))
        case _ =>
          false
      }
    }
  }

  // See: https://stackoverflow.com/questions/9068154/what-is-the-difference-between-and-hashcode/60748297#60748297
  override def hashCode(): Int = {
    31 * super.hashCode() + order.##
  }
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

case class FileContent() extends AbstractParamTree {
  def this(paramTree: ParamTree) = {
    this()

    this.setCVParams(paramTree.getCVParams())
    this.setUserParams(paramTree.getUserParams())
  }
}

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

private case class PrintableParamTree(cvParams: Seq[CVParam], userParams: Seq[UserParam], userTexts: Seq[UserText])

class ParamTree() extends AbstractParamTree with Equals {

  override def toString() = {
    PrintableParamTree(cvParams, userParams, userTexts).toString.replaceAllLiterally("PrintableParamTree","ParamTree")
  }
}

//@XmlRootElement(name = "precursor")
class Precursor(spectrumRef: String) extends AbstractParamTree {
  //@XmlAttribute(required = true)
  //protected var spectrumRef: String = _
  //@XmlElement(name = "isolationWindow")
  protected var isolationWindow: IsolationWindowParamTree = _
  //@XmlElement(name = "selectedIonList")
  protected var selectedIonList: SelectedIonList = _
  //@XmlElement(name = "activation")
  protected var activation: Activation = _

  def getIsolationWindow(): IsolationWindowParamTree = isolationWindow
  def setIsolationWindow(isolationWindow: IsolationWindowParamTree): Unit = {
    this.isolationWindow = isolationWindow
  }

  def getActivation(): Activation = activation
  def setActivation(activation: Activation): Unit = {
    this.activation = activation
  }

  def getSelectedIonList(): SelectedIonList = selectedIonList
  def setSelectedIonList(selectedIonList: SelectedIonList): Unit = {
    this.selectedIonList = selectedIonList
  }

  def parseFirstSelectedIonMz(): Option[Double] = {
    val ionOpt = this.getSelectedIonList().getSelectedIons().headOption
    ionOpt.map(_.getCVParam(PsiMsCV.SELECTED_ION_MZ).getValue.toDouble)
  }
}

//@XmlRootElement(name = "scanList")
class ScanList private() extends AbstractParamTree {
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
  def setScans(scans: Seq[ScanParamTree]): Unit = {
    this.scans = scans
  }
}

class ScanParamTree(instrumentConfigurationRef: String) extends AbstractParamTree {
  //@XmlElementWrapper
  protected var scanWindowList: ScanWindowList = _

  def getScanWindowList(): ScanWindowList = scanWindowList
  def setScanWindowList(scanWindowList: ScanWindowList): Unit = {
    this.scanWindowList = scanWindowList
  }

  def getThermoMetaData(): ThermoScanMetaData = {
    val filterStringCvParam = this.getCVParam(PsiMsCV.FILTER_STRING)
    if (filterStringCvParam == null) return null
    ThermoScanMetaData(filterStringCvParam.getValue)
  }
}

//@XmlRootElement(name = "scanWindow")
class ScanWindow extends AbstractParamTree

//@XmlRootElement(name = "scanWindowList")
class ScanWindowList private() extends AbstractParamTree {
  //@XmlAttribute(required = true)
  //@XmlSchemaType(name = "nonNegativeInteger")
  protected var count = 0
  //@XmlElementWrapper(name = "scanWindow")
  protected var scanWindows: Seq[ScanWindow] = _

  def this(c: Int) {
    this()
    this.count = c
  }

  def getScanWindows(): Seq[ScanWindow] = scanWindows
  def setScanWindows(scanWindows: Seq[ScanWindow]): Unit = {
    this.scanWindows = scanWindows
  }
}

class SelectedIon() extends AbstractParamTree

class SelectedIonList private() extends AbstractParamTree {
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
  def setSelectedIons(selectedIons:  Seq[SelectedIon]): Unit = {
    this.selectedIons = selectedIons
  }
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