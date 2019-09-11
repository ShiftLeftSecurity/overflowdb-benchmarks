import Benchmarks.{loadGratefulDead, timed}
import io.shiftleft.overflowdb.traversals.testdomains.gratefuldead.GratefulDead
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.__
import org.apache.tinkerpop.gremlin.process.traversal.strategy.optimization.LazyBarrierStrategy
import org.apache.tinkerpop.gremlin.structure.{Direction, Graph, Vertex}

import scala.jdk.CollectionConverters._


/**
 * compare performance for V.out.out.out between tinkerpop3 and standard collections.
 *
 * timings on my machine:
 * tp3: 46.73538
 * tp3 without bulking: 428.08582
 * java while loop: 76.96792
 * java forEach: 109.39187
 * scala foreach: 245.60551
 * scala map: 513.7993
 * tp3 flatMap: 3194.6401
 * tp3 flatMap no bulking: 3182.7358
 * scala flatMap: 3131.0464
 *
 * interpretation:
 * 1) tp3 is only faster due to bulking - this is flawed to to using a miniscule graph. when disabling, that's gone
 * 2) standard collections are roughly twice as fast as tinkerpop
 * 3) map is more expensive than foreach
 * 4) flatMap is way more expensive than map
 */
object Tp3VsCollectionsPerformance extends App {
  benchmark(GratefulDead.newGraph)

  def benchmark(graph: Graph): Unit = {
    loadGratefulDead(graph)

    testSetups.foreach { test =>
      val millis = timed(test.iterations) { () =>
        val results = test.traversal(graph)
        val expectedResults = 14465066
        assert(results == expectedResults, s"expected $expectedResults results, but got $results")
      }
      println(s"${test.description}: $millis")
    }
    graph.close
  }

  case class TestSetup(description: String,
                       traversal: Graph => Long,
                       iterations: Int)

  lazy val testSetups = List(
    TestSetup(
      "warmup",
      _.traversal().V().out().out().out().toList.size,
      iterations = 100),

    TestSetup("tp3",
      _.traversal().V().out().out().out().toStream().count(),
      iterations = 200),

    TestSetup("tp3 without bulking",
      _.traversal().withoutStrategies(classOf[LazyBarrierStrategy]).V().out().out().out().toStream().count(),
      iterations = 20),

    TestSetup("java while loop",
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
      iterations = 200),

    TestSetup("java forEach",
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
      iterations = 200),

    TestSetup("scala foreach",
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
      iterations = 200),

    TestSetup("scala map",
      graph => {
        graph.vertices().asScala.map(
          _.vertices(Direction.OUT).asScala.map(
            _.vertices(Direction.OUT).asScala.map(
              _.vertices(Direction.OUT).asScala.map { _ =>
                1
              }
            )
          )
        ).flatten.flatten.flatten.sum
      },
      iterations = 200),

    TestSetup("tp3 flatMap",
      _.traversal.V().flatMap { trav =>
        __(trav.get).out().flatMap { trav =>
          __(trav.get).out().flatMap { trav =>
            __(trav.get).out()
          }
        }: java.util.Iterator[Vertex]
      }.count().next().toInt,
      iterations = 20),

    TestSetup("tp3 flatMap no bulking",
      _.traversal.withoutStrategies(classOf[LazyBarrierStrategy]).V().flatMap { trav =>
        __(trav.get).out().flatMap { trav =>
          __(trav.get).out().flatMap { trav =>
            __(trav.get).out()
          }
        }: java.util.Iterator[Vertex]
      }.count().next().toInt,
      iterations = 20),

    TestSetup("scala flatMap",
      _.vertices().asScala.flatMap(
        _.vertices(Direction.OUT).asScala.flatMap(
          _.vertices(Direction.OUT).asScala.flatMap(
            _.vertices(Direction.OUT).asScala
          )
        )
      ).size,
      iterations = 20),
  )
}
