
organizationName := "ASERVO Software GmbH"

organization := "com.aservo"

name := "LDAP-Crowd-Adapter"

description := "LDAP Server Bridge for Crowd"

version := "6.0.0"

autoScalaLibrary := false

crossPaths := false

credentials ++= (
  if (sys.env.contains("MAIN_REPO_REALM") &&
    sys.env.contains("MAIN_REPO_URL") &&
    sys.env.contains("MAIN_REPO_USERNAME") &&
    sys.env.contains("MAIN_REPO_PASSWORD"))
    Seq(Credentials(
      sys.env("MAIN_REPO_REALM"),
      java.net.URI.create(sys.env("MAIN_REPO_URL")).getAuthority,
      sys.env("MAIN_REPO_USERNAME"),
      sys.env("MAIN_REPO_PASSWORD")
    ))
  else
    Seq.empty
  )

if (sys.env.contains("MAIN_REPO_URL"))
  resolvers := Seq("Main Repository" at sys.env("MAIN_REPO_URL"))
else
  resolvers ++= Seq(
    Resolver.jcenterRepo,
    Resolver.bintrayRepo("typesafe", "maven-releases"),
    Opts.resolver.sonatypeReleases,
    Opts.resolver.sonatypeSnapshots,
    "Atlassian" at "https://maven.atlassian.com/content/groups/public/"
  )

if (sys.env.contains("MAIN_REPO_URL"))
  externalResolvers := Seq("Main Repository" at sys.env("MAIN_REPO_URL"))
else
  externalResolvers ++= Seq()

libraryDependencies ++= Seq(
  "org.apache.logging.log4j" % "log4j-api" % "2.17.1",
  "org.apache.logging.log4j" % "log4j-core" % "2.17.1",
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.17.1",
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "commons-io" % "commons-io" % "2.8.0",
  "org.jetbrains" % "annotations" % "20.1.0",
  "com.google.code.gson" % "gson" % "2.8.6",
  "org.apache.directory.server" % "apacheds-all" % "2.0.0-M24",
  "org.apache.commons" % "commons-dbcp2" % "2.8.0",
  "org.jooq" % "jooq" % "3.12.4",
  "com.h2database" % "h2" % "1.4.199",
  "org.postgresql" % "postgresql" % "42.2.19",
  "org.mariadb.jdbc" % "mariadb-java-client" % "2.7.2",
  "com.microsoft.sqlserver" % "mssql-jdbc" % "9.2.1.jre8",
  "com.atlassian.crowd.client" % "atlassian-crowd-rest-client" % "1.4",
  "com.atlassian.crowd" % "crowd-integration-client-rest" % "4.2.3",
  "com.atlassian.security" % "atlassian-cookie-tools" % "4.0.0" jar,
  "org.bouncycastle" % "bcprov-jdk15on" % "1.68" % "test",
  "net.aichler" % "jupiter-interface" % JupiterKeys.jupiterVersion.value % "test"
)

jacocoReportSettings in Test := JacocoReportSettings()
  .withFormats(JacocoReportFormats.XML)

enablePlugins(JacocoItPlugin)
