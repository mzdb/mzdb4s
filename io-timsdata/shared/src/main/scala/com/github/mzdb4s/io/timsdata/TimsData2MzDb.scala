package com.github.mzdb4s.io.timsdata

import java.io.File
import java.nio.ByteOrder
import scala.collection.mutable.ArrayBuffer

import com.github.mzdb4s.Logging
import com.github.mzdb4s.db.model._
import com.github.mzdb4s.db.model.params._
import com.github.mzdb4s.db.model.params.param._
import com.github.mzdb4s.io.writer.MzDbWriter
import com.github.mzdb4s.msdata._
import com.github.mzdb4s.util.collection._
import com.github.sqlite4s.ISQLiteFactory

/*
object TimsData2MzDb extends com.github.mzdb4s.Logging {

  private val LOG = this.logger

  val defaultTimsFilePath = "D:/Dev/timstof/200ngHeLaPASEF_2min_compressed.d"

  def main(args: Array[String]): Unit = {
    var inst = null
    val convertArgs = new Nothing

    /*try {
      var fileToConvert = defaultTimsFilePath
      var ms1Method = SpectrumGeneratingMethod.SMOOTH
      if (convertArgs.filename != null) {
        LOG.info("File name set to " + convertArgs.filename)
        fileToConvert = convertArgs.filename
      }
      else LOG.info("NO File name set, use " + defaultTimsFilePath)
      if (convertArgs.ms1 != null) {
        LOG.info("Ms1 set to " + convertArgs.ms1)
        ms1Method = convertArgs.ms1
      }
      else LOG.info("NO ms1 set, use SMOOTH")
      val ttDir = new Nothing(fileToConvert)
      if (!ttDir.exists) {
        LOG.error("File " + fileToConvert + " does not exist !! ")
        System.exit(1)
      }
      inst = new Timstof2Mzdb(ttDir, ms1Method)
      inst.createMZdBData()
    } catch {
      case pe: Nothing =>
        LOG.info("Error parsing arguments: " + pe.getMessage)
        cmd.usage
      case e: Exception =>
        // TODO Auto-generated catch block
        e.printStackTrace()
    } finally if (inst != null) {
      LOG.info("Close file")
      inst.closeFile()
    }*/
  }
}
*/

class TimsData2MzDb(timsDataDirPath: String, mzDbFilePath: String)(implicit tdr: ITimsDataReader, sf: ISQLiteFactory) extends Logging {
  private val timsDataDir = new File(timsDataDirPath)
  require(timsDataDir.isDirectory, "can't find directory at: " + timsDataDirPath)

  private val tdfReader = new TDFMetaDataReader(timsDataDirPath, true) // TODO: false
  //initFramesData()

  private val centroidDataEncoding = DataEncoding(-1, DataMode.CENTROID, PeakEncoding.HIGH_RES_PEAK, "none", ByteOrder.LITTLE_ENDIAN)
  //private val profileDataEncoding = DataEncoding(-2, DataMode.PROFILE, PeakEncoding.HIGH_RES_PEAK, "none", ByteOrder.LITTLE_ENDIAN)
  //private val fittedDataEncoding = DataEncoding(-3, DataMode.FITTED, PeakEncoding.HIGH_RES_PEAK, "none", ByteOrder.LITTLE_ENDIAN)

  // FIXME: the mzDB spec doesn't allow currently multiple spectra with the same RT
  // TODO: remove this limitation in the future, for now we modify a bit the RT using a small epsilon
  //private val RT_EPSILON = 0.05f

  /*val noneOp: Option[_] = None

  //For all Spectra Index, map to associated Frame Index
  private var m_spectra2FrameIndex = null
  //Memorize first Spectra Index for each Frame (index)
  private var m_frame2FirstSpectraIndex = null
  private var m_frame2ReadSpectraCount = null
  private var m_frameById = null
  private var m_precursorByIds = null
   */

  /*private def closeFile(): Unit = {
    m_ttReader.closeTimstofFile(m_fileHdl)
  }*/

