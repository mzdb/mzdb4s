package com.github.mzdb4s.util.atomic

import java.util.concurrent.atomic.AtomicInteger

trait InMemoryIdGen {
  private val _idSequence = new AtomicInteger(0)

  def generateNewId(): Int = _idSequence.incrementAndGet
}