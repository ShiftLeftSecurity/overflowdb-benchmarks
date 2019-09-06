name := "overflowdb"

lazy val odbVersion = "c0699eb093e76b17d980790271c9176b04a5313b"

libraryDependencies ++= Seq(
  "io.shiftleft" % "overflowdb-tinkerpop3" % odbVersion,
  "io.shiftleft" %% "overflowdb-traversals" % odbVersion
)

enablePlugins(JavaAppPackaging)
