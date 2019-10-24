package com.github.mzdb4s

import slogging.{ LogLevel => SLogLevel, _}

// Used to abstract Logging from used library
trait Logging extends slogging.LazyLogging

object Logging {

  type LogLevel = com.github.sqlite4s.LogLevel
  val LogLevel = com.github.sqlite4s.LogLevel

  private val namespace = "com.github.mzdb4s"

  // TODO: use scribe instead for better performance (https://github.com/outr/scribe/wiki/getting-started)
  // See: http://www.matthicks.com/2018/02/scribe-2-fastest-jvm-logger-in-world.html
  def configureLogger(logLevel: Logging.LogLevel = LogLevel.DEBUG): Unit = {

    // select logger backend, e.g. simple logging using println (supported by Scala/JVM, Scala.js, and Scala Native)
    LoggerConfig.factory = PrintLoggerFactory()

    // set log level, e.g. to DEBUG
    LoggerConfig.level = logLevel match {
      case LogLevel.OFF => SLogLevel.OFF
      case LogLevel.ERROR => SLogLevel.ERROR
      case LogLevel.WARN => SLogLevel.WARN
      case LogLevel.INFO => SLogLevel.INFO
      case LogLevel.DEBUG => SLogLevel.DEBUG
      case LogLevel.TRACE => SLogLevel.TRACE
    }

    // use stderr for ERROR and WARN
    PrintLoggerFactory.errorStream = System.err
    PrintLoggerFactory.warnStream = System.err

    // use stdout for all other levels
    PrintLoggerFactory.infoStream = System.out
    PrintLoggerFactory.debugStream = System.out
    PrintLoggerFactory.traceStream = System.out

    /**
      * FilterLogger: filter function is called after the current value of LoggerConfig.level has been checked.
      * Hence, if you want to log TRACE statements for a specific source using FilterLogger,
      * you need to set FilterConfig.level = LogLevel.TRACE.
      * This also means that all TRACE logging statements in the code are executed,
      * even if they are subsequently discarded by NullLogger, which may have a serious impact on performance.
      */
    FilterLogger.filter = {
      // use NullLogger for all trace statements from sources starting with "com.github.sqlite4s"
      case (_,source) if source.startsWith(namespace) => NullLogger
      // log all other levels
      case _ => PrintLogger
    }
  }
}