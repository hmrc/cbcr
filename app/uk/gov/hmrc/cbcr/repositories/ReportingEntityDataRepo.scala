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

import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.WriteResult
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json.commands.JSONFindAndModifyCommand
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReportingEntityDataRepo@Inject()(val mongo: ReactiveMongoApi)(implicit ec:ExecutionContext) {

  val repository: Future[JSONCollection] =
    mongo.database.map(_.collection[JSONCollection]("ReportingEntityData"))

  def save(f:ReportingEntityData) : Future[WriteResult] =
    repository.flatMap(_.insert(f))

  def update(p:PartialReportingEntityData) : Future[Boolean] = {

    val criteria = buildUpdateCriteria(p)
    val modifier = buildModifier(p)

    for {
      collection <- repository
      update     <- collection.findAndModify(criteria, JSONFindAndModifyCommand.Update(modifier))
    } yield update.lastError.exists(_.updatedExisting)

  }

  def query(d:DocRefId) : Future[Option[ReportingEntityData]] = {
    val criteria = Json.obj("$or" -> Json.arr(
      Json.obj("cbcReportsDRI"      -> d.id),
      Json.obj("additionalInfoDRI"  -> d.id),
      Json.obj("reportingEntityDRI" -> d.id)
    ))
    repository.flatMap(_.find(criteria).one[ReportingEntityData])
  }

  private def buildModifier(p:PartialReportingEntityData) : JsObject = Json.obj("$set" -> Json.obj(List(
    p.additionalInfoDRI.map(_.docRefId).map(i => "additionalInfoDRI" -> i.id),
    p.cbcReportsDRI.map(_.docRefId).map(i => "cbcReportsDRI" -> i.id),
    p.reportingEntityDRI.corrDocRefId.map(_ => "reportingEntityDRI" -> p.reportingEntityDRI.docRefId.id),
    Some("reportingRole" -> p.reportingRole.toString),
    Some("utr" -> p.utr.utr),
    Some("ultimateParentEntity" -> p.ultimateParentEntity.ultimateParentEntity)
  ).flatten:_*))

  private def buildUpdateCriteria(p:PartialReportingEntityData) : JsObject = {
    val l = List(
      p.additionalInfoDRI.flatMap(_.corrDocRefId.map(c => Json.obj("additionalInfoDRI" -> c.cid.id))),
      p.cbcReportsDRI.flatMap(_.corrDocRefId.map(c =>     Json.obj("cbcReportsDRI" -> c.cid.id))),
      p.reportingEntityDRI.corrDocRefId.map(c =>          Json.obj("reportingEntityDRI" -> c.cid.id))
    ).flatten

    Json.obj("$and" -> Json.arr(l:_*))
  }

}
