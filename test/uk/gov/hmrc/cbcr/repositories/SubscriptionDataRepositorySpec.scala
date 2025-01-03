/*
 * Copyright 2023 HM Revenue & Customs
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

import org.mongodb.scala.model.Filters
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.cbcr.controllers.MockAuth
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.util.UnitSpec
import uk.gov.hmrc.cbcr.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

class SubscriptionDataRepositorySpec extends UnitSpec with MockAuth with GuiceOneAppPerSuite {

  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  implicit val hc: HeaderCarrier = HeaderCarrier()
  private val subscriptionDataRepository = app.injector.instanceOf[SubscriptionDataRepository]
  private val subscriberContact =
    SubscriberContact(name = None, "Dave", "Jones", PhoneNumber("02072653787").get, EmailAddress("dave@dave.com"))
  private val utr = Utr("7000000003")
  private val cbcId = CBCId("XGCBC0000000001")
  private val bpr = BusinessPartnerRecord(
    "MySafeID",
    Some(OrganisationResponse("Dave Corp")),
    EtmpAddress("13 Accacia Ave", None, None, None, None, "GB")
  )
  private val exampleSubscriptionData = SubscriptionDetails(
    bpr,
    SubscriberContact(name = None, "Dave", "Jones", PhoneNumber("02072653787").get, EmailAddress("dave@dave.com")),
    cbcId,
    utr
  )

  "Calls to clear cbcId Details" should {
    "successfully clear  details using cbcId" in {
      val result = subscriptionDataRepository.clearCBCId(cbcId.value)
      await(result).wasAcknowledged() shouldBe true
    }
  }

  "Calls to clear  " should {
    "should successfully clear all data" in {
      val result = subscriptionDataRepository.clear(utr)
      await(result).wasAcknowledged() shouldBe true
    }
  }

  "Calls to Save  SubscriptionData" should {
    "successfully save that SubscriptionData" in {
      val result = subscriptionDataRepository.save2(exampleSubscriptionData)
      await(result).wasAcknowledged() shouldBe true
    }
  }

  "Calls to get Subscription Details" should {
    "successfully fetch that Subscription details using safeId" in {
      val result = subscriptionDataRepository.get("MySafeID")
      await(result).get.businessPartnerRecord.safeId shouldBe "MySafeID"
    }
  }

  "Calls to get  Subscription Details" should {
    "successfully fetch that Subscription details using utr" in {
      val result = subscriptionDataRepository.get(utr)
      await(result).get.utr shouldBe utr
    }
  }

  "Calls to get  Subscription Details" should {
    "successfully fetch that Subscription details using cbcId" in {
      val result = subscriptionDataRepository.get(cbcId.value)
      await(result).value.cbcId.get.value shouldEqual cbcId.get.value
    }
  }

  "Calls to get  checkNumberOfCbcIdForUtr Details" should {
    "successfully fetch that checkNumberOfCbcIdForUtr details using utr" in {
      val result = subscriptionDataRepository.checkNumberOfCbcIdForUtr("7000000003")
      await(result) shouldBe cbcId.size.toLong
    }
  }

  "Calls to  getSubscriptions Details" should {
    "successfully fetch that getSubscriptions details " in {
      val result = subscriptionDataRepository.getSubscriptions(Filters.equal("subscriberContact", subscriberContact))
      await(result).head.subscriberContact.email.value shouldBe "dave@dave.com"
    }
  }

  "Calls to  backup data" should {
    "successfully backup details " in {
      val result = subscriptionDataRepository.backup(List(exampleSubscriptionData))
      await(result).wasAcknowledged() shouldBe true
    }
  }
}
