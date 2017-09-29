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
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection
import uk.gov.hmrc.cbcr.models._
import cats.instances.future._
import play.api.Logger
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONDocument
import uk.gov.hmrc.cbcr.models.DocRefIdResponses._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DocRefIdRepository @Inject()(val mongo: ReactiveMongoApi)(implicit ec:ExecutionContext) {

  val repository: Future[JSONCollection] =
    mongo.database.map(_.collection[JSONCollection]("DocRefId"))

  def delete(d:DocRefId): Future[WriteResult] = {
    val criteria = Json.obj("id" -> d.id)
    for {
      repo <- repository
      x    <- repo.remove(criteria)
    } yield  x
  }

  def save(f:DocRefId) : Future[DocRefIdSaveResponse] = {
    val criteria = Json.obj("id" -> f.id)
    for {
      repo <- repository
      x    <- repo.find(criteria).one[DocRefIdRecord]
      r    <- if(x.isDefined) Future.successful(AlreadyExists)
              else { repo.insert(DocRefIdRecord(f,valid = true)).map(w => if (w.ok) { Ok } else { Failed }) }
    } yield  r
  }

  def save(c:CorrDocRefId, d:DocRefId): Future[(DocRefIdQueryResponse,Option[DocRefIdSaveResponse])] ={
    val criteria = Json.obj("id" -> c.cid.id, "valid" -> true)
    query(c.cid).zip(query(d)).flatMap{
      case (Invalid,_)            => Future.successful((Invalid,None))
      case (DoesNotExist,_)       => Future.successful((DoesNotExist,None))
      case (Valid, Valid|Invalid) => Future.successful((Valid,Some(AlreadyExists)))
      case (Valid, DoesNotExist)  => for {
            repo     <- repository
            doc      <- repo.findAndModify(criteria,repo.updateModifier(BSONDocument("$set" -> BSONDocument("valid" -> false))))
            x        <- if(doc.result[DocRefIdRecord].isDefined) {
              repo.insert(DocRefIdRecord(d, valid = true)).map(w => if(w.ok) { Ok } else { Failed })
            } else {
              Logger.error(doc.toString)
              Future.successful(Failed)
            }
          } yield (Valid,Some(x))
      }
  }

  def query(docRefId: DocRefId): Future[DocRefIdQueryResponse] = {
    val criteria = Json.obj("id" -> docRefId.id)
    OptionT(repository.flatMap(_.find(criteria).one[DocRefIdRecord])).map(r =>
      if(r.valid) Valid
      else        Invalid
    ).getOrElse(DoesNotExist)
  }

}
