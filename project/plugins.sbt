resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(
  Resolver.ivyStylePatterns)
resolvers += Resolver.jcenterRepo

addSbtPlugin("uk.gov.hmrc"        % "sbt-auto-build"         % "3.24.0")
addSbtPlugin("uk.gov.hmrc"        % "sbt-distributables"     % "2.5.0")
addSbtPlugin("org.playframework"  %% "sbt-plugin"            % "3.0.5")
addSbtPlugin("org.scoverage"      % "sbt-scoverage"          % "2.0.12")
addSbtPlugin("org.scalameta"      %% "sbt-scalafmt"          % "2.5.2")

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
