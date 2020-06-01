
organizationName := "ASERVO Software GmbH"

organization := "com.aservo"

name := "Crowd-LDAP-Server"

description := "LDAP Server Bridge for Crowd"

version := "3.0.0"

autoScalaLibrary := false

crossPaths := false

resolvers ++= Seq(
  Resolver.jcenterRepo,
  Resolver.bintrayRepo("typesafe", "maven-releases"),
  Opts.resolver.sonatypeReleases,
  Opts.resolver.sonatypeSnapshots,
  "Atlassian" at "https://maven.atlassian.com/content/groups/public/"
)

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.30",
  "org.slf4j" % "slf4j-log4j12" % "1.7.30",
  "commons-logging" % "commons-logging" % "1.2",
  "commons-io" % "commons-io" % "2.6",
  "org.jetbrains" % "annotations" % "19.0.0",
  "com.google.code.gson" % "gson" % "2.8.6",
  "org.apache.directory.server" % "apacheds-all" % "2.0.0-M24",
  "com.atlassian.crowd" % "crowd-integration-client-rest" % "2.12.0",
  "com.atlassian.security" % "atlassian-cookie-tools" % "3.2.14" jar,
  "org.junit.platform" % "junit-platform-launcher" % "1.5.2" % "test,it",
  "org.junit.jupiter" % "junit-jupiter-engine" % "5.5.2" % "test,it",
  "org.bouncycastle" % "bcprov-jdk15on" % "1.65" % "test,it",
  "com.novocode" % "junit-interface" % "0.11" % "test,it"
)

fork in Test := true

jacocoReportSettings in Test := JacocoReportSettings()
  .withFormats(JacocoReportFormats.ScalaHTML, JacocoReportFormats.XML)
  .withFileEncoding("UTF-8")

jacocoReportSettings in IntegrationTest := JacocoReportSettings()
  .withFormats(JacocoReportFormats.ScalaHTML, JacocoReportFormats.XML)
  .withFileEncoding("UTF-8")

configs(IntegrationTest)
Defaults.itSettings

unmanagedSourceDirectories in IntegrationTest ++= Seq((javaSource in Test).value)

enablePlugins(JacocoItPlugin)
