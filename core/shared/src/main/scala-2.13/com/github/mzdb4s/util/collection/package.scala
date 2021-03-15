package com.github.mzdb4s.util

import scala.collection.Iterable
import scala.collection.IterableOnce
//import scala.collection.generic.FilterMonadic
import scala.collection.mutable.LongMap
import scala.language.implicitConversions

/**
 * @author David Bouyssie
 *
 */
package object collection {
  
  trait LongMapBuilder extends Any {
    @inline final def initLongMap[A, V](xs: Iterable[A]): LongMap[V] = {
      if (xs.isTraversableAgain) new LongMap[V](xs.size) else new LongMap[V]()
    }
  }
  
  trait LongMapBuilderFromTuplesOps extends Any with LongMapBuilder {
    @inline final def fillLongMap[V](xs: Iterable[(Long, V)], longMap: LongMap[V]) = {
      longMap ++= xs
    }
    @inline final def toLongMap[V](xs: Iterable[(Long, V)]): LongMap[V] = {
      val longMap = this.initLongMap[(Long, V), V](xs)
      this.fillLongMap(xs, longMap)
      longMap
    }
  }
  
  object LongMapBuilderFromTraversableTuples extends LongMapBuilderFromTuplesOps
  
  class LongMapBuilderFromTraversableTuples[A](val xs: Iterable[(Long, A)]) extends AnyVal {
    def toLongMap(): LongMap[A] = LongMapBuilderFromTraversableTuples.toLongMap(xs)
  }
  implicit def traversableTuples2longMapBuilder[A]( xs: Iterable[(Long, A)] ): LongMapBuilderFromTraversableTuples[A] = {
    new LongMapBuilderFromTraversableTuples[A](xs)
  }
  implicit def tuplesArray2longMapBuilder[A]( xs: Array[(Long, A)] ): LongMapBuilderFromTraversableTuples[A] = {
    new LongMapBuilderFromTraversableTuples[A](xs)
  }

  trait LongMapBuilderFromIterableOnceOps extends Any with LongMapBuilder {
    
    @inline final def toLongMap[A, V](xs: IterableOnce[A], kvMapping: A => (Long, V)): LongMap[V] = {
      val longMap = new LongMap[V]()

      val xsIter = xs.toIterator
      while (xsIter.hasNext) {
        longMap += kvMapping(xsIter.next())
      }

      longMap
    }
    @inline final def mapByLong[A](xs: IterableOnce[A], byKey: A => Long): LongMap[A] = {
      this.toLongMap(xs, { x: A => (byKey(x), x) })
    }
  }
  
  object LongMapBuilderFromIterableOnce extends LongMapBuilderFromIterableOnceOps

  class LongMapBuilderFromIterableOnce[A](val xs: IterableOnce[A]) extends AnyVal {
    def mapByLong(byKey: A => Long): LongMap[A] = {
      LongMapBuilderFromIterableOnce.mapByLong(xs, byKey)
    }
    
    def toLongMapWith[V](kvMapping: A => (Long, V)): LongMap[V] = {
      LongMapBuilderFromIterableOnce.toLongMap(xs, kvMapping)
    }
  }
  // Note this conversion is conlicting filterMonadic2longMapBuilder
  // TODO: find a way to combine FilterMonadic with TraversableOnce (use TraversableLike ?)
  // DBO => I think that initializing using a match/case in FilterMonadic is simple enough, we can keep the current solution
  implicit def iterableOnce2longMapBuilder[A]( xs: IterableOnce[A] ): LongMapBuilderFromIterableOnce[A] = {
    new LongMapBuilderFromIterableOnce[A](xs)
  }
  implicit def array2longMapBuilder[A]( xs: Array[A] ): LongMapBuilderFromIterableOnce[A] = {
    new LongMapBuilderFromIterableOnce[A](xs)
  }
  
  /*object LongMapBuilderFromFilterMonadic extends LongMapBuilder
  
  class LongMapBuilderFromFilterMonadic[A, Repr](val fm: FilterMonadic[A, Repr]) extends AnyVal {
    
    protected def buildLongMapFromLongMapping[V](longMapping: A => (Long, V)): LongMap[V] = {
      
      // Initialize the map
      val longMap = fm match {
        case xs: TraversableOnce[A] => LongMapBuilderFromFilterMonadic.initLongMap[A, V](xs)
        case _ => new LongMap[V]
      }

      // Fill the map
      fm.foreach { x =>
        longMap += longMapping(x)
      }

      longMap
    }
    
    def mapByLong(byKey: A => Long): LongMap[A] = {
      this.buildLongMapFromLongMapping({ x: A => (byKey(x), x) })
    }
    
    def toLongMapWith[V](kvMapping: A => (Long, V)): LongMap[V] = {
      this.buildLongMapFromLongMapping(kvMapping)
    }
  }
  
  implicit def filterMonadic2longMapBuilder[A, Repr]( fm: FilterMonadic[A, Repr] ): LongMapBuilderFromFilterMonadic[A, Repr] = {
    new LongMapBuilderFromFilterMonadic[A, Repr](fm)
  }*/

  trait LongMapGrouperFromIterableOps extends Any {
    @inline final def groupByLong[A](xs: Iterable[A], byLong: A => Long): LongMap[Iterable[A]] = {

      val tmpMap = xs.groupBy(byLong(_))

      val longMap = new LongMap[Iterable[A]](tmpMap.size)
      longMap ++= tmpMap

      longMap
    }
  }
  
  object LongMapGrouperFromIterable extends LongMapGrouperFromIterableOps

  class LongMapGrouperFromIterable[A](val xs: Iterable[A]) extends AnyVal {
    def groupByLong(byLong: A => Long): LongMap[Iterable[A]] = {
      LongMapGrouperFromIterable.groupByLong(xs, byLong)
    }
  }
  implicit def iterable2longMapGrouper[A]( xs: Iterable[A] ): LongMapGrouperFromIterable[A] = {
    new LongMapGrouperFromIterable[A](xs)
  }
  /*implicit def array2longMapGrouper[A]( xs: Array[A] ): LongMapGrouperFromTraversableLike[A, Array[A]] = {
    new LongMapGrouperFromTraversableLike[A, Array[A]](xs)
  }*/
  
}

