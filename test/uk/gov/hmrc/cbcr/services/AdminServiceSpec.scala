package uk.gov.hmrc.cbcr.services

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.http.Status
import play.api.test.Helpers.stubControllerComponents
import play.api.test.FakeRequest
import uk.gov.hmrc.cbcr.models.ReportingEntityData
import uk.gov.hmrc.cbcr.repositories.{DocRefIdRepository, ReactiveDocRefIdRepository, ReportingEntityDataRepo}
import uk.gov.hmrc.cbcr.util.UnitSpec
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.Future

class AdminServiceSpec extends UnitSpec with MockitoSugar {

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  private val repo = mock[ReportingEntityDataRepo]
  private val docRefIdRepo = mock[ReactiveDocRefIdRepository]
  private val config = mock[Configuration]
  private val docRepo = mock[DocRefIdRepository]
  private val runMode = new RunMode(config)
  private val audit = mock[AuditConnector]
  private val cc = stubControllerComponents()

//  private val cc = mock[ControllerComponents]

  private val adminService = new AdminService(docRefIdRepo, config, repo, docRepo, runMode, audit, cc)
  private val fakeRequest = FakeRequest()
  private val tin = "tin"
  private val reportingPeriod = "aReportingPeriod"

   "adminQueryTin" should {
     "return a notFound response" in {
       when(repo.queryTIN(any(),any())) thenReturn Future.successful(List[ReportingEntityData]())

       val result = adminService.adminQueryTin(tin, reportingPeriod)(fakeRequest)
       verifyStatusCode(result, Status.NOT_FOUND)
     }
   }
}