  /*private def initFramesData(): Unit = {
    val start = System.currentTimeMillis
    //Read TimsFrames with associated MetaData
    val frames = m_ttReader.getFullTimsFrames(m_fileHdl)
    Collections.sort(frames)
    //Init indexes map
    var spectrumIndex = 1
    m_spectra2FrameIndex = new Nothing
    m_frame2FirstSpectraIndex = new Nothing(frames.size * 4 / 3 + 1)
    m_frameById = new Nothing(frames.size * 4 / 3 + 1)
    import scala.collection.JavaConversions._
    for (tf <- frames) {
      val nbrSpectrum = tf.getSpectrumCount //VDS TODO For now 1 spectrum for MS ==> May have one per scans groups!!
      m_frame2FirstSpectraIndex.put(tf.getId, spectrumIndex)
      for (i <- 0 until nbrSpectrum) {
        m_spectra2FrameIndex.put(spectrumIndex, tf.getId)
        spectrumIndex += 1
      }
      m_frameById.put(tf.getId, tf)
    }
    m_frame2ReadSpectraCount = new Nothing(frames.size * 4 / 3 + 1)
    m_precursorByIds = m_ttReader.getPrecursorInfoById(m_fileHdl)
    val end = System.currentTimeMillis
    logger.info("Read meta data for " + frames.size + " frames and " + m_spectra2FrameIndex.size + " spectrum. Duration : " + (end - start) + " ms")
  }*/

  def convert(): Unit = {

    var writer: MzDbWriter = null

    try {
      tdfReader.open()

      writer = new MzDbWriter(
        new File(mzDbFilePath),
        this._createMzDbMetaData(),
        DefaultBBSizes(), //  BBSizes newNBbSize = new BBSizes(10000, 10000,0,0);
        isDIA = false
      )
      writer.open()

      // For test only: allows to test / debug the process of a single scan
      //testFrameAndQuit(177555);

      val frameById = tdfReader.getAllFrames().mapByLong(_.id)
      val precById = tdfReader.getAllPrecursors().mapByLong(_.id)
      //val msmsScansByFrameId = tdfReader.getAllMsMsScans().groupByLong(_.frameId)
      val pasefScansByFrameIdAndFirstScan = tdfReader.getAllPasefMsMsScans().map( ps => (ps.frameId,ps.scanMin) -> ps).toMap

      import MsType._

      var spId = 1 // Sprectrum ID starts at 1
      var cycle = 0 //VDS : TODO 1 cycle = 1Ms + xMSMS ?

      logger.info("Reading and processing spectra from TDF file...")

      tdr.forEachMergedSpectrum(timsDataDirPath, { (frameId: Long, firstScan: Int, lastScan: Int, mzValues: Array[Double], intensityValues: Array[Float]) =>

        val frame = frameById(frameId)
        val frameRT = frame.time

        val peaksCount = mzValues.length
        //println(peaksCount)

        val shOpt = if (peaksCount == 0) {
          logger.warn(s"merged spectrum #$spId is empty, and thus won't be written in the mzDB output file")
          None
        }
        else {
          // Determine the base peak
          var highestPeakIdx = 0
          var maxIntensity = 0f
          var peakIdx = 1
          while (peakIdx < peaksCount) {
            val intensity = intensityValues(peakIdx)
            if (intensity > maxIntensity) {
              highestPeakIdx = peakIdx
              maxIntensity = intensity
            }

            peakIdx += 1
          }
          val basedPeakMz = mzValues(highestPeakIdx)

          frame.msType match {
            case MS => { // Ms Frame

              cycle += 1

              Some(SpectrumHeader(
                id = spId,
                initialId = spId,
                title = s"Frame=$frameId",
                cycle = cycle,
                time = frameRT,
                msLevel = 1,
                activationType = None,
                peaksCount = peaksCount,
                isHighResolution = true,
                tic = intensityValues.sum,
                basePeakMz = basedPeakMz,
                basePeakIntensity = maxIntensity,
                precursorMz = None,
                precursorCharge = None,
                spId,
                precursor = null
              ))
            }
            /*case MSMS => {

            }*/
            case PASEF if peaksCount > 0 => {
              val pasefScanOpt = pasefScansByFrameIdAndFirstScan.get((frameId,firstScan))
              assert(
                pasefScanOpt.isDefined,
                s"can't retrieve PASEF scan information of range ($firstScan,$lastScan) in frame $frameId"
              )

              val precIdOpt = pasefScanOpt.get.precursorId
              assert(
                precIdOpt.isDefined,
                s"can't retrieve precursor of PASEF scan with range ($firstScan,$lastScan) in frame $frameId"
              )

              val precId = precIdOpt.get
              val prec = precById(precId)

              val spectrumTitle = s"Frame=$frameId;ScanRange=[$firstScan,$lastScan];Precursor=$precId"
              val mzDbPrec = new Precursor(spectrumTitle)

              Some(SpectrumHeader(
                id = spId,
                initialId = spId,
                title = spectrumTitle,
                cycle = cycle,
                time = frameRT + (firstScan.toFloat / 10000), // TODO: adjust me
                msLevel = 2,
                activationType = Some(ActivationType.CID), // FIXME: check if correct
                peaksCount = peaksCount,
                isHighResolution = true,
                tic = intensityValues.sum,
                basePeakMz = basedPeakMz,
                basePeakIntensity = maxIntensity,
                precursorMz = Some(prec.monoIsotopicMz),
                precursorCharge = Some(prec.charge),
                spId,
                precursor = mzDbPrec
              ))

            }
            case _ =>

              val warnMsg = s"unsupported frame type ${frame.msType} for frame $frameId " +
                "(only MS1, MSMS and PASEF frames are currently supported)"

              logger.warn(warnMsg)

              None

          } // end of match/case
        }


        if (shOpt.isDefined) {
          val spData = new SpectrumData(mzValues, intensityValues)

          val mzdb4sSp = Spectrum(shOpt.get, spData)
          val spectrumMetaData = SpectrumXmlMetaData(spId, "", "", None, None)

          writer.insertSpectrum(mzdb4sSp, spectrumMetaData, centroidDataEncoding)
        }

        if (spId % 1000 == 0) {
          logger.debug(s"Processed $spId spectra...")
        }

        spId+=1
      }) // ends forEachMergedSpectrum

    } finally {
      if (writer != null) {
        logger.debug("Closing mzDB writer...")
        tdfReader.close()
        writer.close()
      }
    }

  }

