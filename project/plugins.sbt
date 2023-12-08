resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/"
resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)
resolvers += Resolver.jcenterRepo

addSbtPlugin("uk.gov.hmrc"        % "sbt-auto-build"      % "3.15.0")
addSbtPlugin("uk.gov.hmrc"        % "sbt-distributables"  % "2.2.0")
addSbtPlugin("com.typesafe.play"  % "sbt-plugin"          % "2.8.18")
addSbtPlugin("org.scoverage"      % "sbt-scoverage"       % "2.0.8")
addSbtPlugin("com.lucidchart"     % "sbt-scalafmt"        % "1.16")

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
