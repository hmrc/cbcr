import play.sbt.PlayImport.PlayKeys.playDefaultPort
import sbt.*
import sbt.Keys.*
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "cbcr"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(CodeCoverageSettings.settings *)
  .settings(onLoadMessage := "")
  .settings(scalafmtOnCompile := true)
  .settings(playDefaultPort := 9797)
  .settings(majorVersion := 1)
  .settings(
    scalaVersion := "2.13.14",
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    scalacOptions += "-Wconf:src=routes/.*:s"
  )
  // Disable default sbt Test options (might change with new versions of bootstrap)
  .settings(Test / testOptions -= Tests
    .Argument("-o", "-u", "target/test-reports", "-h", "target/test-reports/html-report"))
  // Suppress successful events in Scalatest in standard output (-o)
  // Options described here: https://www.scalatest.org/user_guide/using_scalatest_with_sbt
  .settings(
    Test / testOptions += Tests.Argument(
      TestFrameworks.ScalaTest,
      "-oNCHPQR",
      "-u",
      "target/test-reports",
      "-h",
      "target/test-reports/html-report"))

libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
