package com.github.mzdb4s.io.timsdata

import java.io.File
import java.nio.ByteOrder
import scala.collection.mutable.ArrayBuffer

import com.github.mzdb4s.Logging
import com.github.mzdb4s.db.model._
import com.github.mzdb4s.db.model.params._
import com.github.mzdb4s.db.model.params.param._
import com.github.mzdb4s.io.mgf._
import com.github.mzdb4s.io.reader.MgfReader
import com.github.mzdb4s.io.writer.MzDbWriter
import com.github.mzdb4s.msdata._
import com.github.mzdb4s.util.collection._
import com.github.mzdb4s.util.ms.MsUtils
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

    val ms2MzTolPPM = 50

    //val writtenPrecById = new collection.mutable.LongMap[Boolean] // only used of MGF file loaded
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

      val mgfSpectraByParentFrameId = new collection.mutable.LongMap[ArrayBuffer[(String,Double,Int,Float,Array[Double],Array[Float])]]
      //val mgfSpectrumDataByPrecId = new collection.mutable.LongMap[(Double,Int,Array[Double],Array[Float])]

      val mgfFileStrOpt = timsDataDir.list().find(_.endsWith(".mgf"))
      val mgfFileExists = mgfFileStrOpt.isDefined
      if (mgfFileExists) {
        val mgfFileStr = mgfFileStrOpt.get
        logger.info("The TIMS data directory contains an MGF file, it will be used as reference for MS/MS spectra")

        logger.debug(s"Loading MS/MS spectra from MGF file '$mgfFileStr'")

        val mgfReader = new MgfReader(new File(timsDataDir,mgfFileStr), 1024 * 1024)

        mgfReader.foreachMgfSpectrum { spectrum =>

          /*
          val title = spectrum.mgfHeader.entries.find(_.field == MgfField.TITLE).get.value.toString
          val len = title.length

          val cmpId = new StringBuffer()
          var j = 0
          while (j < len) {
            val char = title(j)
            if (char == ',') j = len
            else if (char.isDigit) {
              cmpId.append(char)
            }
            j += 1
          }
          */

          val rawScansStr = spectrum.mgfHeader.entries.find(_.field == MgfField.RAWSCANS).get.value.toString
          val rawScansStrLen = rawScansStr.length

          val parentMsFrameId = new StringBuffer()
          var j = 0
          while (j < rawScansStrLen) {
            val char = rawScansStr(j)
            if (char.isDigit) {
              parentMsFrameId.append(char)
              j += 1
            } else {
              j = rawScansStrLen
            }
          }

          val mgfHeaderEntries = spectrum.mgfHeader.entries
          var specTitle: String = ""
          var pepMass: Double = 0.0
          var pepCharge: Int = 0
          var rtInSeconds: Float = 0f

          mgfHeaderEntries.foreach { mgfHeaderEntry =>
            mgfHeaderEntry.field match {
              case MgfField.TITLE => specTitle = mgfHeaderEntry.value.toString
              case MgfField.PEPMASS => pepMass = mgfHeaderEntry.value.toString.split("\\s").head.toDouble
              case MgfField.CHARGE => pepCharge = mgfHeaderEntry.value.toString.takeWhile(_ != '+').toInt
              case MgfField.RTINSECONDS => rtInSeconds = mgfHeaderEntry.value.toString.toFloat
              case _ => {}
            }
          }
          //val specTitle = mgfHeaderEntries.find(_.field == MgfField.TITLE).get.value.toString
          //val pepCharge = mgfHeaderEntries.find(_.field == MgfField.CHARGE).map(_.value.toString.takeWhile(_ != '+').toInt).getOrElse(0)
          //val pepMassStr = mgfHeaderEntries.find(_.field == MgfField.PEPMASS).get.value.toString
          //val pepMass = pepMassStr.split("\\s").head.toDouble

          /*val precsOpt = precsByParentFrameId.get(parentMsFrameId.toString.toInt)
          assert(precsOpt.isDefined,s"can't find precursors for MS frame with ID=$parentMsFrameId")

          val precs = precsOpt.get

          val nearestPrec = precs.minBy(s => math.abs(s.mz - pepMass))
          if (math.abs(nearestPrec.mz - pepMass) > 0.5) {
            logger.warn(s"can't find a precursor of mass=$pepMass (z=$pepCharge) for MS frame with ID=$parentMsFrameId")
          } else if (mgfSpectrumDataByPrecId.contains(nearestPrec.id)) {
            logger.warn(s"precursor with id=${nearestPrec.id} already mapped, current mass=$pepMass (z=$pepCharge), MS frame with ID=$parentMsFrameId")
          } else {
            mgfSpectrumDataByPrecId.put(
              nearestPrec.id,
              (pepMass, pepCharge, spectrum.mzList,spectrum.intensityList)
            )
          }*/

          mgfSpectraByParentFrameId.getOrElseUpdate(
            parentMsFrameId.toString.toLong,
            new ArrayBuffer[(String,Double,Int,Float,Array[Double],Array[Float])]
          ) += Tuple6(specTitle,pepMass,pepCharge,rtInSeconds,spectrum.mzList,spectrum.intensityList)

          true // continue loading spectra
        }

        logger.debug(s"MGF file '$mgfFileStr' fully loaded!")
      }

      val frameById = tdfReader.getAllFrames().mapByLong(_.id)
      val precById = tdfReader.getAllPrecursors().mapByLong(_.id)
      //val precsByParentFrameId = tdfReader.getAllPrecursors().groupByLong(_.parentFrameId)
      //val msmsScansByFrameId = tdfReader.getAllMsMsScans().groupByLong(_.frameId)
      val pasefScansByFrameIdAndFirstScan = tdfReader.getAllPasefMsMsScans().map( ps => (ps.frameId,ps.scanMin) -> ps).toMap

      import MsType._

      var spId = 0 // Sprectrum ID starts at 1, will be incremented on creation
      var cycle = 0 //VDS : TODO 1 cycle = 1Ms + xMSMS ?

      def _createPasefSpectrumHeader(
        title: String,
        time: Float,
        mzValues: Array[Double],
        intensityValues: Array[Float],
        precursorMz: Option[Double],
        precursorCharge: Option[Int]
      ): SpectrumHeader = {
        val (basePeakIdx, maxIntensity) = _findBasePeakIndex(intensityValues)
        val basedPeakMz = mzValues(basePeakIdx)

        spId += 1

        SpectrumHeader(
          id = spId,
          initialId = spId,
          title = title,
          cycle = cycle,
          time = time,//frameRT + (firstScan.toFloat / 10000), // TODO: adjust me
          msLevel = 2,
          activationType = Some(ActivationType.CID), // FIXME: check if correct
          peaksCount = mzValues.length,
          isHighResolution = true,
          tic = intensityValues.sum,
          basePeakMz = basedPeakMz,
          basePeakIntensity = maxIntensity,
          precursorMz = precursorMz, //precMzOpt.orElse(Some(prec.mz)),
          precursorCharge = precursorCharge, //precChargeOpt.orElse(prec.charge),
          spId,
          precursor = new Precursor(title)
        )
      }

      logger.info("Reading and processing spectra from TDF file...")

      tdr.forEachMergedSpectrum(timsDataDirPath, { (frameId: Long, firstScan: Int, lastScan: Int, _mzValues: Array[Double], _intensityValues: Array[Float]) =>

        val frame = frameById(frameId)
        //var precMzOpt = Option.empty[Double]
        //var precChargeOpt = Option.empty[Int]

        // Post-processing of detected peaks (workaround for missing proper peak-picker in rust code)
        val (mzValues, intensityValues) = if (frame.msType == MS || !mgfFileExists) {
          //println(".")
          //val mzValues = _mzValues
          //val intensityValues = _intensityValues

          val sortedIndexedIntensities = _intensityValues.zipWithIndex.sortBy(- _._1)
          val initialPeaksCount = _mzValues.length

          val curAggregatedPeaks = new ArrayBuffer[(Double,Float)]
          val filteredPeaks = new ArrayBuffer[(Double,Float)]

          val usedPeakIndices = new collection.mutable.LongMap[Boolean]
          for ((intensity,curPeakIdx) <- sortedIndexedIntensities; if !usedPeakIndices.contains(curPeakIdx)) {
            val mz = _mzValues(curPeakIdx)
            curAggregatedPeaks += Tuple2(mz, intensity)
            usedPeakIndices.put(curPeakIdx,true)

            var peakIdx = curPeakIdx + 1
            var hasNextPeaks = true
            while (hasNextPeaks && peakIdx < initialPeaksCount) {
              val nextPeakMz = _mzValues(peakIdx)
              val mzDiffPPM = MsUtils.DaToPPM(mz, math.abs(mz - nextPeakMz))

              if (mzDiffPPM > ms2MzTolPPM) hasNextPeaks = false
              else {
                curAggregatedPeaks += Tuple2(nextPeakMz, _intensityValues(peakIdx))
                usedPeakIndices.put(peakIdx,true)
              }

              peakIdx += 1
            }

            peakIdx = curPeakIdx - 1
            var hasPrevPeaks = true
            while (hasPrevPeaks && peakIdx >= 0) {
              val prevPeakMz = _mzValues(peakIdx)
              val mzDiffPPM = MsUtils.DaToPPM(mz, math.abs(mz - prevPeakMz))

              if (mzDiffPPM > ms2MzTolPPM) hasPrevPeaks = false
              else {
                curAggregatedPeaks += Tuple2(prevPeakMz, _intensityValues(peakIdx))
                usedPeakIndices.put(peakIdx,true)
              }

              peakIdx -= 1
            }

            // TODO: compute weighted average instead
            filteredPeaks += curAggregatedPeaks.maxBy(_._2)
            curAggregatedPeaks.clear()
          }

          val (mzValuesBuf, intensityValuesBuf) = filteredPeaks.sortBy(_._1).unzip
          (mzValuesBuf.toArray, intensityValuesBuf.toArray)

        } /*else if (frame.msType == PASEF) {

          if (!mgfFileExists) (_mzValues, _intensityValues)
          else {
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

            val prec = precById(precIdOpt.get)

            val mgfSpectrumDataOpt = mgfSpectrumDataByPrecId.get(prec.id)
            if (mgfSpectrumDataOpt.isEmpty) {
              logger.warn("can't find an MGF spectrum for precursor with ID=" + prec.id)
              (_mzValues, _intensityValues)
            } else {
              val mgfSpectrumData = mgfSpectrumDataOpt.get
              precMzOpt = Some(mgfSpectrumData._1)
              precChargeOpt = Some(mgfSpectrumData._2)
              (mgfSpectrumData._3,mgfSpectrumData._4)
            }

            /*val mgfSpectraOpt = mgfSpectraByParentFrameId.get(prec.parentFrameId)
            if (mgfSpectraOpt.isEmpty) {
              logger.warn("can't find an MGF spectrum for precursor with ID=" + precIdOpt.get)
              (_mzValues, _intensityValues)
            } else {
              val mgfSpectra = mgfSpectraOpt.get
              val precMz = prec.monoIsotopicMz
              val nearestMgfSpectrum = mgfSpectra.minBy(s => math.abs(s._1 - precMz))
              if (math.abs(nearestMgfSpectrum._1 - precMz) > 0.001) {
                logger.warn("can't find an MGF spectrum for precursor with ID=" + precIdOpt.get)
                (_mzValues, _intensityValues)
              } else {
                (nearestMgfSpectrum._2, nearestMgfSpectrum._3)
              }
            }*/
          }
        } */ else {
          (_mzValues, _intensityValues)
        }

        val frameRT = frame.time
        val peaksCount = mzValues.length

        val shOpt = if (peaksCount == 0) {
          logger.warn(s"merged spectrum #$spId is empty, and thus won't be written in the mzDB output file")
          None
        }
        else {

          frame.msType match {
            case MS => { // Ms Frame
              //println("MS")

              spId += 1
              cycle += 1

              val (basePeakIdx, maxIntensity) = _findBasePeakIndex(intensityValues)
              val basedPeakMz = mzValues(basePeakIdx)

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
            case PASEF => {

              if (mgfFileExists) None
              else {

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

                val specHeader = _createPasefSpectrumHeader(
                  title = spectrumTitle,
                  time = frameRT + (firstScan.toFloat / 10000), // TODO: adjust me
                  mzValues = mzValues,
                  intensityValues = intensityValues,
                  precursorMz = Some(prec.mz),
                  precursorCharge = prec.charge
                )

                Some(specHeader)
              }
              /*val mzDbPrec = new Precursor(spectrumTitle)

              if (mgfFileExists && writtenPrecById.contains(precId)) {
                None
              }
              else {
                if (mgfFileExists)
                  writtenPrecById.put(precId, true)

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
                  precursorMz = precMzOpt.orElse(Some(prec.mz)),
                  precursorCharge = precChargeOpt.orElse(prec.charge),
                  spId,
                  precursor = mzDbPrec
                ))
              }*/
            }
            case _ =>

              val warnMsg = s"unsupported frame type ${frame.msType} for frame $frameId " +
                "(only MS1, MSMS and PASEF frames are currently supported)"

              logger.warn(warnMsg)

              None

          } // end of match/case
        }

        if (shOpt.isDefined) {
          _insertSpectrum(writer, shOpt.get, mzValues, intensityValues)
        }

        if (mgfFileExists && frame.msType == MS) {
          val mgfSpectraOpt = mgfSpectraByParentFrameId.get(frameId)
          if (mgfSpectraOpt.nonEmpty) {
            val mgfSpectra = mgfSpectraOpt.get.sortBy(_._4) // sort by ASC RT

            var tmpIdx = 0
            mgfSpectra.foreach { mgfSpectrum =>
              val mzValues = mgfSpectrum._5
              val intensityValues = mgfSpectrum._6

              val specHeader = _createPasefSpectrumHeader(
                title = mgfSpectrum._1,
                time = mgfSpectrum._4 + (tmpIdx.toFloat / 1000), // TODO: adjust me
                mzValues = mzValues,
                intensityValues = intensityValues,
                precursorMz = Some(mgfSpectrum._2),
                precursorCharge = Some(mgfSpectrum._3)
              )

              _insertSpectrum(writer, specHeader, mzValues, intensityValues)

              tmpIdx += 1
            }
          }
        } // ends if (mgfFileExists && frame.msType == MS)

      }) // ends forEachMergedSpectrum

    } finally {
      if (writer != null) {
        logger.debug("Closing mzDB writer...")
        tdfReader.close()
        writer.close()
      }
    }

  }

  private def _findBasePeakIndex(intensityValues: Array[Float]): (Int,Float) = {

    val peaksCount = intensityValues.length

    // Determine the base peak
    var basePeakIdx = 0
    var maxIntensity = 0f

    var peakIdx = 1
    while (peakIdx < peaksCount) {
      val intensity = intensityValues(peakIdx)
      if (intensity > maxIntensity) {
        basePeakIdx = peakIdx
        maxIntensity = intensity
      }

      peakIdx += 1
    }

    (basePeakIdx, maxIntensity)
  }

  def _insertSpectrum(writer: MzDbWriter, sh: SpectrumHeader, mzValues: Array[Double], intensityValues: Array[Float]): Unit = {
    val spId = sh.id
    val spData = new SpectrumData(mzValues, intensityValues)

    val mzdb4sSp = Spectrum(sh, spData)
    val spectrumMetaData = SpectrumXmlMetaData(spId, "", "", None, None)

    writer.insertSpectrum(mzdb4sSp, spectrumMetaData, centroidDataEncoding)

    if (spId % 1000 == 0) {
      logger.debug(s"Processed $spId spectra...")
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