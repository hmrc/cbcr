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

import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import uk.gov.hmrc.cbcr.controllers.MockAuth
import uk.gov.hmrc.cbcr.models.FileUploadResponse
import uk.gov.hmrc.cbcr.util.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

class FileUploadRepositorySpec extends UnitSpec with MockAuth with GuiceOneAppPerSuite with BeforeAndAfterEach {

  val config: Configuration = app.injector.instanceOf[Configuration]
  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val fileUploadRepository: FileUploadRepository = app.injector.instanceOf[FileUploadRepository]
  val fur: FileUploadResponse = FileUploadResponse("id1", "fid1", "status", None)

  override def beforeEach(): Unit =
    await(fileUploadRepository.collection.drop().toFuture())

  "saving a FileUploadResponse" should {
    "return None (because the collection is empty)" in {
      val result = await(fileUploadRepository.save2(fur))
      result shouldEqual None
    }
    "insert the object into the repository" in {
      await(fileUploadRepository.save2(fur))
      val inserted = await(fileUploadRepository.get(fur.envelopeId))
      inserted shouldEqual Some(fur)
    }
  }

  "saving an existing FileUploadResponse" should {
    "return the original object" in {
      await(fileUploadRepository.save2(fur))
      val modified = fur.copy(status = "AVAILABLE")
      val original = await(fileUploadRepository.save2(modified))
      original shouldEqual Some(fur)
    }
    "override the object" in {
      await(fileUploadRepository.save2(fur))
      val modified = fur.copy(status = "AVAILABLE")
      await(fileUploadRepository.save2(modified))
      val inserted = await(fileUploadRepository.get(fur.envelopeId))
      inserted shouldEqual Some(modified)
    }
  }

  "saving a FileUploadResponse with a reason" should {
    "insert the object into the repository" in {
      val furWithReason = FileUploadResponse("id1", "fid1", "status", Some("reason"))
      await(fileUploadRepository.save2(furWithReason))

      val inserted = await(fileUploadRepository.get(fur.envelopeId))
      inserted shouldEqual Some(furWithReason)
    }
  }

  "Calls to get a EnvelopId which does not exist" should {
    "should not fetch that envelopId" in {
      val result = await(fileUploadRepository.get("envelopeId"))
      result shouldEqual None
    }
  }

}
