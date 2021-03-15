package com.github.mzdb4s.io.reader.table

import scala.collection.Seq

import com.github.mzdb4s.db.model._
import com.github.mzdb4s.db.table._
import com.github.mzdb4s.io.MzDbContext
import com.github.mzdb4s.io.reader.param.IParamTreeParser
import com.github.sqlite4s.query.SQLiteRecord

object ParamTreeParser {
  val instance: IParamTreeParser = com.github.mzdb4s.io.reader.param.ParamTreeParser
}

class InstrumentConfigReader()(implicit val mzDbContext: MzDbContext) extends AbstractTableModelReader[InstrumentConfiguration] {

  def extractRecord(r: SQLiteRecord): InstrumentConfiguration = {
    val id = r.columnInt(InstrumentConfigurationTable.ID)
    val name = r.columnString(InstrumentConfigurationTable.NAME)
    val softwareId = r.columnInt(InstrumentConfigurationTable.SOFTWARE_ID)
    val paramTreeAsStr = r.columnString(InstrumentConfigurationTable.PARAM_TREE)
    val insConfAsStr = r.columnString(InstrumentConfigurationTable.COMPONENT_LIST)

    val paramTree = if (paramTreeAsStr == null) null else ParamTreeParser.instance.parseParamTree(paramTreeAsStr)
    InstrumentConfiguration(id, name, Some(softwareId), paramTree, ParamTreeParser.instance.parseComponentList(insConfAsStr))
  }

  def getInstrumentConfig(id: Int): InstrumentConfiguration = getRecord(InstrumentConfiguration.TABLE_NAME, id)

  def getInstrumentConfigList(): Seq[InstrumentConfiguration] = getRecordList(InstrumentConfiguration.TABLE_NAME)
}

class MzDbHeaderReader()(implicit val mzDbContext: MzDbContext) extends AbstractTableModelReader[MzDbHeader] {

  def extractRecord(r: SQLiteRecord): MzDbHeader = {
    val version = r.columnString(MzdbTable.VERSION)
    val creationTimestamp = r.columnInt(MzdbTable.CREATION_TIMESTAMP)
    val fileContentAsStr = r.columnString(MzdbTable.FILE_CONTENT)
    val paramTreeAsStr = r.columnString(MzdbTable.PARAM_TREE)

    val fileContent = ParamTreeParser.instance.parseFileContent(fileContentAsStr)

    val paramTree = ParamTreeParser.instance.parseParamTree(paramTreeAsStr)

    MzDbHeader(version, creationTimestamp, fileContent, paramTree)
  }

  def getMzDbHeader(): MzDbHeader = {
    mzDbContext.newSQLiteQuery("SELECT * FROM " + MzdbTable.tableName).extractRecord(this)
  }
}

/*object RunReader {
  import java.text.SimpleDateFormat
  private val dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

  @inline
  def parseDate(dataStr: String): java.util.Date = dateFormat.parse(dataStr)
}*/

class RunReader()(implicit val mzDbContext: MzDbContext) extends AbstractTableModelReader[Run] {

  def extractRecord(r: SQLiteRecord): Run = {
    val id = r.columnInt(RunTable.ID)
    val name = r.columnString(RunTable.NAME)

    // FIXME: switch to Instant when Java 8 is supported
    //Instant startTimestamp = Instant.parse( r.columnString(RunTable.START_TIMESTAMP));
    val startTimestampAsStr = r.columnString(RunTable.START_TIMESTAMP)
    /*var startTimestamp = null
    try startTimestamp = DateUtils.parseDate(startTimestampAsStr, Array[String]("yyyy-MM-dd'T'HH:mm:ss'Z'"))
    catch {
      case e: ParseException =>
        logger.error("can't parse START_TIMESTAMP '" + startTimestampAsStr + "'in mzDB file: return current date")
    }*/

    val startTimestamp: java.util.Date = com.github.mzdb4s.util.date.DateParser.parseIsoDate(startTimestampAsStr)

    val paramTreeAsStr = r.columnString(RunTable.PARAM_TREE)

    Run(
      id,
      name,
      startTimestamp,
      ParamTreeParser.instance.parseParamTree(paramTreeAsStr),
      instrumentConfigId = Some(r.columnInt(RunTable.DEFAULT_INSTRUMENT_CONFIG_ID)),
      sampleId = Some(r.columnInt(RunTable.SAMPLE_ID)),
      sourceFileId = if (r.columnNull(RunTable.DEFAULT_SOURCE_FILE_ID)) None else Some(r.columnInt(RunTable.DEFAULT_SOURCE_FILE_ID))
    )
  }

  def getRun(id: Int): Run = getRecord(Run.TABLE_NAME, id)

  def getRunList(): Seq[Run] = getRecordList(Run.TABLE_NAME)

}

class SampleReader()(implicit val mzDbContext: MzDbContext) extends AbstractTableModelReader[Sample] {

  def extractRecord(r: SQLiteRecord): Sample = {
    val id = r.columnInt(SampleTable.ID)
    val name = r.columnString(SampleTable.NAME)
    val paramTreeAsStr = r.columnString(SampleTable.PARAM_TREE)
    Sample(id, name, ParamTreeParser.instance.parseParamTree(paramTreeAsStr))
  }

  def getSample(id: Int): Sample = getRecord(Sample.TABLE_NAME, id)

  def getSampleList(): Seq[Sample] = getRecordList(Sample.TABLE_NAME)
}

class SoftwareReader()(implicit val mzDbContext: MzDbContext) extends AbstractTableModelReader[Software] {

  def extractRecord(r: SQLiteRecord): Software = {
    val id = r.columnInt(SoftwareTable.ID)
    val name = r.columnString(SoftwareTable.NAME)
    val version = r.columnString(SoftwareTable.VERSION)
    val paramTreeAsStr = r.columnString(SoftwareTable.PARAM_TREE)
    Software(id, name, version, ParamTreeParser.instance.parseParamTree(paramTreeAsStr))
  }

  def getSoftware(id: Int): Software = getRecord(Software.TABLE_NAME, id)

  def getSoftwareList(): Seq[Software] = getRecordList(Software.TABLE_NAME)
}

class SourceFileReader()(implicit val mzDbContext: MzDbContext) extends AbstractTableModelReader[SourceFile] {

  def extractRecord(r: SQLiteRecord): SourceFile = {
    val id = r.columnInt(SourceFileTable.ID)
    val name = r.columnString(SourceFileTable.NAME)
    val location = r.columnString(SourceFileTable.LOCATION)
    val paramTreeAsStr = r.columnString(SourceFileTable.PARAM_TREE)
    val paramTree = if (paramTreeAsStr == null || paramTreeAsStr.isEmpty) null
    else ParamTreeParser.instance.parseParamTree(paramTreeAsStr)

    SourceFile(id, name, location, paramTree)
  }

  def getSourceFile(id: Int): SourceFile = getRecord(SourceFile.TABLE_NAME, id)

  def getSourceFileList(): Seq[SourceFile] = getRecordList(SourceFile.TABLE_NAME)
}