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

package uk.gov.hmrc.cbcr.repositories

import org.bson.types.ObjectId
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import uk.gov.hmrc.cbcr.controllers.MockAuth
import uk.gov.hmrc.cbcr.models.upscan.{Quarantined, Reference, UploadId, UploadSessionDetails}
import uk.gov.hmrc.cbcr.util.UnitSpec

import java.util.UUID
import scala.concurrent.Future

class UploadSessionRepositorySpec
    extends UnitSpec with MockAuth with GuiceOneAppPerSuite with ScalaFutures with IntegrationPatience
    with BeforeAndAfterEach {
  lazy val uploadRep = app.injector.instanceOf[UploadSessionRepository]
  val uploadId = UploadId(UUID.randomUUID().toString)
  val config = app.injector.instanceOf[Configuration]
  val uploadDetails = UploadSessionDetails(ObjectId.get(), uploadId, Reference("xxxx"), Quarantined)

  override def afterEach(): Unit = {
    await(uploadRep.removeAll())
    super.beforeEach()
  }

  "Insert" must {
    "insert UploadStatus" in {
      val res = uploadRep.insert(uploadDetails)
      await(res) // shouldn't throw
    }
    "read UploadStatus" in {
      await(uploadRep.insert(uploadDetails))
      val res: Future[Option[UploadSessionDetails]] = uploadRep.findByUploadId(uploadId)
      val result = res.futureValue.value
      result.uploadId shouldBe uploadDetails.uploadId
      result.reference shouldBe uploadDetails.reference
      result.status shouldBe uploadDetails.status
    }
  }
}
