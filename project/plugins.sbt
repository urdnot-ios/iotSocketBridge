import sbt.Keys.resolvers

resolvers ++= Seq(
  "Bintray Repository" at "https://dl.bintray.com/shmishleniy/"
)
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.2")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.6")
addSbtPlugin("com.lightbend.rp" % "sbt-reactive-app" % "1.2.1")
