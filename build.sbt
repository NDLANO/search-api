import java.util.Properties
import sbt._
import Keys._

val Scalaversion = "2.12.6"
val Scalatraversion = "2.5.1"
val ScalaLoggingVersion = "3.5.0"
val Log4JVersion = "2.11.0"
val Jettyversion = "9.4.11.v20180605"
val AwsSdkversion = "1.11.297"
val ScalaTestVersion = "3.0.1"
val MockitoVersion = "1.10.19"
val JacksonVersion = "2.9.5"
val JsoupVersion = "1.10.2"
val Elastic4sVersion = "6.1.4"
val ElasticsearchVersion = "6.0.1"

val appProperties = settingKey[Properties]("The application properties")

appProperties := {
  val prop = new Properties()
  IO.load(prop, new File("build.properties"))
  prop
}

lazy val commonSettings = Seq(
  organization := appProperties.value.getProperty("NDLAOrganization"),
  version := appProperties.value.getProperty("NDLAComponentVersion"),
  scalaVersion := Scalaversion
)

lazy val search_api = (project in file("."))
  .settings(commonSettings: _*)
  .settings(
    name := "search-api",
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    scalacOptions := Seq("-target:jvm-1.8", "-unchecked", "-deprecation", "-feature"),
    libraryDependencies ++= Seq(
      "ndla" %% "network" % "0.30",
      "ndla" %% "mapping" % "0.7",
      "com.typesafe.scala-logging" %% "scala-logging" % ScalaLoggingVersion,
      "org.apache.logging.log4j" % "log4j-api" % Log4JVersion,
      "org.apache.logging.log4j" % "log4j-core" % Log4JVersion,
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % Log4JVersion,
      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % JacksonVersion,
      "joda-time" % "joda-time" % "2.8.2",
      "org.scalatra" %% "scalatra" % Scalatraversion,
      "org.scalatra" %% "scalatra-json" % Scalatraversion,
      "org.scalatra" %% "scalatra-swagger" % Scalatraversion,
      "org.scalatra" %% "scalatra-scalatest" % Scalatraversion % "test",
      "org.jsoup" % "jsoup" % "1.11.2",
      "org.elasticsearch" % "elasticsearch" % ElasticsearchVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-core" % Elastic4sVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-http" % Elastic4sVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-aws" % Elastic4sVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-embedded" % Elastic4sVersion % "test",
      "com.fasterxml.jackson.core" % "jackson-databind" % JacksonVersion, // Overriding jackson-databind used in elastic4s because of https://snyk.io/vuln/SNYK-JAVA-COMFASTERXMLJACKSONCORE-32111
      "org.apache.lucene" % "lucene-queryparser" % "7.1.0", // Overriding lucene-queryparser used in elasticsearch because of https://snyk.io/vuln/SNYK-JAVA-ORGAPACHELUCENE-31569
      "org.eclipse.jetty" % "jetty-webapp" % Jettyversion % "container;compile",
      "org.eclipse.jetty" % "jetty-plus" % Jettyversion % "container",
      "org.json4s" %% "json4s-native" % "3.5.0",
      "org.json4s" %% "json4s-ext" % "3.5.0",
      "log4j" % "log4j" % "1.2.16",
      "net.bull.javamelody" % "javamelody-core" % "1.73.1",
      "org.jrobin" % "jrobin" % "1.5.9",
      "com.amazonaws" % "aws-java-sdk-cloudwatch" % AwsSdkversion,
      "com.netaporter" %% "scala-uri" % "0.4.16",
      "org.scalatest" %% "scalatest" % ScalaTestVersion % "test",
      "org.mockito" % "mockito-all" % MockitoVersion % "test"
    )
  )
  .enablePlugins(DockerPlugin)
  .enablePlugins(JettyPlugin)

assembly / assemblyJarName := "search-api.jar"
assembly / mainClass := Some("no.ndla.searchapi.JettyLauncher")

val checkfmt = taskKey[Boolean]("check for code style errors")
checkfmt := {
  val noErrorsInMainFiles = (Compile / scalafmtCheck).value
  val noErrorsInTestFiles = (Test / scalafmtCheck).value
  val noErrorsInBuildFiles = (Compile / scalafmtSbtCheck).value

  noErrorsInMainFiles && noErrorsInTestFiles && noErrorsInBuildFiles
}

Test / test := (Test / test).dependsOn(Test / checkfmt).value

val fmt = taskKey[Unit]("Automatically apply code style fixes")
fmt := {
  (Compile / scalafmt).value
  (Test / scalafmt).value
  (Compile / scalafmtSbt).value
}

// Make the docker task depend on the assembly task, which generates a fat JAR file
docker := (docker dependsOn assembly).value

docker / dockerfile := {
  val artifact = (assembly / assemblyOutputPath).value
  val artifactTargetPath = s"/app/${artifact.name}"

  new Dockerfile {
    from("openjdk:8-jre-alpine")
    run("apk", "--no-cache", "add", "ttf-dejavu")
    add(artifact, artifactTargetPath)
    entryPoint("java", "-Dorg.scalatra.environment=production", "-Xmx512M", "-jar", artifactTargetPath)
  }
}

docker / imageNames := Seq(
  ImageName(namespace = Some(organization.value),
            repository = name.value,
            tag = Some(System.getProperty("docker.tag", "SNAPSHOT")))
)

Test / parallelExecution := false

resolvers ++= scala.util.Properties
  .envOrNone("NDLA_RELEASES")
  .map(repo => "Release Sonatype Nexus Repository Manager" at repo)
  .toSeq
