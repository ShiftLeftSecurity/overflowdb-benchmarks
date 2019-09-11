import Benchmarks.timed
import io.shiftleft.overflowdb.traversals.testdomains.gratefuldead.GratefulDead

object VsOperationsTraversal extends App {
  val graph = GratefulDead.newGraphWithData
  def songs = GratefulDead.traversal(graph).songs
  // warmup
  assert(songs.followedBy.followedBy.size == 314932)

  println("warmup complete, connect now")
  Thread.sleep(15000)
  println("starting")

  val millis = timed(100) { () =>
    assert(songs.followedBy.followedBy.followedBy.size == 13907852)
  }
  println(millis)
  graph.close
}
