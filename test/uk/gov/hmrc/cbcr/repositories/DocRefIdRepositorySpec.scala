/*
 * Copyright 2019 HM Revenue & Customs
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
import org.scalatestplus.play.OneAppPerSuite
import play.api.Configuration
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.{DefaultWriteResult, WriteResult}
import uk.gov.hmrc.cbcr.controllers.MockAuth
import uk.gov.hmrc.cbcr.models.{CorrDocRefId, DocRefId}
import uk.gov.hmrc.cbcr.models.DocRefIdResponses.{AlreadyExists, DocRefIdQueryResponse, DocRefIdSaveResponse, DoesNotExist, Invalid, Ok, Valid}
import uk.gov.hmrc.cbcr.util.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class DocRefIdRepositorySpec extends UnitSpec with MockAuth with OneAppPerSuite  with BeforeAndAfterAll {

  val config                  = app.injector.instanceOf[Configuration]
  implicit val ec             = app.injector.instanceOf[ExecutionContext]
  implicit val hc             = HeaderCarrier()
  val writeResult             = DefaultWriteResult(true,1,Seq.empty,None,None,None)
  val notFoundWriteResult     = DefaultWriteResult(true,0,Seq.empty,None,None,None)
  lazy val reactiveMongoApi   = app.injector.instanceOf[ReactiveMongoApi]
  val docRefId                = DocRefId("docRefId-SaveTest")
  val corrRefId               = CorrDocRefId(new DocRefId("corrRefId-SaveTest"))
  val docRefIdRepository      = new DocRefIdRepository(reactiveMongoApi)


  "Calls to edit a DocRefId" should {
    "should successfully edit that docRefId" in {

      val result: Future[Int] = docRefIdRepository.edit(docRefId)
      await(result) shouldBe 0

    }
  }

  "Calls to delete a DocRefId" should {
    "should delete that docRefId" in {

      val result: Future[WriteResult] = docRefIdRepository.delete(docRefId)
      await(result.map(r => r.ok)) shouldBe true

    }
  }


  "Calls to save a DocRefId" should {
    "should successfully create a new doc if it does not exist" in {

      val criteria = Json.obj("id" -> docRefId.id)
      val result: Future[DocRefIdSaveResponse] = docRefIdRepository.save(docRefId)
      await(result) shouldBe Ok

    }

    "should return AlreadyExists because its created in the above step" in {

      val criteria = Json.obj("id" -> docRefId.id)
      val result: Future[DocRefIdSaveResponse] = docRefIdRepository.save(docRefId)
      await(result) shouldBe AlreadyExists

    }
  }

  "Calls to query" should {
    "should check for Valid docRefId" in {

      val criteria = Json.obj("id" -> docRefId.id)
      val result: Future[DocRefIdQueryResponse] = docRefIdRepository.query(docRefId)
      await(result) shouldBe Valid

    }

    "should check for the absence of that doc when it does not exist in database" in {

      val result: Future[DocRefIdQueryResponse] = docRefIdRepository.query(new DocRefId("docRefId-that-Never-Exists"))
      await(result) shouldBe DoesNotExist

    }

  }

  "Calls to save" should {

      "should delete the corrDocRefId if it exists" in {

        val result: Future[WriteResult] = docRefIdRepository.delete(DocRefId(corrRefId.cid.id))
        await(result.map(r => r.ok)) shouldBe true

      }

      "should delete the doesNotExistYet if it exists" in {

        val result: Future[WriteResult] = docRefIdRepository.delete(DocRefId("doesNotExistYet"))
        await(result.map(r => r.ok)) shouldBe true

      }

    "should return (DoesNotExist,None) because corrDocRefId exists" in {

      val criteria = Json.obj("id" -> docRefId.id)
      val result: Future[(DocRefIdQueryResponse,Option[DocRefIdSaveResponse])] = docRefIdRepository.save(corrRefId, docRefId)

      await(result) shouldBe (DoesNotExist,None)

    }



    "should now create a correDocRefId" in {

      val result: Future[DocRefIdSaveResponse] = docRefIdRepository.save(DocRefId(corrRefId.cid.id))
      await(result) shouldBe Ok

    }

    "should return Valid and Some(AlreadyExists) because corrDocRefId exists" in {

      val criteria = Json.obj("id" -> docRefId.id)
      val result: Future[(DocRefIdQueryResponse,Option[DocRefIdSaveResponse])] = docRefIdRepository.save(corrRefId, docRefId)

      await(result) shouldBe (Valid,Some(AlreadyExists))

    }


    "should return Valid and Some(***) because corrDocRefId exists" in {

      val result: Future[(DocRefIdQueryResponse,Option[DocRefIdSaveResponse])] = docRefIdRepository.save(corrRefId, DocRefId("doesNotExistYet"))

      await(result) shouldBe (Valid,Some(Ok))

    }

    "should now return Invalid and None" in {

      val result: Future[(DocRefIdQueryResponse,Option[DocRefIdSaveResponse])] = docRefIdRepository.save(corrRefId, DocRefId("doesNotExistYet"))

      await(result) shouldBe (Invalid,None)

    }




  }



}
