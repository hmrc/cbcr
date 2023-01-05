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

import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import uk.gov.hmrc.cbcr.controllers.MockAuth
import uk.gov.hmrc.cbcr.models.{CorrDocRefId, DocRefId}
import uk.gov.hmrc.cbcr.models.DocRefIdResponses.{AlreadyExists, DocRefIdQueryResponse, DocRefIdSaveResponse, DoesNotExist, Invalid, Ok, Valid}
import uk.gov.hmrc.cbcr.util.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class DocRefIdRepositorySpec extends UnitSpec with MockAuth with GuiceOneAppPerSuite with BeforeAndAfterAll {

  val config = app.injector.instanceOf[Configuration]
  implicit val ec = app.injector.instanceOf[ExecutionContext]
  implicit val hc = HeaderCarrier()
  val docRefId = DocRefId("docRefId-SaveTest")
  val corrRefId = CorrDocRefId(new DocRefId("corrRefId-SaveTest"))
  val docRefIdRepository = app.injector.instanceOf[DocRefIdRepository]

  "Calls to edit a DocRefId" should {
    "should successfully edit that docRefId" in {

      val result = docRefIdRepository.edit(docRefId)
      await(result) shouldBe 0L

    }
  }

  "Calls to delete a DocRefId" should {
    "should delete that docRefId" in {

      val result = docRefIdRepository.delete(docRefId)
      await(result).wasAcknowledged() shouldBe true

    }
  }

  "Calls to save a DocRefId" should {
    "should successfully create a new doc if it does not exist" in {

      val result: Future[DocRefIdSaveResponse] = docRefIdRepository.save2(docRefId)
      await(result) shouldBe Ok

    }
    "should return AlreadyExists because its created in the above step" in {

      val result: Future[DocRefIdSaveResponse] = docRefIdRepository.save2(docRefId)
      await(result) shouldBe AlreadyExists

    }
  }

  "Calls to query" should {
    "should check for Valid docRefId" in {

      val result: Future[DocRefIdQueryResponse] = docRefIdRepository.query(docRefId)
      await(result) shouldBe Valid

    }
    "should check for the absence of that doc when it does not exist in database" in {

      val result: Future[DocRefIdQueryResponse] = docRefIdRepository.query(new DocRefId("docRefId-that-Never-Exists"))
      await(result) shouldBe DoesNotExist

    }

  }

  "Calls to delete" should {

    "should delete the corrDocRefId if it exists" in {

      val result = docRefIdRepository.delete(DocRefId(corrRefId.cid.id))
      await(result).wasAcknowledged() shouldBe true

    }
    "should delete the doesNotExistYet if it exists" in {

      val result = docRefIdRepository.delete(DocRefId("doesNotExistYet"))
      await(result).wasAcknowledged() shouldBe true

    }
  }

  "Calls to save" should {
    "should return (DoesNotExist,None) because corrDocRefId exists" in {

      val result: (DocRefIdQueryResponse, Option[DocRefIdSaveResponse]) =
        await(docRefIdRepository.save2(corrRefId, docRefId))
      result._1 shouldBe DoesNotExist
      result._2 shouldBe None

    }
    "should now create a correDocRefId" in {

      val result: Future[DocRefIdSaveResponse] = docRefIdRepository.save2(DocRefId(corrRefId.cid.id))
      await(result) shouldBe Ok

    }
    "should return Valid and Some(AlreadyExists) because corrDocRefId exists" in {

      val result: (DocRefIdQueryResponse, Option[DocRefIdSaveResponse]) =
        await(docRefIdRepository.save2(corrRefId, docRefId))
      result._1 shouldBe Valid
      result._2 shouldBe Some(AlreadyExists)

    }
    "should return Valid and Some(Ok) because corrDocRefId does not exist yet" in {

      val result: (DocRefIdQueryResponse, Option[DocRefIdSaveResponse]) =
        await(docRefIdRepository.save2(corrRefId, DocRefId("doesNotExistYet")))
      result._1 shouldBe Valid
      result._2 shouldBe Some(Ok)

    }
    "should now return Invalid and None because corrDocRefId exists now" in {

      val result: (DocRefIdQueryResponse, Option[DocRefIdSaveResponse]) =
        await(docRefIdRepository.save2(corrRefId, DocRefId("doesNotExistYet")))
      result._1 shouldBe Invalid
      result._2 shouldBe None

    }
  }

  "Calls to delete as a cleanup operation " should {

    "should delete the corrRefId-SaveTest if it exists finally" in {

      val result = docRefIdRepository.delete(DocRefId("corrRefId-SaveTest"))
      await(result).wasAcknowledged() shouldBe true

    }
    "should delete the docRefId-SaveTest if it exists finally" in {

      val result = docRefIdRepository.delete(DocRefId("docRefId-SaveTest"))
      await(result).wasAcknowledged() shouldBe true

    }
    "should delete the doesNotExistYet if it exists finally" in {

      val result = docRefIdRepository.delete(DocRefId("doesNotExistYet"))
      await(result).wasAcknowledged() shouldBe true

    }
  }

}
