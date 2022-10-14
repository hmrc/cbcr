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

import akka.actor.ActorSystem
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status
import play.api.test.{FakeRequest, Helpers}
import reactivemongo.api.commands.{UpdateWriteResult, WriteError}
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.repositories.MessageRefIdRepository
import uk.gov.hmrc.cbcr.util.UnitSpec

import scala.concurrent.Future

class MessageRefIdControllerSpec extends UnitSpec with ScalaFutures with MockAuth {

  val okResult = UpdateWriteResult(true, 0, 1, Seq.empty, Seq.empty, None, None, None)

  val failResult = UpdateWriteResult(false, 1, 1, Seq.empty, Seq(WriteError(1, 1, "Error")), None, None, Some("Error"))

  val fakePutRequest = FakeRequest(Helpers.PUT, "/messageRefId/myRefIDxx")

  val fakeGetRequest = FakeRequest(Helpers.GET, "/messageRefId/myRefIDxx")
  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  implicit val as = ActorSystem()

  val repo = mock[MessageRefIdRepository]

  val controller = new MessageRefIdController(repo, cBCRAuth, cc)

  "The MessageRefIdController" should {
    "respond with a 200 when asked to save a MessageRefId" in {
      when(repo.save2(any(classOf[MessageRefId]))(any())).thenReturn(Future.successful(okResult))
      val result = controller.save("messagerefid")(fakePutRequest)
      status(result) shouldBe Status.OK
    }

    "respond with a 500 if there is a DB failure" in {
      when(repo.save2(any(classOf[MessageRefId]))(any())).thenReturn(Future.successful(failResult))
      val result = controller.save("messagerefid")(fakePutRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "respond with a 200 when asked to retrieve an existing messageRefId" in {
      when(repo.exists(any(classOf[String]))(any())).thenReturn(Future.successful(true))
      val result = controller.exists("messageRefId")(fakeGetRequest)
      status(result) shouldBe Status.OK
    }

    "respond with a 404 when asked to retrieve a non-existent messageRefId" in {
      when(repo.exists(any(classOf[String]))(any())).thenReturn(Future.successful(false))
      val result = controller.exists("messageRefId")(fakeGetRequest)
      status(result) shouldBe Status.NOT_FOUND
    }

  }

}
