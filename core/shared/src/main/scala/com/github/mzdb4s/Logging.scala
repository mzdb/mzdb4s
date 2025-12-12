package com.github.mzdb4s

import scribe._
import scribe.filter._
import scribe.output.format.ASCIIOutputFormat

// Used to abstract Logging from used library
trait Logging extends scribe.Logging {
  @inline def canLogTrace: Boolean = LogLevel.TRACE <= Logging.MAX_LOG_LEVEL
  @inline def canLogDebug: Boolean = LogLevel.DEBUG <= Logging.MAX_LOG_LEVEL
  @inline def canLogInfo: Boolean = LogLevel.INFO <= Logging.MAX_LOG_LEVEL
  @inline def canLogWarn: Boolean = LogLevel.WARN <= Logging.MAX_LOG_LEVEL
  @inline def canLogError: Boolean = LogLevel.ERROR <= Logging.MAX_LOG_LEVEL
}

object Logging {

  type LogLevel = com.github.mzdb4s.LogLevel
  val LogLevel = com.github.mzdb4s.LogLevel

  private val namespace = "com.github.mzdb4s"
  private val sqlite4sNamespace = "com.github.sqlite4s"
  private var MAX_LOG_LEVEL: LogLevel = LogLevel.DEBUG

  // Implementation of logger config function for scribe (https://github.com/outr/scribe/wiki/getting-started)
  // See: http://www.matthicks.com/2018/02/scribe-2-fastest-jvm-logger-in-world.html
  def configureLogger(maxLogLevel: Logging.LogLevel = LogLevel.DEBUG): Unit = {
    MAX_LOG_LEVEL = maxLogLevel

    // set log level, e.g. to DEBUG
    val scribeMinLogLevelOpt = Option(maxLogLevel match {
      case LogLevel.OFF => null
      case LogLevel.ERROR => Level.Error
      case LogLevel.WARN => Level.Warn
      case LogLevel.INFO => Level.Info
      case LogLevel.DEBUG => Level.Debug
      case LogLevel.TRACE => Level.Trace
    })

    /*val logger = scribe.Logger.root.clearHandlers().clearModifiers().withHandler(
      minimumLevel = scribeMinLogLevelOpt,
      outputFormat = ASCIIOutputFormat
    )

    this.logger = if (scribeMinLogLevelOpt.isDefined) {
      logger.withModifier(
        select(packageName.startsWith(namespace))
          .exclude(level < scribeMinLogLevelOpt.get)
          .priority(Priority.High)
      ).replace()
    } else {
      logger.withModifier(
        select(packageName.startsWith(namespace))
          .exclude(level <= Level.Error)
          .priority(Priority.High)
      ).replace()
    }*/

    // FIXME: we should use orpan() loggers defined by name/package
    // See: https://github.com/outr/scribe/issues/463#issuecomment-1580945299

    // Configure the global root logger
    if (scribeMinLogLevelOpt.isEmpty) {
      scribe.Logger.root.clearHandlers().clearModifiers().replace()
    } else {
      scribe.Logger.root.clearHandlers().clearModifiers().withHandler(
        minimumLevel = scribeMinLogLevelOpt,
        outputFormat = ASCIIOutputFormat,
        writer = scribe.writer.SystemErrWriter,
        modifiers = List(
          select(packageName(namespace))
            .include(level === Level.Error)
            .excludeUnselected
        )
      )
      .withHandler(
        minimumLevel = scribeMinLogLevelOpt,
        outputFormat = ASCIIOutputFormat,
        writer = scribe.writer.SystemOutWriter,
        modifiers = List(
          select(packageName(namespace))
            .include(level < Level.Error)
        )
      )/*.withHandler(
        minimumLevel = None,
        writer = scribe.writer.NullWriter,
        modifiers = List(select(packageName(sqlite4sNamespace)).priority(Priority.High))
      )*//*.withHandler(
        minimumLevel = None,
        modifiers = List(
          select(packageName(sqlite4sNamespace))
            .include(level === Level.Fatal)
            .excludeUnselected
            .priority(Priority.High)
        )
      )*//*.withHandler(
        minimumLevel = Some(Level.Fatal),
        modifiers = List(
          select(packageName.startsWith(sqlite4sNamespace))
            .include(level === Level.Fatal)
            .excludeUnselected
        )
      )*/.replace()
    }

  }
}

sealed abstract class LogLevel {
  def value: Int
  @inline final def >=(other: LogLevel): Boolean = this.value >= other.value
  @inline final def <=(other: LogLevel): Boolean = this.value <= other.value
}

object LogLevel  {
  case object OFF   extends LogLevel { val value = 0 }
  case object ERROR extends LogLevel { val value = 1 }
  case object WARN  extends LogLevel { val value = 2 }
  case object INFO  extends LogLevel { val value = 3 }
  case object DEBUG extends LogLevel { val value = 4 }
  case object TRACE extends LogLevel { val value = 5 }
}