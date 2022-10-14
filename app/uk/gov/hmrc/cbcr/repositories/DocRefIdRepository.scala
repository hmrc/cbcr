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

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import uk.gov.hmrc.cbcr.models.DocRefIdResponses._
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DocRefIdRepository @Inject()(val rmc: ReactiveMongoComponent, val records: ReactiveDocRefIdRepository)
    extends ReactiveRepository[DocRefId, BSONObjectID](
      "DocRefId",
      rmc.mongoConnector.db,
      DocRefId.format,
      ReactiveMongoFormats.objectIdFormats) {

  def delete(d: DocRefId)(implicit ec: ExecutionContext): Future[WriteResult] =
    remove("id" -> d.id)

  def edit(doc: DocRefId)(implicit ec: ExecutionContext): Future[Int] =
    findAndUpdate(query = Json.obj("id" -> doc.id), update = Json.obj("$set" -> Json.obj("valid" -> true)))
      .map(_.value.size)

  def save2(id: DocRefId)(implicit ec: ExecutionContext): Future[DocRefIdSaveResponse] =
    records
      .find("id" -> id.id)
      .map(_.headOption.map(_.valid))
      .flatMap {
        case Some(true) => Future.successful(AlreadyExists)
        case _ =>
          records.insert(DocRefIdRecord(id, valid = true)).map {
            case r if r.ok => Ok
            case _         => Failed
          }
      }

  def save2(c: CorrDocRefId, d: DocRefId)(
    implicit ec: ExecutionContext): Future[(DocRefIdQueryResponse, Option[DocRefIdSaveResponse])] = {
    val criteria = Json.obj("id" -> c.cid.id, "valid" -> true)
    query(c.cid).zip(query(d)).flatMap {
      case (Invalid, _)             => Future.successful((Invalid, None))
      case (DoesNotExist, _)        => Future.successful((DoesNotExist, None))
      case (Valid, Valid | Invalid) => Future.successful((Valid, Some(AlreadyExists)))
      case (Valid, DoesNotExist) =>
        for {
          doc <- records.findAndUpdate(query = criteria, update = Json.obj("$set" -> Json.obj("valid" -> false)))
          x <- if (doc.result[DocRefIdRecord].isDefined) {
                val valid = DocRefIdRecord.docRefIdValidity(d.id)
                records
                  .insert(DocRefIdRecord(d, valid))
                  .map {
                    case r if r.ok => Ok
                    case _         => Failed
                  }
              } else {
                logger.error(doc.toString)
                Future.successful(Failed)
              }
        } yield (Valid, Some(x))
    }
  }

  def query(docRefId: DocRefId)(implicit ec: ExecutionContext): Future[DocRefIdQueryResponse] =
    records
      .find("id" -> docRefId.id)
      .map(_.headOption match {
        case Some(r) if r.valid => Valid
        case Some(_)            => Invalid
        case None               => DoesNotExist
      })

}
