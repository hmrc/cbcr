/*
 * Copyright 2022 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.services.EmailService
import uk.gov.hmrc.cbcr.util.UnitSpec
import play.api.http.Status
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.mvc.Results.Accepted

class CBCREmailControllerSpec extends UnitSpec with ScalaFutures with MockAuth {

  val paramsSub = Map("f_name" → "Tyrion", "s_name" → "Lannister", "cbcrId" -> "XGCBC0000000001")
  val correctEmail: Email = Email(List("tyrion.lannister@gmail.com"), "cbcr_subscription", paramsSub)
  val mockEmailService = mock[EmailService]
  val cbcrEmailController = new CBCREmailController(mockEmailService, cBCRAuth, cc)

  "The CBCREmailController" should {
    "return a 202 for a valid rest call" in {
      val fakeRequestSubscribe = FakeRequest("POST", "/email").withBody(Json.toJson(correctEmail))
      when(mockEmailService.sendEmail(any())(any())) thenReturn Future.successful(Accepted)

      val response = cbcrEmailController.sendEmail()(fakeRequestSubscribe)
      status(response) shouldBe Status.ACCEPTED
    }
    "return a 400 for a call with invalid email" in {
      val fakeRequestSubscribe = FakeRequest("POST", "/email").withBody(Json.obj("bad" -> "request"))

      val response = cbcrEmailController.sendEmail()(fakeRequestSubscribe)
      status(response) shouldBe Status.BAD_REQUEST
    }
  }
}
