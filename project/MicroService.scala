import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import play.sbt.PlayImport.PlayKeys.playDefaultPort

trait MicroService {

  import uk.gov.hmrc._
  import DefaultBuildSettings._
  import TestPhases.oneForkedJvmPerTest
  import uk.gov.hmrc.{SbtAutoBuildPlugin, SbtArtifactory}
  import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
  import uk.gov.hmrc.versioning.SbtGitVersioning
  import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

  val appName: String

  lazy val appDependencies : Seq[ModuleID] = ???
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
    "uk.gov.hmrc",
    "uk.gov.hmrc.cbcr.repositories.*",
    "uk.gov.hmrc.cbcr.connectors.*",
    "uk.gov.hmrc.cbcr.audit.*",
    "uk.gov.hmrc.cbcr.controllers.test.*",
    "uk.gov.hmrc.cbcr.services.RetrieveReportingEntityService",
    "uk.gov.hmrc.cbcr.services.DataMigrationCriteria",
    "uk.gov.hmrc.cbcr",
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
    .settings(
      scalaVersion := "2.11.11",
      libraryDependencies ++= appDependencies,
      retrieveManaged := true,
      evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false)
    )
    .configs(IntegrationTest)
    .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
    .settings(
      Keys.fork in IntegrationTest := false,
      unmanagedSourceDirectories in IntegrationTest := (baseDirectory in IntegrationTest)(base => Seq(base / "it")).value,
      addTestReportOption(IntegrationTest, "int-test-reports"),
      testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
      parallelExecution in IntegrationTest := false)
    .settings(resolvers ++= Seq(
      "hmrc-releases" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases/",
      Resolver.jcenterRepo
    ))
}

private object TestPhases {

  def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
    tests map {
      test => new Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
    }
}
