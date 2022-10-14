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
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.cbcr.models.MessageRefId
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MessageRefIdRepository @Inject()(val rmc: ReactiveMongoComponent)
    extends ReactiveRepository[MessageRefId, BSONObjectID](
      "MessageRefId",
      rmc.mongoConnector.db,
      MessageRefId.format,
      ReactiveMongoFormats.objectIdFormats) {
  override def indexes: List[Index] = List(
    Index(Seq("messageRefId" -> Ascending), Some("Message Ref MessageRefId"), unique = true)
  )

  def save2(f: MessageRefId)(implicit ec: ExecutionContext): Future[WriteResult] = insert(f)

  def exists(messageRefId: String)(implicit ec: ExecutionContext): Future[Boolean] =
    find("messageRefId" -> messageRefId).map(_.nonEmpty)

  def delete(m: MessageRefId)(implicit ec: ExecutionContext): Future[WriteResult] =
    remove("messageRefId" -> m.id)
}
