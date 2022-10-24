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

import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BackupSubscriptionDataRepository @Inject()(implicit rmc: ReactiveMongoComponent)
    extends ReactiveRepository[SubscriptionDetails, BSONObjectID](
      "Subscription_Data_Backup",
      rmc.mongoConnector.db,
      SubscriptionDetails.format,
      ReactiveMongoFormats.objectIdFormats) {}

@Singleton
class SubscriptionDataRepository @Inject()(backupRepo: BackupSubscriptionDataRepository)(
  implicit rmc: ReactiveMongoComponent)
    extends ReactiveRepository[SubscriptionDetails, BSONObjectID](
      "Subscription_Data",
      rmc.mongoConnector.db,
      SubscriptionDetails.format,
      ReactiveMongoFormats.objectIdFormats) {

  override def indexes: List[Index] = List(
    Index(Seq("cbcId" -> Ascending), Some("CBCId Index"), unique = true),
    Index(Seq("utr"   -> Ascending), Some("Utr Index"), unique = true),
  )

  def clearCBCId(cbcId: CBCId)(implicit ec: ExecutionContext): Future[WriteResult] = remove("cbcId" -> cbcId.value)

  def removeAll()(implicit ec: ExecutionContext): Future[WriteResult] = remove()

  def clear(utr: Utr)(implicit ec: ExecutionContext): Future[WriteResult] = remove("utr" -> utr.utr)

  def update(criteria: JsObject, s: SubscriberContact)(implicit ec: ExecutionContext): Future[Boolean] =
    findAndUpdate(
      criteria,
      Json.obj("$set" -> Json.obj("subscriberContact" -> Json.toJson(s)))
    ).map(_.value.isDefined)

  def update(criteria: JsObject, cc: CountryCode)(implicit ec: ExecutionContext): Future[Boolean] =
    findAndUpdate(
      criteria,
      Json.obj("$set" -> Json.obj("businessPartnerRecord.address.countryCode" -> cc))
    ).map(_.value.isDefined)

  def save2(s: SubscriptionDetails)(implicit ec: ExecutionContext): Future[WriteResult] = insert(s)

  def backup(s: List[SubscriptionDetails])(implicit ec: ExecutionContext): Future[List[WriteResult]] =
    Future.sequence(s.map(sd => backupRepo.insert(sd)))

  def get(safeId: String)(implicit ec: ExecutionContext): Future[Option[SubscriptionDetails]] =
    find("businessPartnerRecord.safeId" -> safeId).map(_.headOption)

  def get(cbcId: CBCId)(implicit ec: ExecutionContext): Future[Option[SubscriptionDetails]] =
    find("cbcId" -> cbcId.value).map(_.headOption)

  def get(utr: Utr)(implicit ec: ExecutionContext): Future[Option[SubscriptionDetails]] =
    find("utr" -> utr.utr).map(_.headOption)

  def getSubscriptions(query: (String, JsValueWrapper)*)(
    implicit ec: ExecutionContext): Future[List[SubscriptionDetails]] =
    find(query: _*)

  def checkNumberOfCbcIdForUtr(utr: String)(implicit ec: ExecutionContext): Future[Int] =
    count(Json.obj("utr" -> utr))

}
