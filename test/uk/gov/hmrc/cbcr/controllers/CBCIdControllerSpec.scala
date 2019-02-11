/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.cbcr.controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Matchers}
import org.scalatestplus.play.OneAppPerSuite
import play.api.Configuration
import play.api.http.Status
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.test.FakeRequest
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.services.{LocalSubscription, RemoteSubscription, RunMode, SubscriptionHandlerImpl}
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class CBCIdControllerSpec extends UnitSpec with Matchers with ScalaFutures with MockitoSugar with BeforeAndAfterEach with OneAppPerSuite with MockAuth{

  val localGen = mock[LocalSubscription]
  val remoteGen = mock[RemoteSubscription]
  var runMode = mock[RunMode]
  when(runMode.env) thenReturn "Dev"

  implicit val as = app.injector.instanceOf[ActorSystem]
  val config = app.injector.instanceOf[Configuration]


  implicit val mat = ActorMaterializer()

  val srb = SubscriptionDetails(
    BusinessPartnerRecord("SafeID",Some(OrganisationResponse("Name")), EtmpAddress("Some ave",None,None,None,None, "GB")),
    SubscriberContact(name = None, "dave", "jones", PhoneNumber("123456789").get,EmailAddress("bob@bob.com")),
    None,
    Utr("7000000002")
  )

  val id = CBCId.create(1).getOrElse(fail("Can not generate CBCId"))
  implicit val hc = HeaderCarrier()

  override def afterEach(): Unit = {
    super.afterEach()
    reset(localGen,remoteGen)
  }

  "The CBCIdController" should {
    "query the localCBCId generator when useDESApi is set to false" in {

      val handler = new SubscriptionHandlerImpl(config ++ Configuration("Dev.CBCId.useDESApi" -> false),localGen,remoteGen,runMode)
      val controller = new CBCIdController(handler,cBCRAuth)
      val fakeRequestSubscribe = FakeRequest("POST", "/cbc-id").withBody(Json.toJson(srb))
      when(localGen.createSubscription(any())(any())) thenReturn Future.successful(Ok(Json.obj("cbc-id" -> id.value)))
      val response = controller.subscribe()(fakeRequestSubscribe)
      status(response) shouldBe Status.OK
      jsonBodyOf(response).futureValue shouldEqual Json.obj("cbc-id" -> "XTCBC0100000001")
    }
    "query the remoteCBCId generator when useDESApi is set to true" in {
      val handler = new SubscriptionHandlerImpl(config ++ Configuration("Dev.CBCId.useDESApi" -> true),localGen,remoteGen,runMode)
      val controller = new CBCIdController(handler,cBCRAuth)
      val fakeRequestSubscribe = FakeRequest("POST", "/cbc-id").withBody(Json.toJson(srb))
      when(remoteGen.createSubscription(any())(any())) thenReturn Future.successful(Ok(Json.obj("cbc-id" -> id.value)))
      val response = controller.subscribe()(fakeRequestSubscribe)
      status(response) shouldBe Status.OK
      jsonBodyOf(response).futureValue shouldEqual Json.obj("cbc-id" -> "XTCBC0100000001")
    }
  }

}

