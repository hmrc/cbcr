import sbt.Setting
import scoverage.ScoverageKeys

object CodeCoverageSettings {

  private val excludedPackages: Seq[String] = Seq(
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

  val settings: Seq[Setting[?]] = Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 100,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}