// FIXME: merge with this version from the Scala Center
/*
package fr.profi.util

import scala.collection.mutable.LongMap
import scala.collection.{IterableOps, WithFilter, mutable}
import scala.language.implicitConversions

/**
 * @author David Bouyssie
 *
 */
package object collection {

  trait LongMapBuilder extends Any {
    @inline final def initLongMap[A, V](xs: IterableOnce[A]): LongMap[V] = {
      xs.knownSize match {
        case -1 => new LongMap[V]()
        case size => new LongMap[V](size)
      }
    }
  }

  trait LongMapBuilderFromTuplesOps extends Any with LongMapBuilder {
    @inline final def fillLongMap[V](xs: IterableOnce[(Long, V)], longMap: LongMap[V]) = {
      longMap ++= xs
    }
    @inline final def toLongMap[V](xs: IterableOnce[(Long, V)]): LongMap[V] = {
      val longMap = this.initLongMap[(Long, V), V](xs)
      this.fillLongMap(xs, longMap)
      longMap
    }
  }

  object LongMapBuilderFromTraversableTuples extends LongMapBuilderFromTuplesOps

  class LongMapBuilderFromTraversableTuples[A](val xs: IterableOnce[(Long, A)]) extends AnyVal {
    def toLongMap(): LongMap[A] = LongMapBuilderFromTraversableTuples.toLongMap(xs)
  }
  implicit def traversableTuples2longMapBuilder[A]( xs: IterableOnce[(Long, A)] ): LongMapBuilderFromTraversableTuples[A] = {
    new LongMapBuilderFromTraversableTuples[A](xs)
  }
  implicit def tuplesArray2longMapBuilder[A]( xs: Array[(Long, A)] ): LongMapBuilderFromTraversableTuples[A] = {
    new LongMapBuilderFromTraversableTuples[A](xs)
  }

  trait LongMapBuilderFromIterableOnceOps extends Any with LongMapBuilder {

    @inline final def toLongMap[A, V](xs: IterableOnce[A], kvMapping: A => (Long, V)): LongMap[V] = {
      val longMap = this.initLongMap[A, V](xs)

      val xsIter = xs.toIterator
      while (xsIter.hasNext) {
        longMap += kvMapping(xsIter.next())
      }

      longMap
    }
    @inline final def mapByLong[A](xs: IterableOnce[A], byKey: A => Long): LongMap[A] = {
      this.toLongMap(xs, { x: A => (byKey(x), x) })
    }
  }

  object LongMapBuilderFromIterableOnce extends LongMapBuilderFromIterableOnceOps

  class LongMapBuilderFromIterableOnce[A](val xs: IterableOnce[A]) extends AnyVal {
    def mapByLong(byKey: A => Long): LongMap[A] = {
      LongMapBuilderFromIterableOnce.mapByLong(xs, byKey)
    }

    def toLongMapWith[V](kvMapping: A => (Long, V)): LongMap[V] = {
      LongMapBuilderFromIterableOnce.toLongMap(xs, kvMapping)
    }
  }
  // Note this conversion is conlicting filterMonadic2longMapBuilder
  // TODO: find a way to combine FilterMonadic with IterableOnce (use TraversableLike ?)
  // DBO => I think that initializing using a match/case in FilterMonadic is simple enough, we can keep the current solution
  /*implicit def IterableOnce2longMapBuilder[A]( xs: IterableOnce[A] ): LongMapBuilderFromIterableOnce[A] = {
    new LongMapBuilderFromIterableOnce[A](xs)
  }*/
  implicit def array2longMapBuilder[A]( xs: Array[A] ): LongMapBuilderFromIterableOnce[A] = {
    new LongMapBuilderFromIterableOnce[A](xs)
  }

  object LongMapBuilderFromFilterMonadic extends LongMapBuilder

  class LongMapBuilderFromFilterMonadic[A, Repr[_]](val fm: WithFilter[A, Repr]) extends AnyVal {

    protected def buildLongMapFromLongMapping[V](longMapping: A => (Long, V)): LongMap[V] = {

      // Initialize the map
      val longMap = fm match {
        case xs: IterableOnce[A] => LongMapBuilderFromFilterMonadic.initLongMap[A, V](xs)
        case _ => new LongMap[V]
      }

      // Fill the map
      fm.foreach { x =>
        longMap += longMapping(x)
      }

      longMap
    }

    def mapByLong(byKey: A => Long): LongMap[A] = {
      this.buildLongMapFromLongMapping({ x: A => (byKey(x), x) })
    }

    def toLongMapWith[V](kvMapping: A => (Long, V)): LongMap[V] = {
      this.buildLongMapFromLongMapping(kvMapping)
    }
  }

  implicit def filterMonadic2longMapBuilder[A, Repr[_]]( fm: WithFilter[A, Repr] ): LongMapBuilderFromFilterMonadic[A, Repr] = {
    new LongMapBuilderFromFilterMonadic[A, Repr](fm)
  }

  trait LongMapGrouperFromTraversableLikeOps extends Any {
    @inline final def groupByLong[A, Repr](xs: IterableOps[A, IterableOnce, Repr], byLong: A => Long): LongMap[Repr] = {

      val tmpMap = xs.groupBy(byLong(_))

      val longMap = new LongMap[Repr](tmpMap.size)
      longMap ++= tmpMap

      longMap
    }
  }

  object LongMapGrouperFromTraversableLike extends LongMapGrouperFromTraversableLikeOps

  class LongMapGrouperFromTraversableLike[A, Repr](val xs: IterableOps[A, IterableOnce, Repr]) extends AnyVal {
    def groupByLong(byLong: A => Long): LongMap[Repr] = {
      LongMapGrouperFromTraversableLike.groupByLong(xs, byLong)
    }
  }
  implicit def IterableOnce2longMapGrouper[A,Repr]( xs: IterableOps[A,Iterable, Repr] ): LongMapGrouperFromTraversableLike[A, Repr] = {
    new LongMapGrouperFromTraversableLike[A,Repr](xs)
  }
  implicit def array2longMapGrouper[A]( xs: Array[A] ): LongMapGrouperFromTraversableLike[A, mutable.ArraySeq[A]] = {
    new LongMapGrouperFromTraversableLike(xs)
  }

}
 */