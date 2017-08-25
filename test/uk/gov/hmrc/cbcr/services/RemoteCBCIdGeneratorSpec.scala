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
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import play.api.http.Status._
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

class RemoteCBCIdGeneratorSpec extends UnitSpec with MockitoSugar with OneAppPerSuite with ScalaFutures{

  val desConnector = mock[DESConnector]

  implicit val as = app.injector.instanceOf[ActorSystem]
  implicit val ec = app.injector.instanceOf[ExecutionContext]
  implicit val mat = ActorMaterializer()

  val generator = new RemoteCBCIdGenerator(desConnector)
  val srb = SubscriptionRequestBody2(
    "SAFEID",
    false,
//    None,
    CorrespondenceDetails(
      EtmpAddress("18 Baxter Street",None,None,None,None,"GB"),
      ContactDetails(EmailAddress("blagh@blagh.com"),PhoneNumber("0207654322").get),
      ContactName("Joe","Blogs")
    )
  )

  implicit val hc = new HeaderCarrier()

  val srr = SubscriptionRequestResponse(LocalDateTime.now(),CBCId.create(1).getOrElse(fail("Could not generate CBCID")))
  "The remoteCBCIdGenerator" should {

    "return a 200 with a cbcid when the submission is successful" in {
      when(desConnector.subscribeToCBC(any())(any())) thenReturn Future.successful(HttpResponse(OK,responseJson = Some(Json.toJson(srr))))
      val response = generator.generateCBCId(srb)
      status(response) shouldEqual OK
      jsonBodyOf(response).futureValue shouldEqual Json.obj("cbc-id" -> "XGCBC0000000001")
    }
    "return a 400 when BAD_REQUEST is returned by DES" in {
      when(desConnector.subscribeToCBC(any())(any())) thenReturn Future.successful(HttpResponse(BAD_REQUEST, responseJson = Some(Json.obj("bad" -> "request"))))
      val response = generator.generateCBCId(srb)
      status(response) shouldEqual BAD_REQUEST
    }
    "return a 500 if the response is malformed" in {
      when(desConnector.subscribeToCBC(any())(any())) thenReturn Future.successful(HttpResponse(OK,responseJson = Some(Json.obj("something" -> 1))))
      val response = generator.generateCBCId(srb)
      status(response) shouldEqual INTERNAL_SERVER_ERROR
    }
    "return a 403 if the response is FORBIDDEN" in {
      when(desConnector.subscribeToCBC(any())(any())) thenReturn Future.successful(HttpResponse(FORBIDDEN,responseJson = Some(Json.obj("something" -> 1))))
      val response = generator.generateCBCId(srb)
      status(response) shouldEqual FORBIDDEN
    }
    "return a 503 if DES returns a SERVICE_UNAVAILABLE" in {
      when(desConnector.subscribeToCBC(any())(any())) thenReturn Future.successful(HttpResponse(SERVICE_UNAVAILABLE, responseJson = Some(Json.obj("something" -> 1))))
      val response = generator.generateCBCId(srb)
      status(response) shouldEqual SERVICE_UNAVAILABLE
    }
  }

}
