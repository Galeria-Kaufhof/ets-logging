organization := "de.kaufhof.ets.logging"

name := "ets-logging-apisandbox"

scalaVersion := "2.12.6"

scalacOptions ++= Seq(
  "-target:jvm-1.8",
  "-Xmax-classfile-name", "120",
  "-feature",
  "-deprecation",
  "-language:postfixOps",
  "-Xlint",
  "-Ywarn-inaccessible",
  "-Ywarn-infer-any",
  "-Ywarn-nullary-override",
  "-Ywarn-nullary-unit",
  "-Ywarn-value-discard",
  "-unchecked",
  "-Xsource:2.12"
)

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/releases"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "ch.qos.logback" % "logback-core" % "1.2.3",
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "org.typelevel" %% "cats-effect" % "1.0.0",
  "io.circe" %% "circe-core" % "0.9.3",
  "io.circe" %% "circe-generic" % "0.9.3",
  "io.circe" %% "circe-parser" % "0.9.3",
  "com.typesafe.play" %% "play-json" % "2.6.7",
  "com.typesafe.akka" %% "akka-actor" % "2.5.13",
  "net.logstash.logback" % "logstash-logback-encoder" % "4.11",
  "com.storm-enroute" %% "scalameter-core" % "0.8.2"
)
