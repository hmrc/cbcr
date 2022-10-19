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

import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.cbcr.models.subscription.request.CreateSubscriptionForCBCRequest
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import java.time.LocalDateTime
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubscriptionCacheRepository @Inject()(implicit rmc: ReactiveMongoComponent)
    extends ReactiveRepository[CreateSubscriptionForCBCRequest, BSONObjectID](
      "subscriptionCacheRepository",
      rmc.mongoConnector.db,
      CreateSubscriptionForCBCRequest.format,
      ReactiveMongoFormats.objectIdFormats) {

  override def indexes: List[Index] = List(
    Index(Seq("lastUpdated" -> Ascending), Some("subscription-last-updated-index"), unique = true)
  )

  def get(id: String)(implicit ec: ExecutionContext): Future[Option[CreateSubscriptionForCBCRequest]] =
    find("_id" -> id).map(_.headOption)

  def set(id: String, subscription: CreateSubscriptionForCBCRequest)(implicit ec: ExecutionContext): Future[Boolean] =
    findAndUpdate(
      Json.obj("_id"  -> id),
      Json.obj("$set" -> (subscription copy (lastUpdated = LocalDateTime.now))),
      upsert = true
    ).map(_.value.isDefined)

}
