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

import cats.data.OptionT
import cats.instances.future._
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.WriteConcern
import reactivemongo.api.commands.{Collation, WriteResult}
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import reactivemongo.play.json.collection.JSONCollection
import uk.gov.hmrc.cbcr.models.DocRefIdResponses._
import uk.gov.hmrc.cbcr.models._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DocRefIdRepository @Inject()(val mongo: ReactiveMongoApi)(implicit ec: ExecutionContext) {

  lazy val logger: Logger = Logger(this.getClass)

  val repository: Future[JSONCollection] =
    mongo.database.map(_.collection[JSONCollection]("DocRefId"))

  def delete(d: DocRefId): Future[WriteResult] = {
    val criteria = Json.obj("id" -> d.id)
    for {
      repo <- repository
      x    <- repo.delete().one(criteria)
    } yield x
  }

  def edit(doc: DocRefId): Future[Int] = {

    val criteria = Json.obj("id" -> doc.id)
    for {
      collection <- repository
      update     <- collection.update(ordered = false).one(criteria, Json.obj("$set" -> Json.obj("valid" -> true)))
    } yield update.nModified
  }

  def save(f: DocRefId): Future[DocRefIdSaveResponse] = {
    val criteria = Json.obj("id" -> f.id)
    for {
      repo <- repository
      x    <- repo.find(criteria, None).one[DocRefIdRecord]
      r <- if (x.isDefined) Future.successful(AlreadyExists)
          else {
            repo.insert(ordered = false).one(DocRefIdRecord(f, valid = true)).map(w => if (w.ok) { Ok } else { Failed })
          }
    } yield r
  }

  def save(c: CorrDocRefId, d: DocRefId): Future[(DocRefIdQueryResponse, Option[DocRefIdSaveResponse])] = {
    import reactivemongo.play.json.ImplicitBSONHandlers.BSONDocumentWrites

    val criteria = Json.obj("id" -> c.cid.id, "valid" -> true)
    query(c.cid).zip(query(d)).flatMap {
      case (Invalid, _)             => Future.successful((Invalid, None))
      case (DoesNotExist, _)        => Future.successful((DoesNotExist, None))
      case (Valid, Valid | Invalid) => Future.successful((Valid, Some(AlreadyExists)))
      case (Valid, DoesNotExist) =>
        for {
          repo <- repository
          doc <- repo.findAndModify(
                  criteria,
                  repo.updateModifier(BSONDocument("$set" -> BSONDocument("valid" -> false))),
                  None,
                  None,
                  false,
                  WriteConcern.Default,
                  Option.empty[FiniteDuration],
                  Option.empty[Collation],
                  Seq.empty
                )
          validFlag = DocRefIdRecord.docRefIdValidity(d.id)
          x <- if (doc.result[DocRefIdRecord].isDefined) {
                repo
                  .insert(ordered = false)
                  .one(DocRefIdRecord(d, valid = validFlag))
                  .map(w => if (w.ok) { Ok } else { Failed })
              } else {
                logger.error(doc.toString)
                Future.successful(Failed)
              }
        } yield (Valid, Some(x))
    }
  }

  def query(docRefId: DocRefId): Future[DocRefIdQueryResponse] = {
    val criteria = Json.obj("id" -> docRefId.id)
    OptionT(repository.flatMap(_.find(criteria, None).one[DocRefIdRecord]))
      .map(r =>
        if (r.valid) Valid
        else Invalid)
      .getOrElse(DoesNotExist)
  }

}
