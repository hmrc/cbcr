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

import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import uk.gov.hmrc.cbcr.models.upscan._
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UploadSessionRepository @Inject()(backupRepo: BackupSubscriptionDataRepository)(
  implicit rmc: ReactiveMongoComponent,
  ec: ExecutionContext)
    extends ReactiveRepository[UploadSessionDetails, BSONObjectID](
      "uploadSessionRepository",
      rmc.mongoConnector.db,
      UploadSessionDetails.format,
      ReactiveMongoFormats.objectIdFormats) {
  override def indexes: List[Index] = List(
    Index(Seq("uploadId" -> Ascending), Some("Upload Id"), unique = true)
  )

  def findByUploadId(uploadId: UploadId): Future[Option[UploadSessionDetails]] =
    find("uploadId" -> Json.toJson(uploadId)).map(_.headOption)

  def updateStatus(reference: Reference, newStatus: UploadStatus): Future[Boolean] = {
    implicit val referenceFormatter: OFormat[Reference] = Json.format[Reference]
    findAndUpdate(
      Json.obj("reference" -> Json.toJson(reference)),
      Json.obj("$set"      -> Json.obj("status" -> Json.toJson(newStatus))),
      upsert = true
    ).map(_.value.isDefined)
  }
}
