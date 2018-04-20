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

import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.CollectionIndexesManager
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json.commands.JSONFindAndModifyCommand
import uk.gov.hmrc.cbcr.models._

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReportingEntityDataRepo @Inject()(protected val mongo: ReactiveMongoApi)(implicit ec:ExecutionContext) extends IndexBuilder {

  override protected val collectionName: String = "ReportingEntityData"
  override protected val cbcIndexes: List[CbcIndex] = List( CbcIndex("Reporting Entity DocRefId", "reportingEntityDRI"))

  val repository: Future[JSONCollection] = mongo.database.map(_.collection[JSONCollection](collectionName))

  def delete(d:DocRefId) = {
    val criteria = Json.obj("$or" -> Json.arr(
      Json.obj("cbcReportsDRI"      -> d.id),
      Json.obj("additionalInfoDRI"  -> d.id),
      Json.obj("reportingEntityDRI" -> d.id)
    ))
    repository.flatMap(_.remove(criteria))
  }

  def save(f:ReportingEntityData) : Future[WriteResult] =
    repository.flatMap(_.insert(f))

  def update(p:ReportingEntityData) : Future[Boolean] = {

    val criteria = Json.obj("cbcReportsDRI" -> p.cbcReportsDRI.head.id)

    for {
      collection <- repository
      update     <- collection.update(criteria,p)
    } yield update.ok

  }

  def updateAdditional(p: PartialReportingEntityData): Future[Boolean] = Future.successful(true)

  def update(p:PartialReportingEntityData) : Future[Boolean] = {

    val criteria = buildUpdateCriteria(p)
    val modifier = buildModifier(p)

    for {
      collection <- repository
      update     <- collection.findAndModify(criteria, JSONFindAndModifyCommand.Update(modifier))
    } yield update.lastError.exists(_.updatedExisting)

  }

  /** Find a reportingEntity that has a reportingEntityDRI with the provided docRefId */
  def queryReportingEntity(d:DocRefId) : Future[Option[ReportingEntityData]] =
    repository.flatMap(_.find(Json.obj("reportingEntityDRI" -> d.id)).one[ReportingEntityData])

  def query(d:DocRefId) : Future[Option[ReportingEntityData]] = {
    val criteria = Json.obj("$or" -> Json.arr(
      Json.obj("cbcReportsDRI"      -> d.id),
      Json.obj("additionalInfoDRI"  -> d.id),
      Json.obj("reportingEntityDRI" -> d.id)
    ))
    repository.flatMap(_.find(criteria).one[ReportingEntityData])
  }

  def getAll: Future[List[ReportingEntityDataOld]] =
    repository.flatMap(_.find(JsObject(Seq.empty))
      .cursor[ReportingEntityDataOld]()
      .collect[List](-1, Cursor.FailOnError[List[ReportingEntityDataOld]]())
    )

  private def buildModifier(p:PartialReportingEntityData) : JsObject = {
    val x: immutable.Seq[(String, JsValue)] = List(
      p.additionalInfoDRI.map(_.docRefId).map(i => "additionalInfoDRI" -> JsString(i.id)),
      p.cbcReportsDRI.headOption.map { _ => "cbcReportsDRI" -> JsArray(p.cbcReportsDRI.map(d => JsString(d.docRefId.id))) },
      p.reportingEntityDRI.corrDocRefId.map(_ => "reportingEntityDRI" -> JsString(p.reportingEntityDRI.docRefId.id)),
      Some("reportingRole" -> JsString(p.reportingRole.toString)),
      Some("tin" -> JsString(p.tin.value)),
      Some("ultimateParentEntity" -> JsString(p.ultimateParentEntity.ultimateParentEntity))
    ).flatten

    Json.obj("$set" -> JsObject(x))

  }

  private def buildUpdateCriteria(p:PartialReportingEntityData) : JsObject = {
    val l: immutable.Seq[JsObject] = List(
      p.additionalInfoDRI.flatMap(_.corrDocRefId.map(c => Json.obj("additionalInfoDRI" -> c.cid.id))),
      p.reportingEntityDRI.corrDocRefId.map(c =>          Json.obj("reportingEntityDRI" -> c.cid.id))
    ).flatten ++ p.cbcReportsDRI.map(_.corrDocRefId.map(c =>     Json.obj("cbcReportsDRI" -> c.cid.id))).flatten

    val test = l//if(l.isEmpty) immutable.Seq(Json.obj("reportingEntityDRI" -> p.reportingEntityDRI.docRefId)) else l

    Json.obj("$and" -> JsArray(test))
  }

}
