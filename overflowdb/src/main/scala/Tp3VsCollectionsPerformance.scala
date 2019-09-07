import Benchmarks.{loadGratefulDead, timed}
import io.shiftleft.overflowdb.traversals.testdomains.gratefuldead.GratefulDead
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.__
import org.apache.tinkerpop.gremlin.structure.{Direction, Graph, Vertex}

import scala.jdk.CollectionConverters._


/**
 * compare performance of tinkerpop3 and standard collections.
 *
 * // TODO compare repeat
 *
 * timings on my machine:
 * tp3: V.out.out: 3.608683
 * tp3: V.out.out.out: 45.1047
 * java while loop: V.out.out: 2.8279471
 * java while loop: V.out.out.out: 80.289375
 * java forEach: V.out.out: 3.236158
 * java forEach: V.out.out.out: 212.36609
 * scala foreach: V.out.out: 3.604994
 * scala foreach: V.out.out.out: 224.15091
 * scala map, flatten at the end: V.out.out: 14.646321
 * scala map.flatten every step: V.out.out: 31.522526
 * tp3 flatMap: V.out.out: 63.30443
 * tp3 flatMap: V.out.out.out: 3093.839
 * scala flatMap: V.out.out: 31.73666
 * scala flatMap: V.out.out.out: 3123.2524
 *
 * interpretation:
 * 1) when doing two hops, collections are on par with tp. from 3 hops onwards (without looking at intermediate results) tp3 is faster
 * 2) flatMap is always more expensive, but standard collections are roughly twice as fast as tinkerpop
 * 3) map is more expensive than foreach
 */
object Tp3VsCollectionsPerformance extends App {
  benchmark(GratefulDead.newGraph)

  def benchmark(graph: Graph): Unit = {
    loadGratefulDead(graph)

    testSetups.foreach { test =>
      val millis = timed(test.iterations) { () =>
        val results = test.traversal(graph)
        assert(results == test.expectedResults, s"expected ${test.expectedResults} results, but got $results")
      }
      println(s"${test.description}: $millis")
    }
    graph.close
  }

  case class TestSetup(description: String,
                       traversal: Graph => Long,
                       expectedResults: Long,
                       iterations: Int)

  lazy val testSetups = List(
    TestSetup(
      "warmup",
      _.traversal().V().out().out().toList.size,
      expectedResults = 327370,
      iterations = 100),

    TestSetup("tp3: V.out.out",
      _.traversal().V().out().out().toStream().count(),
      expectedResults = 327370,
      iterations = 100),

    TestSetup("tp3: V.out.out.out",
      _.traversal().V().out().out().out().toStream().count(),
      expectedResults = 14465066,
      iterations = 100),

    TestSetup("java while loop: V.out.out",
      graph => {
        var results = 0
        val iter0 = graph.vertices()
        while (iter0.hasNext) {
          val iter1 = iter0.next().vertices(Direction.OUT)
          while (iter1.hasNext) {
            val iter2 = iter1.next().vertices(Direction.OUT)
            while (iter2.hasNext) {
              iter2.next()
              results += 1
            }
          }
        }
        results
      },
      expectedResults = 327370,
      iterations = 100),

    TestSetup("java while loop: V.out.out.out",
      graph => {
        var results = 0
        val iter0 = graph.vertices()
        while (iter0.hasNext) {
          val iter1 = iter0.next().vertices(Direction.OUT)
          while (iter1.hasNext) {
            val iter2 = iter1.next().vertices(Direction.OUT)
            while (iter2.hasNext) {
              val iter3 = iter2.next().vertices(Direction.OUT)
              while (iter3.hasNext) {
                iter3.next()
                results += 1
              }
            }
          }
        }
        results
      },
      expectedResults = 14465066,
      iterations = 100),

    TestSetup("java forEach: V.out.out",
      graph => {
        var results = 0
        graph.vertices().forEachRemaining(
          _.vertices(Direction.OUT).forEachRemaining(
            _.vertices(Direction.OUT).forEachRemaining { _ =>
              results += 1
            }
          )
        )
        results
      },
      expectedResults = 327370,
      iterations = 100),

    TestSetup("java forEach: V.out.out.out",
      graph => {
        var results = 0
        graph.vertices().forEachRemaining(
          _.vertices(Direction.OUT).forEachRemaining(
            _.vertices(Direction.OUT).forEachRemaining(
              _.vertices(Direction.OUT).forEachRemaining { _ =>
                results += 1
              }
            )
          )
        )
        results
      },
      expectedResults = 14465066,
      iterations = 100),

    TestSetup("scala foreach: V.out.out",
      graph => {
        var results = 0
        graph.vertices().asScala.foreach(
          _.vertices(Direction.OUT).asScala.foreach(
            _.vertices(Direction.OUT).asScala.foreach { _ =>
              results += 1
            }
          )
        )
        results
      },
      expectedResults = 327370,
      iterations = 100),

    TestSetup("scala foreach: V.out.out.out",
      graph => {
        var results = 0
        graph.vertices().asScala.foreach(
          _.vertices(Direction.OUT).asScala.foreach(
            _.vertices(Direction.OUT).asScala.foreach(
              _.vertices(Direction.OUT).asScala.foreach { _ =>
                results += 1
              }
            )
          )
        )
        results
      },
      expectedResults = 14465066,
      iterations = 100),

    TestSetup("scala map, flatten at the end: V.out.out",
      _.vertices().asScala.map { vertex =>
        vertex.vertices(Direction.OUT).asScala.map { vertex =>
          vertex.vertices(Direction.OUT).asScala.map { vertex =>
            1
          }
        }
      }.flatten.flatten.sum,
      expectedResults = 327370,
      iterations = 100),

    TestSetup("scala map.flatten every step: V.out.out",
      _.vertices().asScala.map { vertex =>
        vertex.vertices(Direction.OUT).asScala.map { vertex =>
          vertex.vertices(Direction.OUT).asScala.toSeq.map { vertex =>
            1
          }
        }.flatten
      }.flatten.sum,
      expectedResults = 327370,
      iterations = 100),

    TestSetup("tp3 flatMap: V.out.out",
      _.traversal.V().flatMap { trav =>
        __(trav.get).out().flatMap { trav =>
          __(trav.get).out()
        }: java.util.Iterator[Vertex]
      }.count().next().toInt,
      expectedResults = 327370,
      iterations = 100),

    TestSetup("tp3 flatMap: V.out.out.out",
      _.traversal.V().flatMap { trav =>
        __(trav.get).out().flatMap { trav =>
          __(trav.get).out().flatMap { trav =>
            __(trav.get).out()
          }
        }: java.util.Iterator[Vertex]
      }.count().next().toInt,
      expectedResults = 14465066,
      iterations = 10),

    TestSetup("scala flatMap: V.out.out",
      _.vertices().asScala.flatMap(
        _.vertices(Direction.OUT).asScala.flatMap(
          _.vertices(Direction.OUT).asScala
        )
      ).size,
      expectedResults = 327370,
      iterations = 100),

    TestSetup("scala flatMap: V.out.out.out",
      _.vertices().asScala.flatMap(
        _.vertices(Direction.OUT).asScala.flatMap(
          _.vertices(Direction.OUT).asScala.flatMap(
            _.vertices(Direction.OUT).asScala
          )
        )
      ).size,
      expectedResults = 14465066,
      iterations = 10),
  )
}