  /*
  private def fillmzDBPrecursor(mzdbPrecursor: Nothing, timstofPrecursor: Nothing, collEnergy: String): Unit = {
    val start = System.currentTimeMillis //-> VDS For timing logs
    val isolationParams = new IsolationWindowParamTree
    val isolationParamsList = new util.ArrayList[CVParam]
    isolationParamsList.add(new CVParam(PsiMsCV.ISOLATION_WINDOW_TARGET_MZ.getAccession, "isolation window target m/z", String.valueOf(timstofPrecursor.getMonoIsotopicMz), "MS", Some.apply("MS"), Some.apply("MS:1000040"), Some.apply("m/z")))
    isolationParams.setCVParams(JavaConverters.asScalaIteratorConverter(isolationParamsList.iterator).asScala.toSeq)
    mzdbPrecursor.isolationWindow_$eq(isolationParams)
    val activation = new Nothing
    val params = new util.ArrayList[CVParam]
    //<cvParam cvRef="MS" accession="MS:1000133" name="collision-induced dissociation" value=""/>
    params.add(new CVParam("MS:1000133", "collision-induced dissociation", "", "MS", noneOp, noneOp, noneOp))
    //<cvParam cvRef="MS" accession="MS:1000045" name="collision energy" value="30.0" unitCvRef="UO" unitAccession="UO:0000266" unitName="electronvolt"/>
    params.add(new CVParam("MS:1000045", "collision energy", collEnergy, "MS", Some.apply("UO"), Some.apply("UO:0000266"), Some.apply("electronvolt")))
    activation.setCVParams(JavaConverters.asScalaIteratorConverter(params.iterator).asScala.toSeq)
    mzdbPrecursor.activation_$eq(activation)
    val ionList = new SelectedIonList
    ionList.count_$eq(1)
    val ion = new SelectedIon
    val selectedIons = new util.ArrayList[SelectedIon]
    val ionParams = new util.ArrayList[CVParam]
    ionParams.add(new CVParam(PsiMsCV.SELECTED_ION_MZ.getAccession, "selected ion m/z", String.valueOf(timstofPrecursor.getMonoIsotopicMz), "MS", Some.apply("MS"), Some.apply("MS:1000040"), Some.apply("m/z")))
    ionParams.add(new CVParam("MS:1000041", "charge state", String.valueOf(timstofPrecursor.getCharge), "MS", noneOp, noneOp, noneOp))
    ionParams.add(new CVParam("MS:1000042", "peak intensity", String.valueOf(timstofPrecursor.getIntensity), "MS", Some.apply("MS"), Some.apply("MS:1000131"), Some.apply("number of counts")))
    ion.setCVParams(JavaConverters.asScalaIteratorConverter(ionParams.iterator).asScala.toSeq)
    selectedIons.add(ion)
    ionList.selectedIons_$eq(JavaConverters.asScalaIteratorConverter(selectedIons.iterator).asScala.toSeq)
    mzdbPrecursor.selectedIonList_$eq(ionList)
  }*/

