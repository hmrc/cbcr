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

import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.{Filters, FindOneAndUpdateOptions, IndexModel, IndexOptions}
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.result.DeleteResult
import play.api.libs.json._
import uk.gov.hmrc.cbcr.models.upscan._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UploadSessionRepository @Inject()(backupRepo: BackupSubscriptionDataRepository)(
  implicit mongo: MongoComponent,
  ec: ExecutionContext)
    extends PlayMongoRepository[UploadSessionDetails](
      mongoComponent = mongo,
      collectionName = "uploadSessionRepository",
      domainFormat = UploadSessionDetails.format,
      indexes = Seq(
        IndexModel(ascending("uploadId"), IndexOptions().name("Upload Id").unique(true))
      )
    ) {

  def findByUploadId(uploadId: UploadId): Future[Option[UploadSessionDetails]] =
    collection.find(equal("uploadId", Json.toJson(uploadId))).headOption()

  def insert(sessionDetails: UploadSessionDetails): Future[Boolean] =
    collection.insertOne(sessionDetails).head.map(_.wasAcknowledged())

  def updateStatus(reference: Reference, newStatus: UploadStatus): Future[Boolean] =
    collection
      .findOneAndUpdate(
        equal("reference", Json.toJson(reference)),
        set("status", Json.toJson(newStatus)),
        FindOneAndUpdateOptions().upsert(true)
      )
      .headOption()
      .map(_.isDefined)

  def removeAll(): Future[DeleteResult] = collection.deleteMany(Filters.empty()).toFuture()

}
