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
import org.scalatest.{BeforeAndAfterEach, Matchers}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.http.Status
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.test.FakeRequest
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.services.{LocalSubscription, RemoteSubscription, RunMode, SubscriptionHandlerImpl}
import uk.gov.hmrc.cbcr.util.UnitSpec
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CBCIdControllerSpec
    extends UnitSpec with Matchers with ScalaFutures with BeforeAndAfterEach with GuiceOneAppPerSuite with MockAuth {

  val localGen = mock[LocalSubscription]
  val remoteGen = mock[RemoteSubscription]
  var runMode = mock[RunMode]
  when(runMode.env) thenReturn "Dev"

  implicit val as = app.injector.instanceOf[ActorSystem]
  val config = app.injector.instanceOf[Configuration]

  implicit val mat = ActorMaterializer()

  val srb = SubscriptionDetails(
    BusinessPartnerRecord(
      "SafeID",
      Some(OrganisationResponse("Name")),
      EtmpAddress("Some ave", None, None, None, None, "GB")),
    SubscriberContact(name = None, "dave", "jones", PhoneNumber("123456789").get, EmailAddress("bob@bob.com")),
    None,
    Utr("7000000002")
  )
  val crb = CorrespondenceDetails(
    EtmpAddress("Some ave", None, None, None, None, "GB"),
    ContactDetails(EmailAddress("bob@bob.com"), PhoneNumber("123456789").get),
    ContactName("Bob", "Bobbet"))

  val id = CBCId.create(1).getOrElse(fail("Can not generate CBCId"))
  implicit val hc = HeaderCarrier()

  override def afterEach(): Unit = {
    super.afterEach()
    reset(localGen, remoteGen)
  }

  "The CBCIdController" should {
    "query the localCBCId generator when useDESApi is set to false" in {

      val handler = new SubscriptionHandlerImpl(
        config ++ Configuration("Dev.CBCId.useDESApi" -> false),
        localGen,
        remoteGen,
        runMode)
      val controller = new CBCIdController(handler, cBCRAuth, cc)
      val fakeRequestSubscribe = FakeRequest("POST", "/cbc-id").withBody(Json.toJson(srb))
      when(localGen.createSubscription(any())(any())) thenReturn Future.successful(Ok(Json.obj("cbc-id" -> id.value)))
      val response = controller.subscribe()(fakeRequestSubscribe)
      status(response) shouldBe Status.OK
      jsonBodyOf(response).futureValue shouldEqual Json.obj("cbc-id" -> "XTCBC0100000001")
    }
    "query the remoteCBCId generator when useDESApi is set to true" in {
      val handler = new SubscriptionHandlerImpl(
        config ++ Configuration("Dev.CBCId.useDESApi" -> true),
        localGen,
        remoteGen,
        runMode)
      val controller = new CBCIdController(handler, cBCRAuth, cc)
      val fakeRequestSubscribe = FakeRequest("POST", "/cbc-id").withBody(Json.toJson(srb))
      when(remoteGen.createSubscription(any())(any())) thenReturn Future.successful(Ok(Json.obj("cbc-id" -> id.value)))
      val response = controller.subscribe()(fakeRequestSubscribe)
      status(response) shouldBe Status.OK
      jsonBodyOf(response).futureValue shouldEqual Json.obj("cbc-id" -> "XTCBC0100000001")
    }
    "generate bad request response if request doesn't contain valid subscriptionDetails" in {

      val handler = new SubscriptionHandlerImpl(
        config ++ Configuration("Dev.CBCId.useDESApi" -> false),
        localGen,
        remoteGen,
        runMode)
      val controller = new CBCIdController(handler, cBCRAuth, cc)
      val fakeRequestSubscribe = FakeRequest("POST", "/cbc-id").withBody(Json.obj("bad" -> "request"))
      val response = controller.subscribe()(fakeRequestSubscribe)
      status(response) shouldBe Status.BAD_REQUEST
    }
    "return 200 when updateSubscription passed valid CorrespondenceDetails in request" in {

      val handler = new SubscriptionHandlerImpl(
        config ++ Configuration("Dev.CBCId.useDESApi" -> true),
        localGen,
        remoteGen,
        runMode)
      val controller = new CBCIdController(handler, cBCRAuth, cc)
      val fakeRequestSubscribe = FakeRequest("POST", "/cbc-id").withBody(Json.toJson(crb))
      when(remoteGen.updateSubscription(any(), any())(any())) thenReturn Future.successful(
        Ok(Json.obj("cbc-id" -> id.value)))
      val response = controller.updateSubscription("safeId")(fakeRequestSubscribe)
      status(response) shouldBe Status.OK
    }
    "return 400 when updateSubscription passed invalid CorrespondenceDetails in request" in {

      val handler = new SubscriptionHandlerImpl(
        config ++ Configuration("Dev.CBCId.useDESApi" -> true),
        localGen,
        remoteGen,
        runMode)
      val controller = new CBCIdController(handler, cBCRAuth, cc)
      val fakeRequestSubscribe = FakeRequest("POST", "/cbc-id").withBody(Json.obj("bad" -> "request"))
      val response = controller.updateSubscription("safeId")(fakeRequestSubscribe)
      status(response) shouldBe Status.BAD_REQUEST
    }

    "no error generated when getSubscription called" in {

      val handler = new SubscriptionHandlerImpl(
        config ++ Configuration("Dev.CBCId.useDESApi" -> false),
        localGen,
        remoteGen,
        runMode)
      val controller = new CBCIdController(handler, cBCRAuth, cc)
      val fakeRequestSubscribe = FakeRequest("GET", "/cbc-id")
      when(localGen.getSubscription(any())(any())) thenReturn Future.successful(Ok(Json.obj("some" -> "thing")))
      val response = controller.getSubscription("safeId")(fakeRequestSubscribe)
      status(response) shouldBe Status.OK
    }
  }

}
