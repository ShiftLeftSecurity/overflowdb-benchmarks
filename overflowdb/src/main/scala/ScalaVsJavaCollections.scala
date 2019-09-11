import io.shiftleft.overflowdb.traversals.testdomains.gratefuldead.GratefulDead
import org.apache.tinkerpop.gremlin.structure.Direction
import scala.jdk.CollectionConverters._

object ScalaVsJavaCollections extends App {
  val graph = GratefulDead.newGraphWithData
  graph.traversal().V().out().out().out().toList.size
  val expectedResults = 14465066

  println("connect now, starting in 15s")
  Thread.sleep(15000)
  println("starting benchmark")
  println(timed(200) { () =>
    // java foreach: 210ms
//    var results = 0
//    graph.vertices().forEachRemaining(
//      _.vertices(Direction.OUT).forEachRemaining(
//        _.vertices(Direction.OUT).forEachRemaining(
//          _.vertices(Direction.OUT).forEachRemaining { _ =>
//            results += 1
//          }
//        )
//      )
//    )

    // scala foreach: 183ms
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
    assert(results == expectedResults, s"expected $expectedResults results, but got $results")
  })
  println("done")

  /* returns the average time in millis */
  def timed(iterations: Int)(fun: () => Unit): Float = {
    val start = System.nanoTime
    1.to(iterations).foreach { _ => fun() }
    val average = (System.nanoTime - start) / iterations.toFloat / 1_000_000f
    average
  }
}
