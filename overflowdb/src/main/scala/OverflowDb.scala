import io.shiftleft.overflowdb.traversals.testdomains.gratefuldead.GratefulDead

object OverflowDbTinkerpop3 extends App {
  Benchmarks.Tinkerpop3.benchmark(GratefulDead.newGraph)
}

