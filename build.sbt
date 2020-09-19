import com.typesafe.sbt.packager.docker._
import sbt.Keys.mappings

organization := "com.urdnot.iot"

name := "iotSocketBridge"

// Docker image name:
packageName := s"${name.value.toLowerCase}"

version := "0.2.1"

val scalaMajorVersion = "2.13"
val scalaMinorVersion = "2"

scalaVersion := scalaMajorVersion.concat("." + scalaMinorVersion)

libraryDependencies ++= {
  val akkaVersion = "2.5.30"
  val akkaStreamKafkaVersion = "2.0.4"
  val logbackClassicVersion = "1.2.3"
  val scalatestVersion = "3.1.1"
  val scalaLoggingVersion = "3.9.2"
  Seq(
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "org.scalatest" %% "scalatest" % scalatestVersion % Test,
    "ch.qos.logback" % "logback-classic" % logbackClassicVersion,
    "com.typesafe.akka" %% "akka-stream-kafka" % akkaStreamKafkaVersion,
    "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion
  )
}


enablePlugins(DockerPlugin)

mainClass := Some(s"${organization.value}.SocketKafkaBridge")
mainClass in (Compile, assembly) := Some(s"${mainClass.value}")

assemblyJarName := s"${name.value}.v${version.value}.jar"
val meta = """META.INF(.)*""".r

mappings in(Compile, packageBin) ~= {
  _.filterNot {
    case (_, name) => Seq("application.conf").contains(name)
  }
}
assemblyMergeStrategy in assembly := {
  case n if n.endsWith(".properties") => MergeStrategy.concat
  case PathList("reference.conf") => MergeStrategy.concat
  case PathList("resources/application.conf") => MergeStrategy.discard
  case meta(_) => MergeStrategy.discard
  case x => MergeStrategy.first
}


dockerBuildOptions += "--no-cache"
dockerUpdateLatest := true
dockerPackageMappings in Docker += file(s"target/scala-2.13/${assemblyJarName.value}") -> s"opt/docker/${assemblyJarName.value}"
mappings in Docker += file("src/main/resources/application.conf") -> "opt/docker/application.conf"
mappings in Docker += file("src/main/resources/logback.xml") -> "opt/docker/logback.xml"

dockerCommands := Seq(
  Cmd("FROM", "openjdk:11-jdk-slim"),
  Cmd("LABEL", s"""MAINTAINER="Jeffrey Sewell""""),
  Cmd("COPY", s"opt/docker/${assemblyJarName.value}", s"/opt/docker/${assemblyJarName.value}"),
  Cmd("COPY", "opt/docker/application.conf", "/var/application.conf"),
  Cmd("COPY", "opt/docker/logback.xml", "/var/logback.xml"),
  Cmd("ENV", "CLASSPATH=/opt/docker/application.conf:/opt/docker/logback.xml"),
  Cmd("ENTRYPOINT", s"java -cp /opt/docker/${assemblyJarName.value} ${mainClass.value.get}")
)