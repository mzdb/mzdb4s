package com.github.mzdb4s.util.io

import java.io._

// Source: https://stackoverflow.com/questions/8664705/how-to-read-file-from-end-to-start-in-reverse-order-in-java
object FastReversedLineInputStream {
  private val DEFAULT_MAX_LINE_BYTES = 8192 * 1024 // 8 Mo
  private val DEFAULT_BUFFER_SIZE = 8192 * 1024 // 8 Mo

  def createBufferedReader(
    file: File,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    maxLineBytes: Int = DEFAULT_MAX_LINE_BYTES
  ): BufferedReader = {
    new BufferedReader(new InputStreamReader(new FastReversedLineInputStream(file, bufferSize, maxLineBytes)))
  }

}

class FastReversedLineInputStream(
  val file: File,
  var bufferSize: Int,
  var maxLineBytes: Int
) extends InputStream with Closeable {

  def this(file: File) {
    this(file, FastReversedLineInputStream.DEFAULT_BUFFER_SIZE, FastReversedLineInputStream.DEFAULT_MAX_LINE_BYTES)
  }

  private var in: RandomAccessFile = _

  private var currentFilePos = 0L

  //private var bufferSize = 0
  private var buffer: Array[Byte] = _
  private var currentBufferPos = 0

  // private var maxLineBytes = 0
  private var currentLine: Array[Byte] = _
  private var currentLineWritePos = 0
  private var currentLineReadPos = 0
  private var lineBuffered = false

  _init()

  private def _init(): Unit = {

     this.maxLineBytes = getAdaptedSize(file.length, maxLineBytes)
     in = new RandomAccessFile(file, "r")
     currentFilePos = file.length - 1

     in.seek(currentFilePos)
    //println("is ok: ", in.readByte == 0xA)
     //if (in.readByte == 0xA) currentFilePos -= 1

     currentLine = new Array[Byte](maxLineBytes)
     currentLine(0) = 0xA.toByte

     this.bufferSize = getAdaptedSize(file.length, bufferSize)
     buffer = new Array[Byte](bufferSize)
     fillBuffer()
     fillLineBuffer()

     //println("maxLineBytes is: " + maxLineBytes)
     //println("bufferSize is: " + bufferSize)
  }

  private def getAdaptedSize(fileLength: Long, defaultSize: Int): Int = { // the size should never be bigger than twice the file length
    if (defaultSize * 2 > fileLength) {
      if (fileLength < 1024) fileLength.toInt else 1024
    } else defaultSize
  }

  @throws[IOException]
  override def read(): Int = {
    if (currentFilePos <= 0 && currentBufferPos < 0 && currentLineReadPos < 0) return -1

    if (!lineBuffered) fillLineBuffer()
    //println("currentLineReadPos in read: " + currentLineReadPos)

    if (lineBuffered) {
      if (currentLineReadPos == 0) {
        lineBuffered = false
      }

      val b = currentLine(currentLineReadPos)
      currentLineReadPos -= 1
      return b
    }

    0
  }

  @throws[IOException]
  private def fillBuffer(): Unit = {
    if (currentFilePos < 0) return

    //println("currentFilePos: " +currentFilePos)
    //println("bufferSize: " +bufferSize)

    if (currentFilePos < bufferSize) {
      in.seek(0)
      in.read(buffer)
      //println("buffer head: "  + buffer.head.asInstanceOf[Char])
      currentBufferPos = currentFilePos.toInt
      currentFilePos = -1
    }
    else {
      in.seek(currentFilePos)
      in.read(buffer)
      //println("buffer last: "  + buffer.last.asInstanceOf[Char])
      currentBufferPos = bufferSize - 1
      currentFilePos = currentFilePos - bufferSize
    }
  }

  /*@throws[IOException]
  // FIXME: this workaround is pretty slow under Scala native => try to understand why
  private def fillBuffer(): Unit = {
    if (currentFilePos < 0) return
    //println("currentFilePos: " + currentFilePos)

    // If we reach the beginning of the file (position should be smaller than the buffer)
    if (currentFilePos < bufferSize) {
      //println("seeking file")
      in.seek(0)
      buffer = new Array[Byte](currentFilePos.asInstanceOf[Int] + 1)
      in.readFully(buffer)
      currentBufferPos = currentFilePos.asInstanceOf[Int]
      currentFilePos = -1
    }
    else {
      in.seek(currentFilePos - buffer.length)
      in.readFully(buffer)
      //println("buffer last: "  + buffer.last.asInstanceOf[Char])
      currentBufferPos = bufferSize - 1
      currentFilePos = currentFilePos - bufferSize
    }
  }*/

  @throws[IOException]
  private def fillLineBuffer(): Unit = {
    currentLineWritePos = 1

    //println("filling line buffer...")

    var reachedEndOfLine = false
    while (reachedEndOfLine == false) { // we've read all the buffer - need to fill it again
      if (currentBufferPos < 0) {
        fillBuffer()

        // nothing was buffered - we reached the beginning of a file
        if (currentBufferPos < 0) {
          currentLineReadPos = currentLineWritePos - 1
          lineBuffered = true
          return
        }
      }

      val b = buffer(currentBufferPos)
      currentBufferPos -= 1
      //println("b: " + b.asInstanceOf[Char])

      // \n is found - line fully buffered
      if (b == 0xA) {
        currentLineReadPos = currentLineWritePos - 1
        lineBuffered = true
        reachedEndOfLine = true // break while loop
      }
      // just ignore \r for now
      else if (b == 0xD) {}
      else {
        if (currentLineWritePos == maxLineBytes)
          throw new IOException("file has a line exceeding " + maxLineBytes + " bytes; use constructor to pickup bigger line buffer")

        //println(".")

        // write the current line bytes in reverse order - reading from
        // the end will produce the correct line
        currentLine(currentLineWritePos) = b
        currentLineWritePos += 1
      }
    }

    //println("currentLineWritePos: " + currentLineWritePos)
    //println("currentLineReadPos: " + currentLineReadPos)
  }

  import java.io.IOException

  @throws[IOException]
  override def close(): Unit = {
    if (in != null) {
      in.close
      in = null
    }
  }
}
