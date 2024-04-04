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

import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.result.DeleteResult
import uk.gov.hmrc.cbcr.models.DocRefIdResponses._
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DocRefIdRepository @Inject()(val mongo: MongoComponent, val records: ReactiveDocRefIdRepository)(
  implicit ec: ExecutionContext)
    extends PlayMongoRepository[DocRefId](
      mongoComponent = mongo,
      collectionName = "DocRefId",
      domainFormat = DocRefId.format,
      indexes = Seq(),
    ) {

  override lazy val requiresTtlIndex: Boolean = false

  def delete(d: DocRefId): Future[DeleteResult] =
    collection.deleteOne(equal("id", d.id)).toFuture()

  def edit(doc: DocRefId): Future[Long] =
    collection
      .updateMany(
        equal("id", doc.id),
        set("valid", true),
      )
      .toFuture()
      .map(_.getModifiedCount())

  def save2(id: DocRefId): Future[DocRefIdSaveResponse] =
    records.collection
      .find(equal("id", id.id))
      .headOption()
      .flatMap {
        case Some(entry) if entry.valid => Future.successful(AlreadyExists)
        case _                          => records.collection.insertOne(DocRefIdRecord(id, valid = true)).toFuture().map(_ => Ok)
      }

  def save2(c: CorrDocRefId, d: DocRefId): Future[(DocRefIdQueryResponse, Option[DocRefIdSaveResponse])] =
    query(c.cid).zip(query(d)).flatMap {
      case (Invalid, _)             => Future.successful((Invalid, None))
      case (DoesNotExist, _)        => Future.successful((DoesNotExist, None))
      case (Valid, Valid | Invalid) => Future.successful((Valid, Some(AlreadyExists)))
      case (Valid, DoesNotExist) =>
        for {
          doc <- records.collection
                  .findOneAndUpdate(and(equal("id", c.cid.id), equal("valid", true)), set("valid", false))
                  .toFutureOption()
          x <- if (doc.isDefined) {
                val valid = DocRefIdRecord.docRefIdValidity(d.id)
                records.collection
                  .insertOne(DocRefIdRecord(d, valid))
                  .toFuture()
                  .map(_ => Ok)
              } else {
                Future.successful(Failed)
              }
        } yield (Valid, Some(x))
    }

  def query(docRefId: DocRefId): Future[DocRefIdQueryResponse] =
    records.collection
      .find(equal("id", docRefId.id))
      .headOption()
      .map {
        case Some(r) if r.valid => Valid
        case Some(_)            => Invalid
        case None               => DoesNotExist
      }
}
