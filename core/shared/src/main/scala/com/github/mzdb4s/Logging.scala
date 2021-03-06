package com.github.mzdb4s

import scribe._
import scribe.filter._
import scribe.output.format.ASCIIOutputFormat

// Used to abstract Logging from used library
trait Logging extends scribe.Logging

object Logging {

  type LogLevel = com.github.mzdb4s.LogLevel
  val LogLevel = com.github.mzdb4s.LogLevel

  private val namespace = "com.github.mzdb4s"

  // Implementation of logger config function for scribe (https://github.com/outr/scribe/wiki/getting-started)
  // See: http://www.matthicks.com/2018/02/scribe-2-fastest-jvm-logger-in-world.html
  def configureLogger(minLogLevel: Logging.LogLevel = LogLevel.DEBUG): Unit = {

    // set log level, e.g. to DEBUG
    val scribeMinLogLevelOpt = Option(minLogLevel match {
      case LogLevel.OFF => null
      case LogLevel.ERROR => Level.Error
      case LogLevel.WARN => Level.Warn
      case LogLevel.INFO => Level.Info
      case LogLevel.DEBUG => Level.Debug
      case LogLevel.TRACE => Level.Trace
    })

    val logger = scribe.Logger.root.clearHandlers().clearModifiers().withHandler(
      minimumLevel = scribeMinLogLevelOpt,
      outputFormat = ASCIIOutputFormat
    )

    if (scribeMinLogLevelOpt.isDefined) {
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
    }

  }
}

sealed abstract class LogLevel {
  def value: Int
  @inline final def >=(other: LogLevel): Boolean = this.value >= other.value
}

object LogLevel  {
  case object OFF   extends LogLevel { val value = 0 }
  case object ERROR extends LogLevel { val value = 1 }
  case object WARN  extends LogLevel { val value = 2 }
  case object INFO  extends LogLevel { val value = 3 }
  case object DEBUG extends LogLevel { val value = 4 }
  case object TRACE extends LogLevel { val value = 5 }
}