/*
 * Copyright 2017 HM Revenue & Customs
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
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.{BSONSerializationPack, Cursor}
import reactivemongo.api.commands.{Command, WriteResult}
import reactivemongo.bson.{BSONArray, BSONDocument}
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json.commands.JSONFindAndModifyCommand
import uk.gov.hmrc.cbcr.models.{CBCId, SubscriberContact, SubscriptionDetails, Utr}

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class SubscriptionDataRepository @Inject() (private val mongo: ReactiveMongoApi)(implicit ec:ExecutionContext){

  val repository: Future[JSONCollection] =
    mongo.database.map(_.collection[JSONCollection]("Subscription_Data"))

  def clear(cbcId:CBCId): OptionT[Future,WriteResult] = {
    val criteria = Json.obj("cbcId" -> cbcId.value)
    for {
      repo   <- OptionT.liftF(repository)
      result <- OptionT.liftF(repo.remove(criteria, firstMatchOnly = true))
    } yield result
  }

  def clear(utr:Utr): Future[WriteResult] = {
    val criteria = Json.obj("utr" -> utr.utr)
    for {
      repo   <- repository
      result <- repo.remove(criteria, firstMatchOnly = true)
    } yield result
  }

  def update(cbcId:CBCId,s:SubscriberContact): Future[Boolean] = {
    val criteria = BSONDocument("cbcId" -> cbcId.value)
    val modifier = Json.obj("$set" -> Json.obj("subscriberContact" -> Json.toJson(s)))
    for {
      collection <- repository
      update     <- collection.findAndModify(criteria, JSONFindAndModifyCommand.Update(modifier))
    } yield update.value.isDefined
  }

  def save(s:SubscriptionDetails) : Future[WriteResult] =
    repository.flatMap(_.insert(s))

  def get(safeId:String) : OptionT[Future,SubscriptionDetails] =
    getGeneric(Json.obj("businessPartnerRecord.safeId" -> safeId))

  def get(cbcId:CBCId) : OptionT[Future,SubscriptionDetails] =
    getGeneric(Json.obj("cbcId" -> cbcId.value))

  def get(utr:Utr): OptionT[Future,SubscriptionDetails] =
    getGeneric(Json.obj("utr" -> utr.utr))

  def getSubscriptions(criteria: JsObject) = {
    repository.flatMap(_.find(criteria)
      .cursor[SubscriptionDetails]()
      .collect[List](-1, Cursor.FailOnError[List[SubscriptionDetails]]())
    )
  }

 private def getGeneric(criteria:JsObject) =
    OptionT(repository.flatMap(_.find(criteria).one[SubscriptionDetails]))

}
