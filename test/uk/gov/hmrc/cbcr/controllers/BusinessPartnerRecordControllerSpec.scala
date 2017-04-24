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

import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.cbcr.connectors.DESConnector
import uk.gov.hmrc.cbcr.models.{EtmpAddress, FindBusinessDataResponse}
import uk.gov.hmrc.play.http.HttpResponse
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by max on 24/04/17.
  */
class BusinessPartnerRecordControllerSpec extends UnitSpec with MockitoSugar {

  val dc = mock[DESConnector]
  val controller = new BusinessPartnerRecordController(dc)

  "The BusinessPartnerRecordController" should {
    "respond with a 200 if the UTR is matched" in {
      val response = FindBusinessDataResponse(false, None, None, Some("safeid"), EtmpAddress(None, None, None, None, Some("SW46NS"), None))
      val utr = "700000002"
      val fakeRequestSubscribe = FakeRequest("GET", "/getBusinessPartnerRecord")
      when(dc.lookup(utr)) thenReturn Future.successful(HttpResponse(Status.OK, Some(Json.toJson(response))))
      status(controller.getBusinessPartnerRecord(utr)(fakeRequestSubscribe)) shouldBe Status.OK
    }

    "respond with a 404 if the UTR is not found" in {
      val utr = "700000002"
      val fakeRequestSubscribe = FakeRequest("GET", "/getBusinessPartnerRecord")
      when(dc.lookup(utr)) thenReturn Future.successful(HttpResponse(Status.NOT_FOUND))
      status(controller.getBusinessPartnerRecord(utr)(fakeRequestSubscribe)) shouldBe Status.NOT_FOUND

    }
    "respond with a 500 if the DES service is unavailable" in {
      val utr = "700000002"
      val fakeRequestSubscribe = FakeRequest("GET", "/getBusinessPartnerRecord")
      when(dc.lookup(utr)) thenReturn Future.successful(HttpResponse(Status.INTERNAL_SERVER_ERROR))
      status(controller.getBusinessPartnerRecord(utr)(fakeRequestSubscribe)) shouldBe Status.INTERNAL_SERVER_ERROR

    }

  }


}
