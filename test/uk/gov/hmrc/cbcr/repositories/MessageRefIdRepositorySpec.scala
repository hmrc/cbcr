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

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.cbcr.controllers.MockAuth
import uk.gov.hmrc.cbcr.models.MessageRefId
import uk.gov.hmrc.cbcr.util.UnitSpec

import scala.concurrent.Future

class MessageRefIdRepositorySpec extends UnitSpec with MockAuth with GuiceOneAppPerSuite {

  private val messageRefIdRepository = app.injector.instanceOf[MessageRefIdRepository]

  "Calls to Save  MessageRefId" should {
    "successfully save that MessageRefId" in {

      val result = messageRefIdRepository.save2(MessageRefId("mRId"))
      await(result).wasAcknowledged() shouldBe true

    }
  }

  "Calls to check existence of a MessageRefId" should {
    "return available response for that MessageRefId" in {

      val result: Future[Boolean] = messageRefIdRepository.exists("mRId")
      await(result) shouldBe true

    }
  }
  "Calls to check existence of a MessageRefId" should {
    "that does not exist should return false for that MessageRefId" in {

      val result: Future[Boolean] = messageRefIdRepository.exists("mRId1")
      await(result) shouldBe false

    }
  }

  "Calls to delete a MessageRefId which exists" should {
    "delete that MessageRefId" in {

      val result = messageRefIdRepository.delete(MessageRefId("mRId"))
      await(result).wasAcknowledged() shouldBe true

    }
  }

}
