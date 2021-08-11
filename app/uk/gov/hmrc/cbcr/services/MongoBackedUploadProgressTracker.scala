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

package uk.gov.hmrc.cbcr.services

import org.slf4j.LoggerFactory
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.cbcr.models.upscan._
import uk.gov.hmrc.cbcr.repositories.UploadSessionRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait UploadProgressTracker {

  def requestUpload(uploadId: UploadId, fileReference: Reference): Future[Boolean]

  def registerUploadResult(reference: Reference, uploadStatus: UploadStatus): Future[Boolean]

  def getUploadResult(id: UploadId): Future[Option[UploadStatus]]

}

class MongoBackedUploadProgressTracker @Inject()(repository: UploadSessionRepository)(implicit ec: ExecutionContext)
    extends UploadProgressTracker {
  private val logger = LoggerFactory.getLogger(getClass)

  override def requestUpload(uploadId: UploadId, fileReference: Reference): Future[Boolean] =
    repository.insert(UploadSessionDetails(BSONObjectID.generate(), uploadId, fileReference, InProgress))

  override def registerUploadResult(fileReference: Reference, uploadStatus: UploadStatus): Future[Boolean] = {
    logger.debug("In the register " + fileReference.toString + "   " + uploadStatus.toString)
    repository.updateStatus(fileReference, uploadStatus)
  }

  override def getUploadResult(id: UploadId): Future[Option[UploadStatus]] = {
    logger.debug("Getting the upload result from the database")
    for (result <- repository.findByUploadId(id)) yield {
      result map { x =>
        logger.debug("The status is " + x.status)
        x.status
      }
    }
  }

}
