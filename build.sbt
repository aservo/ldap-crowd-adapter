
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
  "org.slf4j" % "slf4j-api" % "1.7.21",
  "org.slf4j" % "slf4j-log4j12" % "1.7.21",
  "commons-logging" % "commons-logging" % "1.2",
  "org.jetbrains" % "annotations-java5" % "15.0",
  "com.google.code.gson" % "gson" % "2.8.5",
  "org.apache.directory.server" % "apacheds-all" % "2.0.0-M24",
  "com.atlassian.crowd" % "crowd-integration-client-rest" % "2.9.1",
  "com.atlassian.security" % "atlassian-cookie-tools" % "3.2.4" jar,
  "junit" % "junit" % "4.12" % "test",
  "com.novocode" % "junit-interface" % "0.11" % "test" exclude("junit", "junit-dep"),
  "org.bouncycastle" % "bcprov-jdk15on" % "1.61" % "test"
)
