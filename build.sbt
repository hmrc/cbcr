import uk.gov.hmrc._
import DefaultBuildSettings._
import uk.gov.hmrc.{SbtAutoBuildPlugin, SbtArtifactory}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import play.sbt.PlayImport.PlayKeys.playDefaultPort
import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport._
import play.sbt.PlayImport._
import sbt._

val appName = "cbcr"

lazy val appDependencies: Seq[ModuleID] = compile ++ test()

val akkaVersion     = "2.5.23"

val akkaHttpVersion = "10.0.15"


dependencyOverrides += "com.typesafe.akka" %% "akka-stream"    % akkaVersion

dependencyOverrides += "com.typesafe.akka" %% "akka-protobuf"  % akkaVersion

dependencyOverrides += "com.typesafe.akka" %% "akka-slf4j"     % akkaVersion

dependencyOverrides += "com.typesafe.akka" %% "akka-actor"     % akkaVersion

dependencyOverrides += "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion

val compile = Seq(
  "org.reactivemongo" %% "play2-reactivemongo"        % "1.0.3-play26",
  "org.reactivemongo" %% "reactivemongo-bson"         % "0.20.13",
  ws,
  "uk.gov.hmrc"       %% "auth-client"                % "5.2.0-play-26",
  "uk.gov.hmrc"       %% "bootstrap-backend-play-26"  % "4.2.0",
  "uk.gov.hmrc"       %% "domain"                     % "5.11.0-play-26",
  "org.typelevel"     %% "cats"                       % "0.9.0" exclude("org.scalacheck","scalacheck_2.12"),
  "com.github.kxbmap" %% "configs"                    % "0.6.0",
  "uk.gov.hmrc"       %% "emailaddress"               % "3.5.0",
  "uk.gov.hmrc"       %% "simple-reactivemongo"       % "8.0.0-play-26"

)

def test(scope: String = "test,it") = Seq(
  "com.typesafe.akka"       %% "akka-testkit"         % "2.6.14" % scope,
  "org.scalatest"           %% "scalatest"            % "3.2.7"  % scope,
  "org.pegdown"             % "pegdown"               % "1.6.0"  % scope,
  "org.scalatestplus.play"  %% "scalatestplus-play"   % "5.1.0"  % scope,
  "org.mockito"             %  "mockito-core"         % "3.9.0"  % scope,
  "org.scalacheck"          %% "scalacheck"           % "1.15.3" % scope,
  "org.eu.acolyte"          %% "play-reactive-mongo"  % "1.0.57" % scope
)

lazy val plugins : Seq[Plugins] = Seq.empty
lazy val playSettings : Seq[Setting[_]] = Seq.empty

lazy val excludedPackages = Seq(
  "<empty>",
  "Reverse*",
  "models/.data/..*",
  "view.*",
  ".*standardError*.*",
  ".*govuk_wrapper*.*",
  ".*main_template*.*",
  "uk.gov.hmrc.BuildInfo",
  "app.*",
  "prod.*",
  "config.*",
  "testOnlyDoNotUseInAppConf.*",
  "testOnly.*",
  "test",
  "uk.gov.hmrc.cbcr.controllers.test.TestSubscriptionDataController",
  "uk.gov.hmrc.cbcr.models.ContactDetails",
  "uk.gov.hmrc.cbcr.models.CorrDocRefId",
  "uk.gov.hmrc.cbcr.models.CorrespondenceDetails",
  "uk.gov.hmrc.cbcr.models.CountryCode",
  "uk.gov.hmrc.cbcr.models.DbOperationResult",
  "uk.gov.hmrc.cbcr.models.InvalidState",
  "uk.gov.hmrc.cbcr.models.MessageRefId",
  "uk.gov.hmrc.cbcr.models.MigrationRequest",
  "uk.gov.hmrc.cbcr.models.SubscriptionRequest",
  "uk.gov.hmrc.cbcr.models.Utr*"
)

lazy val scoverageSettings = {
  import scoverage._
  Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageMinimum := 80,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true
  )
}


lazy val microservice = Project(appName, file("."))
  .enablePlugins(Seq(play.sbt.PlayScala,SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory) ++ plugins : _*)
  .settings(playSettings ++ scoverageSettings : _*)
  .settings(scalaSettings: _*)
  .settings(playDefaultPort := 9797)
  .settings(publishingSettings: _*)
  .settings(majorVersion := 1 )
  .settings(defaultSettings(): _*)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    scalaVersion := "2.12.11",
    libraryDependencies ++= appDependencies,
    retrieveManaged := true,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    scalafmtOnCompile in Compile := true,
    scalafmtOnCompile in Test := true
  )
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    Keys.fork in IntegrationTest := false,
    unmanagedSourceDirectories in IntegrationTest := (baseDirectory in IntegrationTest)(base => Seq(base / "it")).value,
    addTestReportOption(IntegrationTest, "int-test-reports"),
    testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
    parallelExecution in IntegrationTest := false,
    scalafmtOnCompile in IntegrationTest := true)
  .settings(resolvers ++= Seq(
    "hmrc-releases" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases/",
    Resolver.jcenterRepo
  ))

def oneForkedJvmPerTest(tests: Seq[TestDefinition]) = {
  tests.map { test =>
    new Group(test.name, Seq(test), SubProcess(ForkOptions().withRunJVMOptions(Vector(s"-Dtest.name=${test.name}"))))
  }
}
