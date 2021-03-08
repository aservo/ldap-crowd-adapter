
logLevel := Level.Warn

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

addSbtPlugin("net.aichler" % "sbt-jupiter-interface" % "0.9.0")

addSbtPlugin("com.github.sbt" % "sbt-jacoco" % "3.3.0")

addSbtPlugin("com.github.mwz" % "sbt-sonar" % "2.2.0")
