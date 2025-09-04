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
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import org.mongodb.scala.result.{DeleteResult, InsertOneResult}
import uk.gov.hmrc.cbcr.models.MessageRefId
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mdc.Mdc.preservingMdc
import org.mongodb.scala.SingleObservableFuture

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MessageRefIdRepository @Inject() (mongo: MongoComponent)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[MessageRefId](
      mongoComponent = mongo,
      collectionName = "MessageRefId",
      domainFormat = MessageRefId.format,
      indexes = Seq(
        IndexModel(ascending("messageRefId"), IndexOptions().unique(true).name("Message Ref MessageRefId"))
      )
    ) {
  override lazy val requiresTtlIndex: Boolean = false

  def save2(f: MessageRefId): Future[InsertOneResult] =
    preservingMdc {
      collection.insertOne(f).toFuture()
    }

  def exists(messageRefId: String): Future[Boolean] =
    preservingMdc {
      collection.find(equal("messageRefId", messageRefId)).headOption().map(_.isDefined)
    }

  def delete(m: MessageRefId): Future[DeleteResult] =
    preservingMdc {
      collection.deleteOne(equal("messageRefId", m.id)).head()
    }
}
