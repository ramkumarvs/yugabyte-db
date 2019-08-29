name := """yugaware"""

lazy val root = (project in file("."))
  .enablePlugins(PlayJava, PlayEbean, SbtWeb, JavaAppPackaging)
  .disablePlugins(PlayLayoutPlugin)

scalaVersion := "2.11.7"
version := (sys.process.Process("cat version.txt").lines_!.head)

libraryDependencies ++= Seq(
  javaJdbc,
  cache,
  javaWs,
  filters,
  "org.mockito" % "mockito-core" % "1.10.19",
  "org.mindrot" % "jbcrypt" % "0.3m",
  "org.postgresql" % "postgresql" % "9.4.1208",
  "commons-io" % "commons-io" % "2.4",
  "org.apache.httpcomponents" % "httpcore" % "4.4.5",
  "org.apache.httpcomponents" % "httpclient" % "4.5.2",
  "org.flywaydb" %% "flyway-play" % "3.0.1",
  // https://github.com/YugaByte/cassandra-java-driver/releases
  "com.yugabyte" % "cassandra-driver-core" % "3.2.0-yb-19",
  "org.yaml" % "snakeyaml" % "1.17",
  "com.google.protobuf" % "protobuf-java" % "2.6.1",
  "org.bouncycastle" % "bcpkix-jdk15on" % "1.61"
)
// Default to true if nothing passed on the env, so we can pick up YB jars from local java itest.
lazy val mavenLocal = Option(System.getenv("USE_MAVEN_LOCAL")).getOrElse("false")
resolvers += {
  if (mavenLocal != null && mavenLocal.equals("true")) {
    Resolver.mavenLocal
  } else {
    "Yugabyte Nexus Snapshots" at System.getenv("YB_NEXUS_SNAPSHOT_URL")
  }
}

libraryDependencies += "org.yb" % "yb-client" % "0.8.0-SNAPSHOT"

dependencyOverrides += "io.netty" % "netty-handler" % "4.0.36.Final"

javaOptions in Test += "-Dconfig.file=src/main/resources/application.test.conf"
testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-q", "-a")

// Skip packaging javadoc for now
sources in (Compile, doc) := Seq()
publishArtifact in (Compile, packageDoc) := false

topLevelDirectory := None
