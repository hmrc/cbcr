import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport._
import play.sbt.PlayImport.PlayKeys.playDefaultPort
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "cbcr"
val silencerVersion = "1.7.13"

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
    ScoverageKeys.coverageMinimumStmtTotal := 80,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(Seq(play.sbt.PlayScala, SbtDistributablesPlugin) ++ plugins : _*)
  .settings(playSettings ++ scoverageSettings : _*)
  .settings(scalaSettings: _*)
  .settings(playDefaultPort := 9797)
  .settings(publishingSettings: _*)
  .settings(majorVersion := 1 )
  .settings(defaultSettings(): _*)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    scalaVersion := "2.13.11",
    libraryDependencies ++= AppDependencies(),
    retrieveManaged := true,
    update / evictionWarningOptions := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true
  )
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(inConfig(Test)(testSettings))
  .settings(
    IntegrationTest / Keys.fork := false,
    IntegrationTest / unmanagedSourceDirectories := (IntegrationTest / baseDirectory)(base => Seq(base / "it")).value,
    addTestReportOption(IntegrationTest, "int-test-reports"),
    IntegrationTest / testGrouping := oneForkedJvmPerTest((IntegrationTest / definedTests).value),
    IntegrationTest / parallelExecution := false,
    IntegrationTest / scalafmtOnCompile := true,
    Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.ScalaLibrary,
    Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat,
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
    ),

  )
  .settings(scalacOptions += "-P:silencer:pathFilters=routes")
  .settings(Global / lintUnusedKeysOnLoad := false)

def oneForkedJvmPerTest(tests: Seq[TestDefinition]) = {
  tests.map { test =>
    new Group(test.name, Seq(test), SubProcess(ForkOptions().withRunJVMOptions(Vector(s"-Dtest.name=${test.name}"))))
  }
}

lazy val testSettings = Def.settings(
  // Must fork so that config system properties are set
  fork := true,
  unmanagedResourceDirectories += (baseDirectory.value / "test" / "resources"),
  javaOptions ++= Seq(
    "-Dconfig.resource=test.application.conf",
    "-Dlogger.resource=logback-test.xml"
  )
)

libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always