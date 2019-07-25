package com.github.sqlite4s

object SQLiteFactoryRegistry {
  private var _myFactory: Option[ISQLiteFactory] = None

  def setFactory(factory: ISQLiteFactory): Unit =  {
    _myFactory = Some(factory)
  }

  def getFactory(): Option[ISQLiteFactory] = _myFactory
}
