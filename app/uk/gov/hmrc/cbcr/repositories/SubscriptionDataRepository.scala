/*
 * Copyright 2019 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import cats.data.OptionT
import cats.instances.future._
import play.api.Logger
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.{JsObject, JsValue, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor
import reactivemongo.api.commands.{DefaultWriteResult, WriteResult}
import reactivemongo.api.indexes.{CollectionIndexesManager, Index}
import reactivemongo.api.indexes.IndexType.{Ascending, Text}
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import reactivemongo.play.json.collection.{Helpers, JSONCollection}
import reactivemongo.play.json.commands.JSONFindAndModifyCommand
import uk.gov.hmrc.cbcr.models._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


@Singleton
class SubscriptionDataRepository @Inject() (protected val mongo: ReactiveMongoApi)(implicit ec:ExecutionContext) extends IndexBuilder {

  override protected val collectionName: String = "Subscription_Data"
  override protected val cbcIndexes: List[CbcIndex] = List(CbcIndex("CBCId Index", "cbcId"), CbcIndex("Utr Index", "utr"))

  val cbcIndexName = "CBCId Index"
  val utrIndexName = "Utr Index"

  val repository: Future[JSONCollection] =
    mongo.database.map(_.collection[JSONCollection]("Subscription_Data"))

  private val backupRepo: Future[JSONCollection] =
    mongo.database.map(_.collection[JSONCollection]("Subscription_Data_Backup"))

  def clearCBCId(cbcId:CBCId): OptionT[Future,WriteResult] = {
    val criteria = Json.obj("cbcId" -> cbcId.value)
    for {
      repo   <- OptionT.liftF(repository)
      result <- OptionT.liftF(repo.delete().one(criteria, Some(1)))//.remove(criteria, firstMatchOnly = true))
    } yield result
  }

  def clear(utr:Utr): Future[WriteResult] = {
    val criteria = Json.obj("utr" -> utr.utr)
    for {
      repo   <- repository
      result <- repo.delete().one(criteria, Some(1))
    } yield result
  }

  def update(criteria: JsObject,s: SubscriberContact): Future[Boolean] = {
    val modifier = Json.obj("$set" -> Json.obj("subscriberContact" -> Json.toJson(s)))
    for {
      collection <- repository
      update     <- collection.findAndModify(criteria, JSONFindAndModifyCommand.Update(modifier))
    } yield update.value.isDefined
  }

  def update(criteria: JsObject, cc: CountryCode): Future[Boolean] = {
    val modifier = Json.obj("$set" -> Json.obj("businessPartnerRecord.address.countryCode" -> cc))
    for {
      collection <- repository
      update     <- collection.findAndModify(criteria, JSONFindAndModifyCommand.Update(modifier))
    } yield update.value.isDefined
  }

  def save(s:SubscriptionDetails) : Future[WriteResult] =
    repository.flatMap(_.insert(s))

  def backup(s:List[SubscriptionDetails]) : List[Future[WriteResult]] =
    s.map(sd => backupRepo.flatMap(_.insert[SubscriptionDetails](sd)))

  def get(safeId:String) : OptionT[Future,SubscriptionDetails] =
    getGeneric(Json.obj("businessPartnerRecord.safeId" -> safeId))

  def get(cbcId:CBCId) : OptionT[Future,SubscriptionDetails] =
    getGeneric(Json.obj("cbcId" -> cbcId.value))

  def get(utr:Utr): OptionT[Future,SubscriptionDetails] =
    getGeneric(Json.obj("utr" -> utr.utr))

  def getSubscriptions(criteria: JsObject): Future[List[SubscriptionDetails]] = {
    repository.flatMap(_.find(criteria, None)
      .cursor[SubscriptionDetails]()
      .collect[List](-1, Cursor.FailOnError[List[SubscriptionDetails]]())
    )
  }

 private def getGeneric(criteria:JsObject) =
    OptionT(repository.flatMap(_.find(criteria, None).one[SubscriptionDetails]))


  def checkNumberOfCbcIdForUtr(utr: String):Future[Int] = {
    val utrRecord = Json.obj("utr" -> utr)

    repository.flatMap(_.find(utrRecord, None)
        .cursor[SubscriptionDetails]()
        .collect[List](-1, Cursor.FailOnError[List[SubscriptionDetails]]())).map(_.size)
  }

}
