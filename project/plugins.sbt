resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/"
resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(
  Resolver.ivyStylePatterns)
resolvers += Resolver.jcenterRepo

addSbtPlugin("uk.gov.hmrc"       % "sbt-auto-build"     % "3.20.0")
addSbtPlugin("uk.gov.hmrc"       % "sbt-distributables" % "2.5.0")
addSbtPlugin("org.playframework" %% "sbt-plugin"        % "3.0.1")
addSbtPlugin("org.scoverage"     % "sbt-scoverage"      % "2.0.9")
addSbtPlugin("com.lucidchart"    %% "sbt-scalafmt"      % "1.16")

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
