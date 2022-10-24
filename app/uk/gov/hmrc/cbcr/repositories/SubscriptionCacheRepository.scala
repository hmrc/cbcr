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
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{FindOneAndReplaceOptions, IndexModel, IndexOptions}
import uk.gov.hmrc.cbcr.models.subscription.request.CreateSubscriptionForCBCRequest
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.LocalDateTime
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubscriptionCacheRepository @Inject()(implicit mongo: MongoComponent, ec: ExecutionContext)
    extends PlayMongoRepository[CreateSubscriptionForCBCRequest](
      mongoComponent = mongo,
      collectionName = "subscriptionCacheRepository",
      domainFormat = CreateSubscriptionForCBCRequest.format,
      indexes = Seq(
        IndexModel(ascending("lastUpdated"), IndexOptions().name("subscription-last-updated-index").unique(true))
      )
    ) {

  def get(id: String): Future[Option[CreateSubscriptionForCBCRequest]] =
    collection.find(equal("_id", id)).headOption

  def set(id: String, subscription: CreateSubscriptionForCBCRequest): Future[Boolean] =
    collection
      .findOneAndReplace(
        equal("_id", id),
        subscription copy (lastUpdated = LocalDateTime.now),
        FindOneAndReplaceOptions().upsert(true)
      )
      .headOption
      .map(_.isDefined)

}