  /*private def testFrameAndQuit(spId: Int): Unit = {
    val frameId = m_spectra2FrameIndex.get(spId)
    var nbrScanForFrame = m_frame2ReadSpectraCount.getOrDefault(frameId, 0)
    nbrScanForFrame += 1
    val timsFrame = m_frameById.get(frameId)
    if (timsFrame != null) if (!timsFrame.spectrumRead) {
      val tfs = Collections.singletonList(timsFrame)
      m_ttReader.fillFramesWithSpectrumData(m_fileHdl, tfs)
    }
    else logger.warn("#### NO FRAME " + frameId + " for spectra " + spId)
    val ttSpectrum = timsFrame.getSingleSpectrum(m_ms1Method)
    logger.info("spectrum {}  contains {} peaks", spId, ttSpectrum.getMasses.length)
    System.exit(0)
  }*/

  private def _createMzDbMetaData(): MzDbMetaData = {

    // Read global properties
    val globalProperties = tdfReader.getGlobalMetaData()

    val mzDbHeader = MzDbHeader(
      version = "0.7",
      creationTimestamp = (new java.util.Date().getTime / 1000).toInt,
      FileContent(), // FIXME: should be filled
      paramTree = ParamTree(
        userParams = List(
          UserParam(name = "origin_file_format", value = "Bruker TDF format", `type` = "xsd:string")
        ),
        cvParams = List(
          CVParam(accession="MS:1002817",name="Bruker TDF format")
        )
      )
    )

    val dataEncodings = List(centroidDataEncoding)/*
      profileDataEncoding,
      centroidDataEncoding,
      fittedDataEncoding
    )*/

    //CommonInstrumentParams ex
    /*
    <referenceableParamGroup id="CommonInstrumentParams">
      <cvParam cvRef="MS" accession="MS:1001911" name="Q Exactive" value=""/>
      <cvParam cvRef="MS" accession="MS:1000529" name="instrument serial number" value="Exactive Series slot #2533"/>
    </referenceableParamGroup>
    */

    val ciParams = CommonInstrumentParams(-1, ParamTree())

    val srcCompo = new SourceComponent(order = 1)
    // FIXME: this is only an example
    srcCompo.setCVParams(
      List(
        //<cvParam cvRef="MS" accession="MS:1000398" name="nanoelectrospray" value=""/>
        CVParam(accession="MS:1000398", name="nanoelectrospray"),
        //<cvParam cvRef="MS" accession="MS:1000485" name="nanospray inlet" value=""/>
        CVParam(accession="MS:1000485", name="nanospray inlet")
      )
    )

    // TODO: Add Analyzer and Detector component
    val compList = new ComponentList(List(srcCompo))
    var curSoftId = 1
    val iconf = InstrumentConfiguration(-1, "IC1", Some(curSoftId), ParamTree(), compList)

    val softList = new ArrayBuffer[Software]

    val acqSoftPropsOpt = globalProperties.get("AcquisitionSoftware")
    if (acqSoftPropsOpt.isDefined) {
      val name = acqSoftPropsOpt.get
      val version = globalProperties.getOrElse("AcquisitionSoftwareVersion", "Unknown")
      softList += Software(curSoftId, name, version, ParamTree())
      curSoftId += 1
    }

    val processMethodParamTree = ParamTree()
    val pm = ProcessingMethod(
      id = 1,
      number = 1,
      "TimsData2mzDB conversion",
      Some(processMethodParamTree),
      curSoftId
    )
    softList += Software(curSoftId, "TimsData2mzDB", "0.9.10", ParamTree())
    curSoftId += 1

    // Fake for mzdb-access
    //softList += Software(curSoftId, "mzDB", "0.9.10", ParamTree())

    val acqDateStrOpt = globalProperties.get("AcquisitionDateTime")
    val parsedDateOpt = acqDateStrOpt.map(DateParserFactory.getInstance().parseTDFDate)

    val timsDataName = timsDataDir.getName
    val run = Run(1, timsDataName, parsedDateOpt.getOrElse(new java.util.Date()), ParamTree())

    val sampleName = globalProperties.getOrElse("SampleName", timsDataName + "_Sample")
    val sample = Sample(1, sampleName, ParamTree())

    val sourceFile = SourceFile(1, timsDataName, timsDataDir.getAbsolutePath, ParamTree())

    logger.debug("Created MzDbMetaData")

    MzDbMetaData(
      mzDbHeader = mzDbHeader,
      dataEncodings = dataEncodings,
      commonInstrumentParams = ciParams,
      instrumentConfigurations = List(iconf),
      processingMethods = List(pm),
      runs = List(run),
      samples = List(sample),
      softwareList = softList,
      sourceFiles = List(sourceFile)
    )
  }

}