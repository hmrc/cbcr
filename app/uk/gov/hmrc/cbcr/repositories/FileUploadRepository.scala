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
import play.api.Configuration
import uk.gov.hmrc.cbcr.models.FileUploadResponse
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.play.http.logging.Mdc.preservingMdc

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileUploadRepository @Inject() (mongo: MongoComponent, config: Configuration)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[FileUploadResponse](
      mongoComponent = mongo,
      collectionName = "FileUpload",
      domainFormat = FileUploadResponse.ufrFormat,
      indexes = Seq(
        IndexModel(
          ascending("id"),
          IndexOptions()
            .name("id")
            .expireAfter(
              config.get[FiniteDuration]("mongodb.cache-ttl.expiry-time").toSeconds,
              TimeUnit.SECONDS
            )
        )
      ),
      replaceIndexes = true
    ) {
  override lazy val requiresTtlIndex: Boolean = false

  def save2(f: FileUploadResponse): Future[Unit] =
    preservingMdc {
      collection.insertOne(f).toFutureOption().map(_ => ())
    }

  def get(envelopeId: String): Future[Option[FileUploadResponse]] =
    preservingMdc {
      for {
        responses <- collection.find(equal("envelopeId", envelopeId)).toFuture()
      } yield responses.lastOption
    }
}
