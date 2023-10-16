package uk.gov.hmrc.cbcr.config

import play.api.{ConfigLoader, Configuration, PlayException}
import uk.gov.hmrc.cbcr.config.ConfigurationOps.ConfigurationOps
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}

object ConfigurationOps {
  implicit class ConfigurationOps(self: Configuration) {
    def load[A](path: String)(implicit loader: ConfigLoader[A]): A =
      self
        .getOptional[A](path)
        .getOrElse(throw new PlayException("Configuration error", s"Missing configuration key: $path", null))
  }
}

@Singleton
class ApplicationConfig @Inject()(configuration: Configuration, servicesConfig: ServicesConfig) {
  import uk.gov.hmrc.cbcr.config.ConfigurationOps.ConfigurationOps
  val etmpHod: String = servicesConfig.baseUrl("etmp-hod")
  val etmpHodEnvironment: String = servicesConfig.getConfString("etmp-hod.environment", "")
  val etmpHodAuthorizationToken: String = servicesConfig.getConfString("etmp-hod.authorization-token", "")
  val useDESApi: Boolean = configuration.load[Boolean]("Prod.CBCId.useDESApi")
  val docRefIdsToClear: String = configuration.getOptional[String]("Prod.DocRefId.clear").getOrElse("")
  val retrieveReportingEntity: Boolean =
    configuration.getOptional[Boolean]("Prod.retrieve.ReportingEntity").getOrElse(false)
  val retrieveDocRefId: String = configuration.getOptional[String]("Prod.retrieve.docRefId").getOrElse("")
  val emailAlertLogString: String =
    configuration.getOptional[String]("Prod.emailAlertLogString").getOrElse("CBCR_EMAIL_FAILURE")
  val auditCbcIds: String = configuration.getOptional[String]("Prod.audit.cbcIds").getOrElse("")
  val auditSubscriptions: Boolean = configuration.getOptional[Boolean]("Prod.audit.subscriptions").getOrElse(false)
}
