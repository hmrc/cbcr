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

package uk.gov.hmrc.cbcr.controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.data.OptionT
import cats.instances.future._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.libs.json.Json._
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{FakeRequest, Helpers}
import reactivemongo.api.commands.{DefaultWriteResult, WriteError, WriteResult}
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.repositories.SubscriptionDataRepository
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubscriptionDataControllerSpec extends UnitSpec with MockitoSugar {

  val store = mock[SubscriptionDataRepository]

  val okResult = DefaultWriteResult(true,0,Seq.empty,None,None,None)

  val failResult = DefaultWriteResult(false,1,Seq(WriteError(1,1,"Error")),None,None,Some("Error"))

  val bpr = BusinessPartnerRecord(Some("MySafeID"),Some(OrganisationResponse("Dave Corp")),EtmpAddress(None,None,None,None,None,None))
  val exampleSubscriptionData = SubscriptionDetails(bpr,SubscriberContact("Dave","Jones","02072653787",EmailAddress("dave@dave.com")),CBCId("XGCBC0000000001").get,Utr("utr"))

  val controller = new SubscriptionDataController(store)

  val fakePostRequest: FakeRequest[JsValue] = FakeRequest(Helpers.POST, "/saveSubscriptionData").withBody(toJson(exampleSubscriptionData))

  val fakeGetRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(Helpers.GET, "/retrieveSubscriptionData")

  val fakeDeleteRequest = FakeRequest(Helpers.DELETE,"subscription-data")


  implicit val as = ActorSystem()
  implicit val mat = ActorMaterializer()

  val cbcId = CBCId.create(1).getOrElse(fail("Couldn't generate cbcid"))
  val utr = Utr("7000000002")

  "The SubscriptionDataController" should {
    "respond with a 200 when asked to store SubscriptionData" in {
      when(store.save(any(classOf[SubscriptionDetails]))).thenReturn(Future.successful(okResult))
      val result  = controller.saveSubscriptionData()(fakePostRequest)
      status(result) shouldBe Status.OK
    }

    "respond with a 500 if there is a DB failure" in {
      when(store.save(any(classOf[SubscriptionDetails]))).thenReturn(Future.successful(failResult))
      val result  = controller.saveSubscriptionData()(fakePostRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "respond with a 200 and a SubscriptionData when asked to retrieve an existing CBCID" in {
      when(store.get(any(classOf[CBCId]))).thenReturn(OptionT.some[Future,SubscriptionDetails](exampleSubscriptionData))
      val result  = controller.retrieveSubscriptionDataCBCId(cbcId)(fakeGetRequest)
      status(result) shouldBe Status.OK
      jsonBodyOf(result).validate[SubscriptionDetails].isSuccess shouldBe true
    }

    "respond with a 404 when asked to retrieve a non-existent CBCID" in {
      when(store.get(any(classOf[CBCId]))).thenReturn(OptionT.none[Future,SubscriptionDetails])
      val result  = controller.retrieveSubscriptionDataCBCId(cbcId)(fakeGetRequest)
      status(result) shouldBe Status.NOT_FOUND
    }

    "respond with a 200 when queried with a utr that already exists" in {
      when(store.get(any(classOf[Utr]))).thenReturn(OptionT.some[Future,SubscriptionDetails](exampleSubscriptionData))
      val result  = controller.retrieveSubscriptionDataUtr(utr)(fakeGetRequest)
      status(result) shouldBe Status.OK
    }

    "respond with a 404 when queried with a utr that doesnt exist" in {
      when(store.get(any(classOf[Utr]))).thenReturn(OptionT.none[Future,SubscriptionDetails])
      val result  = controller.retrieveSubscriptionDataUtr(utr)(fakeGetRequest)
      status(result) shouldBe Status.NOT_FOUND

    }

    "respond with a 200 when asked to clear a record that exists" in {
      when(store.clear(any(classOf[CBCId]))).thenReturn(OptionT.some[Future,WriteResult](okResult))
      val result  = controller.clearSubscriptionData(cbcId)(fakeDeleteRequest)
      status(result) shouldBe Status.OK
    }

    "respond with a 404 when asked to clear a record that doesn't exist" in {
      when(store.clear(any(classOf[CBCId]))).thenReturn(OptionT.none[Future,WriteResult])
      val result  = controller.clearSubscriptionData(cbcId)(fakeDeleteRequest)
      status(result) shouldBe Status.NOT_FOUND
    }

    "respond with a 500 when asked to clear a record but something goes wrong" in {
      when(store.clear(any(classOf[CBCId]))).thenReturn(OptionT.some[Future,WriteResult](failResult))
      val result  = controller.clearSubscriptionData(cbcId)(fakeDeleteRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

  }
}
