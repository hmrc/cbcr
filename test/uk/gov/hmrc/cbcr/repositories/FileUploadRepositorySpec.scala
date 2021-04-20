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

package uk.gov.hmrc.cbcr.repositories

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.WriteResult
import uk.gov.hmrc.cbcr.controllers.MockAuth
import uk.gov.hmrc.cbcr.models.UploadFileResponse
import uk.gov.hmrc.cbcr.util.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class FileUploadRepositorySpec extends UnitSpec with MockAuth with GuiceOneAppPerSuite {

  val config = app.injector.instanceOf[Configuration]
  implicit val ec = app.injector.instanceOf[ExecutionContext]
  implicit val hc = HeaderCarrier()
  lazy val reactiveMongoApi = app.injector.instanceOf[ReactiveMongoApi]
  val fileUploadRepository = new FileUploadRepository(reactiveMongoApi)
  val fir = UploadFileResponse("id1", "fid1", "status", None)

  "Calls to Save  UploadFileResponse" should {
    "should successfully save that UploadFileResponse" in {

      val result: Future[WriteResult] = fileUploadRepository.save(fir)
      await(result.map(r => r.ok)) shouldBe true

    }
  }

  "Calls to get a EnvelopId" should {
    "should successfully fetch that envelopId" in {

      val result: Future[Option[UploadFileResponse]] = fileUploadRepository.get("id1")
      await(result.map(r => r.get.envelopeId)) shouldBe "id1"

    }
  }

  "Calls to get a EnvelopId which does not exist" should {
    "should not fetch that envelopId" in {

      val result: Future[Option[UploadFileResponse]] = fileUploadRepository.get("envelopeId")
      await(result.map(r => r)) shouldBe None

    }
  }

}
