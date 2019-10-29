package uk.gov.hmrc.cbcr.connectors

import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import uk.gov.hmrc.cbcr.controllers.MockAuth
import org.mockito.Mockito._
import play.api.libs.json.Writes
import uk.gov.hmrc.cbcr.services.RunMode
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.OneAppPerSuite
import play.api.Configuration
import uk.gov.hmrc.cbcr.models.Email
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.cbcr.util.UnitSpec

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

class EmailConnectorSpec extends UnitSpec with MockAuth with ScalaFutures with OneAppPerSuite{

  "send email" should {

    "submit request to email micro service to send email and get successful response status" in new Setup {

      // given
      val endpointUrl = "emailHost://emailHost:1337/hmrc/email"
      when(httpMock.POST[Email, HttpResponse]
        (
          url = eqTo(endpointUrl),
          body = any[Email],
          headers = any[Seq[(String, String)]]())
        (
          wts = any[Writes[Email]](),
          rds = any[HttpReads[HttpResponse]](),
          hc = any[HeaderCarrier],
          ec = any[ExecutionContext])
      ).thenReturn(Future.successful(HttpResponse(202)))

      // when
      val result = await(connector.sendEmail(correctEmail))

      // then
      result.status shouldBe 202

      // and
      val expectedResponseBody = Email(List("tyrion.lannister@gmail.com"), templateId, paramsSub)
      verify(httpMock).POST(
        url = eqTo(endpointUrl),
        body = eqTo(expectedResponseBody),
        headers = any[Seq[(String, String)]]())(
        wts = any[Writes[Email]](),
        rds = any[HttpReads[HttpResponse]](),
        hc = eqTo(hc),
        ec = eqTo(executionContext))
    }
  }

  sealed trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val executionContext: ExecutionContextExecutor = ExecutionContext.Implicits.global
    val httpMock: HttpClient = mock[HttpClient]
   // val mockEmailConnector = mock[EmailConnectorImpl]
    val mockAuditConnector = mock[AuditConnector]
    val runMode = mock[RunMode]
    val config = app.injector.instanceOf[Configuration]
  //  val params = Map("p1" -> "v1")
    val templateId = "my-template"
    val recipient = "user@example.com"
   // val emailService = new EmailService(mockEmailConnector, mockAuditConnector, config, runMode)
    val paramsSub = Map("f_name" → "Tyrion","s_name" → "Lannister", "cbcrId" -> "XGCBC0000000001")
    val correctEmail: Email = Email(List("tyrion.lannister@gmail.com"), "cbcr_subscription", paramsSub)
   // implicit val hc = HeaderCarrier()

    //val appConfig = mock[AppConfig]
   // val environment = mock[Environment]
   /* val configuration:Configuration = mock[Configuration]
    when(configuration.getString(any(),any())).thenReturn(Some("emailHost"))
    when(configuration.getInt(any())).thenReturn(Some(1337))*/

    val connector = new EmailConnectorImpl(config,httpMock)

  }


}
