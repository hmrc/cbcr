import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport._
import play.sbt.PlayImport.PlayKeys.playDefaultPort
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "cbcr"

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
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(scoverageSettings : _*)
  .settings(onLoadMessage := "")
  .settings(scalaSettings: _*)
  .settings(playDefaultPort := 9797)
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
  .settings(Global / lintUnusedKeysOnLoad := false)
  // Disable default sbt Test options (might change with new versions of bootstrap)
  .settings(Test / testOptions -= Tests.Argument("-o", "-u", "target/test-reports", "-h", "target/test-reports/html-report"))
  // Suppress successful events in Scalatest in standard output (-o)
  // Options described here: https://www.scalatest.org/user_guide/using_scalatest_with_sbt
  .settings(Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oNCHPQR", "-u", "target/test-reports", "-h", "target/test-reports/html-report"))

def oneForkedJvmPerTest(tests: Seq[TestDefinition]) = {
  tests.map { test =>
    new Group(test.name, Seq(test), SubProcess(ForkOptions().withRunJVMOptions(Vector(s"-Dtest.name=${test.name}"))))
  }
}

libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
