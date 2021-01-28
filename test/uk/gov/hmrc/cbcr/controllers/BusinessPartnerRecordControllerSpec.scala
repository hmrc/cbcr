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

package uk.gov.hmrc.cbcr.controllers

import org.mockito.ArgumentMatchers.{eq => EQ}
import org.mockito.Mockito._
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.cbcr.connectors.DESConnector
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.cbcr.util.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by max on 24/04/17.
  */
class BusinessPartnerRecordControllerSpec extends UnitSpec with MockAuth {

  val dc = mock[DESConnector]
  passAuthMock()
  val controller = new BusinessPartnerRecordController(dc, cBCRAuth, cc)

  "The BusinessPartnerRecordController" should {
    "respond with a 200 if the UTR is matched" in {
      val response = Json.obj(
        "safeId"               -> "XV0000100085686",
        "agentReferenceNumber" -> "BARN0000191",
        "isEditable"           -> false,
        "isAnAgent"            -> true,
        "isAnASAgent"          -> true,
        "isAnIndividual"       -> true,
        "individual" -> Json.obj(
          "firstName"   -> "First Name Taxpayer 01",
          "lastName"    -> "Last Name Taxpayer 01",
          "dateOfBirth" -> "1960-12-01"),
        "address" -> Json.obj(
          "addressLine1" -> "Matheson House 1",
          "addressLine2" -> "Grange Central",
          "addressLine3" -> "Telford",
          "addressLine4" -> "Shropshire",
          "countryCode"  -> "GB",
          "postalCode"   -> "TF3 4ER"
        ),
        "contactDetails" -> Json.obj()
      )
      val utr = "700000002"
      val fakeRequestSubscribe = FakeRequest("GET", "/getBusinessPartnerRecord")
      when(dc.lookup(EQ(utr))) thenReturn Future.successful(HttpResponse(Status.OK, Some(response)))
      status(controller.getBusinessPartnerRecord(utr)(fakeRequestSubscribe)) shouldBe Status.OK
    }

    "respond with a 404 if the UTR is not found" in {
      val utr = "700000002"
      val fakeRequestSubscribe = FakeRequest("GET", "/getBusinessPartnerRecord")
      when(dc.lookup(EQ(utr))) thenReturn Future.successful(HttpResponse(Status.NOT_FOUND))
      status(controller.getBusinessPartnerRecord(utr)(fakeRequestSubscribe)) shouldBe Status.NOT_FOUND

    }
    "respond with a 500 if the DES service is unavailable" in {
      val utr = "700000002"
      val fakeRequestSubscribe = FakeRequest("GET", "/getBusinessPartnerRecord")
      when(dc.lookup(EQ(utr))) thenReturn Future.successful(HttpResponse(Status.INTERNAL_SERVER_ERROR))
      status(controller.getBusinessPartnerRecord(utr)(fakeRequestSubscribe)) shouldBe Status.INTERNAL_SERVER_ERROR

    }
    "respond with a 400 if the DES service returns BAD_REQUEST" in {
      val utr = "700000002"
      val fakeRequestSubscribe = FakeRequest("GET", "/getBusinessPartnerRecord")
      when(dc.lookup(EQ(utr))) thenReturn Future.successful(HttpResponse(Status.BAD_REQUEST))
      status(controller.getBusinessPartnerRecord(utr)(fakeRequestSubscribe)) shouldBe Status.BAD_REQUEST

    }
  }

}
