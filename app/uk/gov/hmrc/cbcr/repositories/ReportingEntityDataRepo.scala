/*
 * Copyright 2019 HM Revenue & Customs
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

import java.time.LocalDate

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.{Json, _}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor
import reactivemongo.api.commands.WriteResult
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json.commands.JSONFindAndModifyCommand
import uk.gov.hmrc.cbcr.models._

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}
import play.api.Logger

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
    repository.flatMap(_.insert(f.copy(creationDate = Some(LocalDate.now()))))

  def update(p:ReportingEntityData) : Future[Boolean] = {

    val criteria = Json.obj("cbcReportsDRI" -> p.cbcReportsDRI.head.id)

    for {
      collection <- repository
      update     <- collection.update(criteria,p)
    } yield update.ok

  }

  def update(p:PartialReportingEntityData) : Future[Boolean] = {
    if(p.additionalInfoDRI.flatMap(_.corrDocRefId).isEmpty &&
       p.cbcReportsDRI.flatMap(_.corrDocRefId).isEmpty &&
       p.reportingEntityDRI.corrDocRefId.isEmpty) {
      Future.successful(true)
    } else {
      val criteria:JsObject = buildUpdateCriteria(p)
      for {
        om          <- query(criteria).map(r => r.get).map(red => red.oldModel)
        modifier    = buildModifier(p,om)
        collection  <- repository
        update      <- collection.findAndModify(criteria, JSONFindAndModifyCommand.Update(modifier))
      } yield update.lastError.exists(_.updatedExisting)
    }
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

  private def query(c:JsObject) : Future[Option[ReportingEntityDataModel]] = {
    repository.flatMap(_.find(c).one[ReportingEntityDataModel])
  }


  def query(d:String) : Future[List[ReportingEntityData]] = {
    val criteria = Json.obj("$or" -> Json.arr(
      Json.obj("cbcReportsDRI"      -> Json.obj("$regex" -> (".*" + d + ".*" ))),
      Json.obj("additionalInfoDRI"  -> Json.obj("$regex" -> (".*" + d + ".*" ))),
      Json.obj("reportingEntityDRI" -> Json.obj("$regex" -> (".*" + d + ".*" )))
    ))
    Logger.info(s"ReportingEntityData retrieval query criteria: $criteria")
    repository.flatMap(_.find(criteria)
      .cursor[ReportingEntityData]()
      .collect[List](-1, Cursor.FailOnError[List[ReportingEntityData]]())
    )
  }

  def queryCbcId(cbcId: CBCId, reportingPeriod: LocalDate) : Future[Option[ReportingEntityData]] = {
    val criteria = Json.obj(
      "reportingEntityDRI" -> Json.obj("$regex" -> (".*" + cbcId.toString + ".*" )),
        "reportingPeriod" -> reportingPeriod.toString
    )

    Logger.info(s"ReportingEntityData retrieval query criteria: $criteria")
    repository.flatMap(_.find(criteria).one[ReportingEntityData])
  }

  def query(c:String, r:String) : Future[Option[ReportingEntityData]] = {
    val criteria = Json.obj("$and" -> Json.arr(
      Json.obj("$or" -> Json.arr(
        Json.obj("cbcReportsDRI"      -> Json.obj("$regex" -> (".*" + c + ".*" ))),
        Json.obj("additionalInfoDRI"  -> Json.obj("$regex" -> (".*" + c + ".*" ))),
        Json.obj("reportingEntityDRI" -> Json.obj("$regex" -> (".*" + c + ".*" )))
      )),
      Json.obj("reportingPeriod" -> r)
    ))
    repository.flatMap(_.find(criteria).one[ReportingEntityData])
  }

  def getAll: Future[List[ReportingEntityDataOld]] =
    repository.flatMap(_.find(JsObject(Seq.empty))
      .cursor[ReportingEntityDataOld]()
      .collect[List](-1, Cursor.FailOnError[List[ReportingEntityDataOld]]())
    )

  private def buildModifier(p:PartialReportingEntityData,aiOldModel:Boolean) : JsObject = {
    val x: immutable.Seq[(String, JsValue)] = List(
      if(aiOldModel) p.additionalInfoDRI.headOption.map(_.docRefId).map(i => "additionalInfoDRI" -> JsString(i.id))
      else p.additionalInfoDRI.headOption.map { _ => "additionalInfoDRI" -> JsArray(p.additionalInfoDRI.map(d => JsString(d.docRefId.id)))},
      p.cbcReportsDRI.headOption.map { _ => "cbcReportsDRI" -> JsArray(p.cbcReportsDRI.map(d => JsString(d.docRefId.id))) },
      p.reportingEntityDRI.corrDocRefId.map(_ => "reportingEntityDRI" -> JsString(p.reportingEntityDRI.docRefId.id)),
      Some("reportingRole" -> JsString(p.reportingRole.toString)),
      Some("tin" -> JsString(p.tin.value)),
      Some("ultimateParentEntity" -> JsString(p.ultimateParentEntity.ultimateParentEntity)),
      p.reportingPeriod.map(rd => "reportingPeriod" -> JsString(rd.toString))
    ).flatten

    Json.obj("$set" -> JsObject(x))

  }

  private def buildUpdateCriteria(p:PartialReportingEntityData) : JsObject = {
    val l: immutable.Seq[JsObject] =
      p.additionalInfoDRI.map(_.corrDocRefId.map(c => Json.obj("additionalInfoDRI" -> c.cid.id))).flatten ++
      p.reportingEntityDRI.corrDocRefId.map(c => Json.obj("reportingEntityDRI" -> c.cid.id)) ++
      p.cbcReportsDRI.map(_.corrDocRefId.map(c =>     Json.obj("cbcReportsDRI" -> c.cid.id))).flatten

    Json.obj("$and" -> JsArray(l))
  }

  def updateCreationDate(d:DocRefId, c: LocalDate) : Future[Int] = {
    val criteria = Json.obj("cbcReportsDRI" -> d.id)
    for {
      collection <- repository
      update     <- collection.update(criteria,Json.obj("$set" -> Json.obj("creationDate" -> c)))
    } yield update.nModified
  }

  def deleteCreationDate(d:DocRefId) : Future[Int] = {
    val criteria = Json.obj("cbcReportsDRI" -> d.id)
    for {
      collection <- repository
      update     <- collection.update(criteria,Json.obj("$unset" -> Json.obj("creationDate" -> 1)))
    } yield update.nModified
  }

  def confirmCreationDate(d:DocRefId, c:LocalDate) : Future[Int] = {
    val criteria = Json.obj("cbcReportsDRI" -> d.id, "creationDate" -> c)
    for {
      collection <- repository
      found      <- collection.count(Some(criteria))
    } yield found
  }

  def deleteReportingPeriod(d:DocRefId) : Future[Int] = {
    val criteria = Json.obj("cbcReportsDRI" -> d.id)
    for {
      collection <- repository
      update     <- collection.update(criteria,Json.obj("$unset" -> Json.obj("reportingPeriod" -> 1)))
    } yield update.nModified
  }

  def dropReportEntityCollection(): Future[Boolean] ={
    repository.flatMap(_.drop(true))
  }

}
