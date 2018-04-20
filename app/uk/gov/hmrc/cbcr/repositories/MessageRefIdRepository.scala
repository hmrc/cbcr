/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.WriteResult
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json._
import uk.gov.hmrc.cbcr.models.MessageRefId

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MessageRefIdRepository@Inject() (val mongo: ReactiveMongoApi)(implicit ec:ExecutionContext) extends IndexBuilder {
  override protected val collectionName: String = "MessageRefId"
  override protected val cbcIndexes: List[CbcIndex] = List( CbcIndex("Message Ref MessageRefId", "messageRefId"))

  val repository: Future[JSONCollection] =
    mongo.database.map(_.collection[JSONCollection]("MessageRefId"))

  def save(f:MessageRefId) : Future[WriteResult] =
    repository.flatMap(_.insert(f))

  def exists(messageRefId: String): Future[Boolean] = {
    val criteria = Json.obj("messageRefId" -> messageRefId)
    repository.flatMap(_.find(criteria).one[MessageRefId].map(_.isDefined))
  }

  def delete(m:MessageRefId): Future[WriteResult] = {
    val criteria = Json.obj("messageRefId" -> m.id)
    for {
      repo <- repository
      x    <- repo.remove(criteria)
    } yield  x
  }
}
