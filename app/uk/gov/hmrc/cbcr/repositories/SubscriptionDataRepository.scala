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

import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions}
import org.mongodb.scala.result.{DeleteResult, InsertManyResult, InsertOneResult}
import play.api.Configuration
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import uk.gov.hmrc.play.http.logging.Mdc.preservingMdc

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BackupSubscriptionDataRepository @Inject() (mongo: MongoComponent)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[SubscriptionDetails](
      mongoComponent = mongo,
      collectionName = "Subscription_Data_Backup",
      domainFormat = SubscriptionDetails.format,
      extraCodecs = Seq(
        Codecs.playFormatCodec(SubscriberContact.formats)
      ),
      indexes = Seq()
    ) {}

@Singleton
class SubscriptionDataRepository @Inject() (mongo: MongoComponent, config: Configuration)(
  backupRepo: BackupSubscriptionDataRepository
)(implicit
  ec: ExecutionContext
) extends PlayMongoRepository[SubscriptionDetails](
      mongoComponent = mongo,
      collectionName = "Subscription_Data",
      domainFormat = SubscriptionDetails.format,
      extraCodecs = Seq(
        Codecs.playFormatCodec(SubscriberContact.formats)
      ),
      indexes = Seq(
        IndexModel(
          ascending("cbcId"),
          IndexOptions()
            .name("CBCId Index")
            .unique(true)
            .expireAfter(
              config.get[FiniteDuration]("mongodb.cache-ttl.expiry-time").toSeconds,
              TimeUnit.SECONDS
            )
        ),
        IndexModel(
          ascending("utr"),
          IndexOptions()
            .name("Utr Index")
            .unique(true)
            .expireAfter(
              config.get[FiniteDuration]("mongodb.cache-ttl.expiry-time").toSeconds,
              TimeUnit.SECONDS
            )
        )
      ),
      replaceIndexes = true
    ) {
  def clearCBCId(cbcId: CBCId): Future[DeleteResult] =
    preservingMdc {
      collection.deleteOne(equal("cbcId", cbcId.value)).toFuture()
    }

  def removeAll(): Future[DeleteResult] =
    preservingMdc {
      collection.deleteMany(Filters.empty()).toFuture()
    }

  def clear(utr: Utr): Future[DeleteResult] =
    preservingMdc {
      collection.deleteOne(equal("utr", utr.utr)).toFuture()
    }

  def update(criteria: Bson, s: SubscriberContact): Future[Boolean] =
    preservingMdc {
      collection
        .findOneAndUpdate(
          criteria,
          set("subscriberContact", s)
        )
        .headOption()
        .map(_.isDefined)
    }

  def update(criteria: Bson, cc: CountryCode): Future[Boolean] =
    preservingMdc {
      collection
        .findOneAndUpdate(
          criteria,
          set("businessPartnerRecord.address.countryCode", cc)
        )
        .headOption()
        .map(_.isDefined)
    }

  def save2(s: SubscriptionDetails): Future[InsertOneResult] =
    preservingMdc {
      collection.insertOne(s).toFuture()
    }

  def backup(s: List[SubscriptionDetails]): Future[InsertManyResult] =
    preservingMdc {
      backupRepo.collection.insertMany(s).toFuture()
    }

  def get(safeId: String): Future[Option[SubscriptionDetails]] =
    preservingMdc {
      collection.find(equal("businessPartnerRecord.safeId", safeId)).headOption()
    }

  def get(cbcId: CBCId): Future[Option[SubscriptionDetails]] =
    preservingMdc {
      collection.find(equal("cbcId", cbcId.value)).headOption()
    }

  def get(utr: Utr): Future[Option[SubscriptionDetails]] =
    preservingMdc {
      collection.find(equal("utr", utr.utr)).headOption()
    }

  def getSubscriptions(query: Bson): Future[Seq[SubscriptionDetails]] =
    preservingMdc {
      collection.find(query).toFuture()
    }

  def checkNumberOfCbcIdForUtr(utr: String): Future[Long] =
    preservingMdc {
      collection.countDocuments(equal("utr", utr)).toFuture()
    }
}
