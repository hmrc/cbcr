package uk.gov.hmrc.cbcr.services

import cats.data.NonEmptyList
import ch.qos.logback.classic.Level
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{Configuration, Logger}
import uk.gov.hmrc.cbcr.models.{
  CBC701,
  DocRefId,
  ReportingEntityData,
  TIN,
  UltimateParentEntity
}
import uk.gov.hmrc.cbcr.repositories.ReportingEntityDataRepo
import uk.gov.hmrc.cbcr.util.{LogCapturing, UnitSpec}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetrieveReportingEntityServiceSpec
    extends LogCapturing
    with UnitSpec
    with GuiceOneAppPerSuite
    with ScalaFutures {

  private val reportingEntityDataRepo =
    app.injector.instanceOf[ReportingEntityDataRepo]
  private val configuration = app.injector.instanceOf[Configuration]
  private val runMode = new RunMode(configuration)
  private val audit = app.injector.instanceOf[AuditConnector]
  private val mockRunMode = mock[RunMode]
  val mockReportingEntityDataRepo = mock[ReportingEntityDataRepo]


  private val retrieveReportingEntityService =
    new RetrieveReportingEntityService(reportingEntityDataRepo,
                                       configuration,
                                       runMode,
                                       audit)
  private val retrieveReportingEntityServiceWithMockRunMode =
    new RetrieveReportingEntityService(reportingEntityDataRepo,
                                       configuration,
                                       mockRunMode,
                                       audit)

  private val tin = TIN("value", "issuedBy")
  private val emptyDocRefId = DocRefId("")
  private val ultimateParentEntity = UltimateParentEntity("")
  private val reportingEntityData =
    ReportingEntityData(NonEmptyList(emptyDocRefId, List.empty),
                        List.empty,
                        DocRefId(""),
                        tin,
                        ultimateParentEntity,
                       reportingRole = CBC701,
                        None,
                        None)

  private def isRetrieveReportingEntityTrue(
      rrEntityService: RetrieveReportingEntityService) =
    rrEntityService.retrieveReportingEntity

  "the val retrieveReportingEntity" should {
    "return true if it successfully matches the required values" in {
      isRetrieveReportingEntityTrue(retrieveReportingEntityService) shouldBe true
    }
    "return false if the configuration value does not match any of the required values" in {
      when(mockRunMode.env) thenReturn "wrongEnv"
      isRetrieveReportingEntityTrue(
        retrieveReportingEntityServiceWithMockRunMode) shouldBe false
    }

    "log info if the configuration value does not match any of the required values" in {
      withCaptureOfLoggingFrom(Logger) { logs =>
        new RetrieveReportingEntityService(reportingEntityDataRepo,
                                           configuration,
                                           mockRunMode,
                                           audit)
        when(mockRunMode.env) thenReturn "wrongEnv"
        logs.count(_.getLevel == Level.INFO) shouldBe 1
      }
    }
    "log info if the configuration value matches the required values but the query of the docRefId is empty" in {
      withCaptureOfLoggingFrom(Logger) { logs =>
       val service = new RetrieveReportingEntityService(mockReportingEntityDataRepo,
                                           configuration,
                                           runMode,
                                           audit)
        service.
        when(mockReportingEntityDataRepo.query(any[String]())) thenReturn Future.successful(List[ReportingEntityData]())

        logs.count(_.getLevel == Level.INFO) shouldBe 3
        logs.reverse.headOption shouldBe Some("")

      }
    }
  }
}
