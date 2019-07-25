/*package fr.profi.mzdb.util.iterator

import java.util.LinkedList
import java.util.Iterator

// FIXME: DBO => the previous implementation was spawning a private thread, this was removed for SN compatibility
// TODO: DBO => re-implement this class in a single thread mode but with internal buffer
class BufferedIterator[E <: AnyRef](private var source: Iterator[E], private var max: Int) extends Iterator[E] {

  private val queue = new LinkedList[E]
  private var nextReturn: E = _
  private var done = new Object()

  /* def run(): Unit = {
     new Thread(("BufferedIterator Filler")) {
        while (source.hasNext) {
          val next = source.next
          //queue synchronized {}
          while (queue.size >= max) {
            try
              queue.wait()
            catch {
              case doh: InterruptedException =>
                doh.printStackTrace()
                throw doh
            }
          }

          queue.add(next)
          queue.notify

        }
        queue synchronized queue.add(done)
        queue.notify

      }
    }.start*/

  def hasNext: Boolean = {
    source.hasNext
    /*while ( {nextReturn == null}) {
      queue synchronized
      while ( {queue.isEmpty}) try
        queue.wait
      catch {
        case doh: Nothing =>
          doh.printStackTrace
          return false
      }
      nextReturn = queue.removeFirst.asInstanceOf[E]
      queue.notify
      if (nextReturn eq done) return false

    }
    true*/
  }

  def next(): E = {
    /*if (!hasNext) throw new NoSuchElementException()
    val retVal = nextReturn
    nextReturn = null
    retVal*/
    source.next()
  }

  override def remove(): Unit = {
    throw new UnsupportedOperationException("Unsupported operation.")
  }
}*/