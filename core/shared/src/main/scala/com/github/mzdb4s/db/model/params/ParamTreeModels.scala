package com.github.mzdb4s.db.model.params

import param._
import thermo._

//import javax.xml.bind.annotation._

//@XmlRootElement(name = "activation")
class Activation() extends AbstractParamTree

//@XmlType(name="AnalyzerComponent")
class AnalyzerComponent() extends Component

class Component extends AbstractParamTree {
  //@XmlAttribute
  protected var order = 0
  def getOrder(): Int = order
}

//@XmlRootElement(name = "componentList")
class ComponentList() extends AbstractParamTree {
  //@XmlElements(Array(Array(new XmlElement(name = "detector", required = true, `type` = classOf[DetectorComponent]), new XmlElement(name = "analyzer", required = true, `type` = classOf[AnalyzerComponent]), new XmlElement(name = "source", required = true, `type` = classOf[SourceComponent]))))
  //@XmlElementWrapper
  protected var components: Seq[Component] = null
  //@XmlAttribute(required = true)
  //@XmlSchemaType(name = "nonNegativeInteger")
  protected var count = 0

  def this(c: Int) {
    this()
    this.count = c
  }
}

//@XmlType(name = "DetectorComponent")
class DetectorComponent extends Component {}

//@XmlRootElement(name = "isolationWindow")
class IsolationWindowParamTree() extends AbstractParamTree

//@XmlRootElement(name = "params")
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
class SourceComponent extends Component