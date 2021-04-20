/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.cbcr.repositories

import cats.data.OptionT
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.WriteResult
import uk.gov.hmrc.cbcr.controllers.MockAuth
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.util.UnitSpec
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class SubscriptionDataRepositorySpec extends UnitSpec with MockAuth with GuiceOneAppPerSuite {

  val config = app.injector.instanceOf[Configuration]
  implicit val ec = app.injector.instanceOf[ExecutionContext]
  implicit val hc = HeaderCarrier()
  lazy val reactiveMongoApi = app.injector.instanceOf[ReactiveMongoApi]
  val subscriptionDataRepository = new SubscriptionDataRepository(reactiveMongoApi)
  val subscriberContact =
    SubscriberContact(name = None, "Dave", "Jones", PhoneNumber("02072653787").get, EmailAddress("dave@dave.com"))
  val subscriberContact1 = SubscriberContact(
    None,
    "changedFirstName",
    "changedLastName",
    PhoneNumber("07777888866").get,
    EmailAddress("changedbob@bob.com"))
  val utr = Utr("7000000003")
  val cbcId = CBCId("XGCBC0000000001")
  val address = EtmpAddress("address1", Some("address2"), Some("address3"), Some("address4"), Some("PO1 1OP"), "UK")
  val bpr = BusinessPartnerRecord(
    "MySafeID",
    Some(OrganisationResponse("Dave Corp")),
    EtmpAddress("13 Accacia Ave", None, None, None, None, "GB"))
  val exampleSubscriptionData = SubscriptionDetails(
    bpr,
    SubscriberContact(name = None, "Dave", "Jones", PhoneNumber("02072653787").get, EmailAddress("dave@dave.com")),
    cbcId,
    utr)
  val jsonObct = Json.obj()

  "Calls to clear cbcId Details" should {
    "successfully clear  details using cbcId" in {
      val result: OptionT[Future, WriteResult] = subscriptionDataRepository.clearCBCId(cbcId.value)
      await(result.value.map(r => r.get.ok)) shouldBe true
    }
  }

  "Calls to clear  " should {
    "should successfully clear all data" in {
      val result: Future[WriteResult] = subscriptionDataRepository.clear(utr)
      await(result.map(r => r.ok)) shouldBe true
    }
  }

  "Calls to Save  SubscriptionData" should {
    "successfully save that SubscriptionData" in {
      val result: Future[WriteResult] = subscriptionDataRepository.save(exampleSubscriptionData)
      await(result.map(r => r.ok)) shouldBe true
    }
  }

  "Calls to get Subscription Details" should {
    "successfully fetch that Subscription details using safeId" in {
      val result: OptionT[Future, SubscriptionDetails] = subscriptionDataRepository.get("MySafeID")
      await(result.value.map(r => r.get.businessPartnerRecord.safeId)) shouldBe "MySafeID"
    }
  }

  "Calls to get  Subscription Details" should {
    "successfully fetch that Subscription details using utr" in {
      val result: OptionT[Future, SubscriptionDetails] = subscriptionDataRepository.get(utr)
      await(result.value.map(r => r.get.utr)) shouldBe utr
    }
  }

  "Calls to get  Subscription Details" should {
    "successfully fetch that Subscription details using cbcId" in {
      val result: OptionT[Future, SubscriptionDetails] = subscriptionDataRepository.get(cbcId.value)
      await(result.value.map(r => r.get.cbcId.get.value)) shouldEqual cbcId.get.value
    }
  }

  "Calls to get  checkNumberOfCbcIdForUtr Details" should {
    "successfully fetch that checkNumberOfCbcIdForUtr details using utr" in {
      val result: Future[Int] = subscriptionDataRepository.checkNumberOfCbcIdForUtr("7000000003")
      await(result) shouldBe cbcId.size
    }
  }

  "Calls to  getSubscriptions Details" should {
    "successfully fetch that getSubscriptions details " in {
      val modifier = Json.obj("subscriberContact" -> Json.toJson(subscriberContact))
      val result: Future[List[SubscriptionDetails]] = subscriptionDataRepository.getSubscriptions(modifier)
      await(result.map(x => x.apply(0).subscriberContact.email.value)) shouldBe "dave@dave.com"
    }
  }

  "Calls to  backup data" should {
    "successfully backup details " in {
      val result: List[Future[WriteResult]] = subscriptionDataRepository.backup(List(exampleSubscriptionData))
      result.length >= (1)
    }
  }

  "Calls to update  subscription repository using subscriber Contact" should {
    "successfully update that Subscription details using subscriber Contact" in {
      val modifier = Json.obj("subscriberContact" -> Json.toJson(subscriberContact))
      val result: Future[Boolean] = subscriptionDataRepository.update(modifier, subscriberContact1)
      await(result) shouldBe true
    }
  }

  "Calls to update  subscription repository using country code" should {
    "should successfully update that Subscription details using country code" in {
      val modifier = Json.obj("businessPartnerRecord.address.countryCode" -> "GB")
      val result: Future[Boolean] = subscriptionDataRepository.update(modifier, CountryCode("FR"))
      await(result) shouldBe true
    }
  }
}
