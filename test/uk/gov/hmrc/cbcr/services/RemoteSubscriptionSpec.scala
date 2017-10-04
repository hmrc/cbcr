/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.cbcr.services

import java.time.LocalDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.cbcr.connectors.DESConnector
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.test.UnitSpec
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.OneAppPerSuite
import play.api.http.Status._
import play.api.libs.json.{JsError, JsResult, JsValue, Json}

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }

class RemoteSubscriptionSpec extends UnitSpec with MockitoSugar with OneAppPerSuite with ScalaFutures{

  val desConnector = mock[DESConnector]

  implicit val as = app.injector.instanceOf[ActorSystem]
  implicit val ec = app.injector.instanceOf[ExecutionContext]
  implicit val mat = ActorMaterializer()

  val generator = new RemoteSubscription(desConnector)

  val cd = CorrespondenceDetails(
    EtmpAddress("line1",None,None,None,None,"GB"),
    ContactDetails(EmailAddress("test@test.com"),PhoneNumber("9876543").get),
    ContactName("FirstName","Surname")
  )

  val srb = SubscriptionRequest(
    "safeId",
    false,
    cd
  )
  val bpr = BusinessPartnerRecord("MySafeID",Some(OrganisationResponse("Dave Corp")),EtmpAddress("13 Accacia Ave",None,None,None,None,"GB"))
  val exampleSubscriptionData = SubscriptionDetails(bpr,SubscriberContact("Dave","Jones",PhoneNumber("02072653787").get,EmailAddress("dave@dave.com")),CBCId("XGCBC0000000001"),Utr("utr"))

  val getResponse = GetResponse(
    bpr.safeId,
    ContactName(exampleSubscriptionData.subscriberContact.firstName,exampleSubscriptionData.subscriberContact.lastName),
    ContactDetails(exampleSubscriptionData.subscriberContact.email,exampleSubscriptionData.subscriberContact.phoneNumber),
    exampleSubscriptionData.businessPartnerRecord.address
  )


  private val subscriptionRequestRsponse =
    """
      |{"processingDate":"2017-08-29T18:13:08Z",
      |"cbcSubscriptionID":"XHCBC1000000037"}
    """.stripMargin

  implicit val hc = new HeaderCarrier()

  val srr = SubscriptionResponse(LocalDateTime.now(),CBCId.create(1).getOrElse(fail("Could not generate CBCID")))
  "The remoteCBCIdGenerator" should {
    "be able to subscribe a user" which {

      "returns a 200 with a cbcid when the submission is successful" in {
        when(desConnector.createSubscription(any())) thenReturn Future.successful(HttpResponse(OK, responseJson = Some(Json.toJson(srr))))
        val response = generator.createSubscription(srb)
        status(response) shouldEqual OK
        jsonBodyOf(response).futureValue shouldEqual Json.obj("cbc-id" -> "XGCBC0000000001")
      }
      "returns a 400 when BAD_REQUEST is returned by DES" in {
        when(desConnector.createSubscription(any())) thenReturn Future.successful(HttpResponse(BAD_REQUEST, responseJson = Some(Json.obj("bad" -> "request"))))
        val response = generator.createSubscription(srb)
        status(response) shouldEqual BAD_REQUEST
      }
      "returns a 500 if the response is malformed" in {
        when(desConnector.createSubscription(any())) thenReturn Future.successful(HttpResponse(OK, responseJson = Some(Json.obj("something" -> 1))))
        val response = generator.createSubscription(srb)
        status(response) shouldEqual INTERNAL_SERVER_ERROR
      }
      "returns a 403 if the response is FORBIDDEN" in {
        when(desConnector.createSubscription(any())) thenReturn Future.successful(HttpResponse(FORBIDDEN, responseJson = Some(Json.obj("something" -> 1))))
        val response = generator.createSubscription(srb)
        status(response) shouldEqual FORBIDDEN
      }
      "returns a 503 if DES returns a SERVICE_UNAVAILABLE" in {
        when(desConnector.createSubscription(any())) thenReturn Future.successful(HttpResponse(SERVICE_UNAVAILABLE, responseJson = Some(Json.obj("something" -> 1))))
        val response = generator.createSubscription(srb)
        status(response) shouldEqual SERVICE_UNAVAILABLE
      }
    }
    "be able to update a user" which {
      "returns a 200 when provided with all the correct data" in {
        when(desConnector.updateSubscription(any(),any())) thenReturn Future.successful(HttpResponse(OK, responseJson = Some(Json.toJson(UpdateResponse(LocalDateTime.now())))))
        val response = generator.updateSubscription("safeId",cd)
        status(response) shouldEqual OK
      }
      "returns a 400 if the request is bad" in {
        when(desConnector.updateSubscription(any(),any())) thenReturn Future.successful(HttpResponse(BAD_REQUEST))
        val response = generator.updateSubscription("safeId",cd)
        status(response) shouldEqual BAD_REQUEST
      }
      "returns a 500 if DES returns a 500" in {
        when(desConnector.updateSubscription(any(),any())) thenReturn Future.successful(HttpResponse(INTERNAL_SERVER_ERROR))
        val response = generator.updateSubscription("safeId",cd)
        status(response) shouldEqual INTERNAL_SERVER_ERROR
      }
      "returns a 500 if DES returns invalid json" in {
        when(desConnector.updateSubscription(any(),any())) thenReturn Future.successful(HttpResponse(OK,  responseJson = Some(Json.obj("something" -> 1))))
        val response = generator.updateSubscription("safeId",cd)
        status(response) shouldEqual INTERNAL_SERVER_ERROR
      }
    }
    "be able to display the subscription details for a user" which {
      "queries the DES api and passes along the data" in {
        when(desConnector.getSubscription(any())) thenReturn Future.successful(HttpResponse(OK,  responseJson = Some(Json.toJson(getResponse))))
        val response = generator.getSubscription("safeId")
        status(response) shouldEqual OK
      }
      "returns a 400 if the request is invalid" in {
        when(desConnector.getSubscription(any())) thenReturn Future.successful(HttpResponse(BAD_REQUEST))
        val response = generator.getSubscription("safeId")
        status(response) shouldEqual BAD_REQUEST

      }
      "returns a 404 if the safeId is not found" in {
        when(desConnector.getSubscription(any())) thenReturn Future.successful(HttpResponse(NOT_FOUND))
        val response = generator.getSubscription("safeId")
        status(response) shouldEqual NOT_FOUND

      }
    }
  }

  "The CBCId" should {
    "construct a remote Etmp CbcId based on a string of the correct pattern" in {
      val id: Option[CBCId] = CBCId("XHCBC1000000037")
      id.isDefined shouldBe true

    }

    "Parse a Json SubscriptionRequestResponse " in {
      val result = Json.fromJson[SubscriptionResponse](Json.parse(subscriptionRequestRsponse))
      println(result)
      result.isInstanceOf[JsError] shouldNot equal(true)

      val decisionRequest: SubscriptionResponse =
        result.getOrElse(srr)
      println(decisionRequest)
      decisionRequest shouldNot equal(srr)
    }
  }

}
