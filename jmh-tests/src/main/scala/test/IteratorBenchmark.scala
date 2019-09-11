package test

import org.openjdk.jmh.annotations.Benchmark
import scala.jdk.CollectionConverters._

class IteratorBenchmark {
  def makeJavaIter = new java.util.Iterator[Int] {
    var i = 0
    override def hasNext = i < 10_000
    override def next = {
      i += 1
      i
    }
  }

  @Benchmark
  def javaIteratorWhile(): Int = {
    val iter = makeJavaIter
    var lastElement = 0
    while (iter.hasNext)
      lastElement = iter.next
    lastElement
  }

  @Benchmark
  def javaIteratorForeach(): Int = {
    val iter = makeJavaIter
    var lastElement = 0
    iter.forEachRemaining { i =>
      lastElement = i
    }
    lastElement
  }

  @Benchmark
  def scalaIteratorForeach(): Int = {
    val iter = makeJavaIter.asScala
    var lastElement = 0
    iter.foreach { i =>
      lastElement = i
    }
    lastElement
  }

}
