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
import cats.data.{EitherT, OptionT}
import cats.instances.future._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.http.Status
import play.api.libs.json.Json._
import play.api.libs.json.{JsObject, JsValue}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.cbcr.models.{InvalidState, _}
import uk.gov.hmrc.cbcr.repositories.GenericRepository
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by max on 04/04/17.
  */
class SubscriptionDataControllerSpec extends UnitSpec with MockitoSugar {


  val store: GenericRepository[SubscriptionData] = mock[GenericRepository[SubscriptionData]]

  val exampleSubscriptionData = SubscriptionData("name", "position", "email", "cbcId")

  val controller = new SubscriptionDataController()(store)

  val fakePostRequest: FakeRequest[JsValue] = FakeRequest(Helpers.POST, "/saveSubscriptionData").withBody(toJson(exampleSubscriptionData))

  val fakeGetRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(Helpers.GET, "/retrieveSubscriptionData")


  implicit val as = ActorSystem()
  implicit val mat = ActorMaterializer()

  "The SubscriptionDataController" should {
    "respond with a 200 when asked to store SubscriptionData" in {
      when(store.save(any(classOf[SubscriptionData]))).thenReturn(EitherT.pure[Future,InvalidState,DbOperationResult](UpdateSuccess))
      val result  = controller.saveSubscriptionData()(fakePostRequest)
      status(result) shouldBe Status.OK
    }

    "respond with a 500 if there is a DB failure" in {
      when(store.save(any(classOf[SubscriptionData]))).thenReturn(EitherT.pure[Future,InvalidState,DbOperationResult](UpdateFailed))
      val result  = controller.saveSubscriptionData()(fakePostRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "respond with a 200 and a SubscriptionData when asked to retrieve an existing CBCID" in {
      when(store.retrieve(any(classOf[JsObject]))).thenReturn(OptionT.pure[Future,SubscriptionData](exampleSubscriptionData))
      val result  = controller.retrieveSubscriptionData("cbcId")(fakeGetRequest)
      status(result) shouldBe Status.OK
      jsonBodyOf(result).validate[SubscriptionData].isSuccess shouldBe true
    }

    "respond with a 404 when asked to retrieve a non-existent CBCID" in {
      when(store.retrieve(any(classOf[JsObject]))).thenReturn(OptionT.fromOption[Future](None:Option[SubscriptionData]))
      val result  = controller.retrieveSubscriptionData("cbcId")(fakeGetRequest)
      status(result) shouldBe Status.NOT_FOUND
    }

  }
}
