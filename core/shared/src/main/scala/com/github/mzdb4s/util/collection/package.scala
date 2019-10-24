package com.github.mzdb4s.util

import scala.collection.TraversableLike
import scala.collection.mutable.LongMap
import scala.collection.generic.FilterMonadic
import scala.language.implicitConversions

/**
 * @author David Bouyssie
 *
 */
package object collection {
  
  trait LongMapBuilder extends Any {
    @inline final def initLongMap[A, V](xs: TraversableOnce[A]): LongMap[V] = {
      if (xs.isTraversableAgain) new LongMap[V](xs.size) else new LongMap[V]()
    }
  }
  
  trait LongMapBuilderFromTuplesOps extends Any with LongMapBuilder {
    @inline final def fillLongMap[V](xs: TraversableOnce[(Long, V)], longMap: LongMap[V]) = {
      longMap ++= xs
    }
    @inline final def toLongMap[V](xs: TraversableOnce[(Long, V)]): LongMap[V] = {
      val longMap = this.initLongMap[(Long, V), V](xs)
      this.fillLongMap(xs, longMap)
      longMap
    }
  }
  
  object LongMapBuilderFromTraversableTuples extends LongMapBuilderFromTuplesOps
  
  class LongMapBuilderFromTraversableTuples[A](val xs: TraversableOnce[(Long, A)]) extends AnyVal {
    def toLongMap(): LongMap[A] = LongMapBuilderFromTraversableTuples.toLongMap(xs)
  }
  implicit def traversableTuples2longMapBuilder[A]( xs: TraversableOnce[(Long, A)] ): LongMapBuilderFromTraversableTuples[A] = {
    new LongMapBuilderFromTraversableTuples[A](xs)
  }
  implicit def tuplesArray2longMapBuilder[A]( xs: Array[(Long, A)] ): LongMapBuilderFromTraversableTuples[A] = {
    new LongMapBuilderFromTraversableTuples[A](xs)
  }

  trait LongMapBuilderFromTraversableOnceOps extends Any with LongMapBuilder {
    
    @inline final def toLongMap[A, V](xs: TraversableOnce[A], kvMapping: A => (Long, V)): LongMap[V] = {
      val longMap = this.initLongMap[A, V](xs)

      val xsIter = xs.toIterator
      while (xsIter.hasNext) {
        longMap += kvMapping(xsIter.next())
      }

      longMap
    }
    @inline final def mapByLong[A](xs: TraversableOnce[A], byKey: A => Long): LongMap[A] = {
      this.toLongMap(xs, { x: A => (byKey(x), x) })
    }
  }
  
  object LongMapBuilderFromTraversableOnce extends LongMapBuilderFromTraversableOnceOps

  class LongMapBuilderFromTraversableOnce[A](val xs: TraversableOnce[A]) extends AnyVal {
    def mapByLong(byKey: A => Long): LongMap[A] = {
      LongMapBuilderFromTraversableOnce.mapByLong(xs, byKey)
    }
    
    def toLongMapWith[V](kvMapping: A => (Long, V)): LongMap[V] = {
      LongMapBuilderFromTraversableOnce.toLongMap(xs, kvMapping)
    }
  }
  // Note this conversion is conlicting filterMonadic2longMapBuilder
  // TODO: find a way to combine FilterMonadic with TraversableOnce (use TraversableLike ?)
  // DBO => I think that initializing using a match/case in FilterMonadic is simple enough, we can keep the current solution
  /*implicit def traversableOnce2longMapBuilder[A]( xs: TraversableOnce[A] ): LongMapBuilderFromTraversableOnce[A] = {
    new LongMapBuilderFromTraversableOnce[A](xs)
  }*/
  implicit def array2longMapBuilder[A]( xs: Array[A] ): LongMapBuilderFromTraversableOnce[A] = {
    new LongMapBuilderFromTraversableOnce[A](xs)
  }
  
  object LongMapBuilderFromFilterMonadic extends LongMapBuilder
  
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
  }

  trait LongMapGrouperFromTraversableLikeOps extends Any {
    @inline final def groupByLong[A, Repr](xs: TraversableLike[A, Repr], byLong: A => Long): LongMap[Repr] = {

      val tmpMap = xs.groupBy(byLong(_))

      val longMap = new LongMap[Repr](tmpMap.size)
      longMap ++= tmpMap

      longMap
    }
  }
  
  object LongMapGrouperFromTraversableLike extends LongMapGrouperFromTraversableLikeOps

  class LongMapGrouperFromTraversableLike[A, Repr](val xs: TraversableLike[A, Repr]) extends AnyVal {
    def groupByLong(byLong: A => Long): LongMap[Repr] = {
      LongMapGrouperFromTraversableLike.groupByLong(xs, byLong)
    }
  }
  implicit def traversableOnce2longMapGrouper[A,Repr]( xs: TraversableLike[A, Repr] ): LongMapGrouperFromTraversableLike[A, Repr] = {
    new LongMapGrouperFromTraversableLike[A,Repr](xs)
  }
  implicit def array2longMapGrouper[A]( xs: Array[A] ): LongMapGrouperFromTraversableLike[A, Array[A]] = {
    new LongMapGrouperFromTraversableLike[A, Array[A]](xs)
  }
  
}