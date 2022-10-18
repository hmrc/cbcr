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

import java.time.LocalDate
import java.time._
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.{Json, _}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.WriteResult
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import uk.gov.hmrc.cbcr.models._

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.cbcr.services.AdminReportingEntityData
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.util.chaining.scalaUtilChainingOps

@Singleton
class ReportingEntityDataRepo @Inject()(val rmc: ReactiveMongoComponent)(implicit ec: ExecutionContext)
    extends ReactiveRepository[ReportingEntityDataModel, BSONObjectID](
      "ReportingEntityData",
      rmc.mongoConnector.db,
      ReportingEntityDataModel.format,
      ReactiveMongoFormats.objectIdFormats) {

  override def indexes: List[Index] = List(
    Index(Seq("reportingEntityDRI" -> Ascending), Some("Reporting Entity DocRefId"), unique = true)
  )

  def delete(d: DocRefId): Future[WriteResult] =
    remove(
      "$or" -> Json.arr(
        Json.obj("cbcReportsDRI"      -> d.id),
        Json.obj("additionalInfoDRI"  -> d.id),
        Json.obj("reportingEntityDRI" -> d.id)
      ))

  def removeAllDocs(): Future[WriteResult] = remove()

  def save(f: ReportingEntityData): Future[WriteResult] =
    insert(f.copy(creationDate = Some(LocalDate.now())).toDataModel)

  def update(p: ReportingEntityData): Future[Boolean] =
    findAndUpdate(
      Json.obj("cbcReportsDRI" -> p.cbcReportsDRI.head.id),
      Json.obj("$set"          -> Json.toJson(p.toDataModel))
    ).map(_.value.isDefined)

  /**This is an admin endpoint**/
  def updateReportingEntityDRI(
    adminReportingEntityData: AdminReportingEntityData,
    docRefId: DocRefId): Future[Boolean] =
    findAndUpdate(
      Json.obj("reportingEntityDRI" -> docRefId.id),
      Json.obj("$set"               -> Json.toJson(adminReportingEntityData))
    ).map(_.value.isDefined)

  def update(p: PartialReportingEntityData): Future[Boolean] =
    if (p.additionalInfoDRI.flatMap(_.corrDocRefId).isEmpty &&
        p.cbcReportsDRI.flatMap(_.corrDocRefId).isEmpty &&
        p.reportingEntityDRI.corrDocRefId.isEmpty) {
      updateEntityReportingPeriod(
        p.reportingEntityDRI.docRefId,
        p.entityReportingPeriod.getOrElse(throw new RuntimeException("EntityReportingPeriod missing")))
    } else {
      val conditions =
        p.additionalInfoDRI.flatMap(_.corrDocRefId).map(c => Json.obj("additionalInfoDRI" -> c.cid.id)) ++
          p.reportingEntityDRI.corrDocRefId.map(c => Json.obj("reportingEntityDRI"        -> c.cid.id)) ++
          p.cbcReportsDRI.flatMap(_.corrDocRefId).map(c => Json.obj("cbcReportsDRI"       -> c.cid.id))

      for {
        record <- find("$and" -> JsArray(conditions)).map {
                   case record :: _ => record
                   case _ =>
                     throw new NoSuchElementException("Original report not found in Mongo, while trying to update.")
                 }
        modifier = buildModifier(p, record)
        update <- findAndUpdate(Json.obj("$and" -> JsArray(conditions)), modifier)
      } yield update.lastError.exists(_.updatedExisting)
    }

  /** Find a reportingEntity that has a reportingEntityDRI with the provided docRefId */
  def queryReportingEntity(d: DocRefId): Future[Option[ReportingEntityData]] =
    find("reportingEntityDRI" -> d.id).map(_.headOption.map(_.toPublicModel))

  def query(d: DocRefId): Future[Option[ReportingEntityData]] =
    find(
      "$or" -> Json.arr(
        Json.obj("cbcReportsDRI"      -> d.id),
        Json.obj("additionalInfoDRI"  -> d.id),
        Json.obj("reportingEntityDRI" -> d.id)
      ))
      .map(_.headOption.map(_.toPublicModel))

  def query(d: String): Future[List[ReportingEntityData]] =
    find(
      "$or" -> Json.arr(
        Json.obj("cbcReportsDRI"      -> Json.obj("$regex" -> (".*" + d + ".*"))),
        Json.obj("additionalInfoDRI"  -> Json.obj("$regex" -> (".*" + d + ".*"))),
        Json.obj("reportingEntityDRI" -> Json.obj("$regex" -> (".*" + d + ".*")))
      )).map(_.map(_.toPublicModel))

  def queryCbcId(cbcId: CBCId, reportingPeriod: LocalDate): Future[Option[ReportingEntityData]] =
    find(
      "reportingEntityDRI" -> Json.obj("$regex" -> (".*" + cbcId.toString + ".*")),
      "reportingPeriod"    -> reportingPeriod.toString
    ).map(_.headOption.map(_.toPublicModel))

  def queryTIN(tin: String, reportingPeriod: String): Future[List[ReportingEntityData]] =
    find("tin" -> tin, "reportingPeriod" -> reportingPeriod)
      .map(_.map(_.toPublicModel))

  def queryTINDatesOverlapping(tin: String, entityReportingPeriod: EntityReportingPeriod): Future[Boolean] =
    find("tin" -> tin).map(_.map(_.toPublicModel).pipe(datesAreOverlapping(_, entityReportingPeriod)))

  def getLatestReportingEntityData(reportingEntityData: List[ReportingEntityData]): List[ReportingEntityData] = {
    val timestampRegex = """\d{8}T\d{6}""".r

    val reportingEntityDri: Seq[String] = reportingEntityData.map(data => data.reportingEntityDRI.id)

    val timestamps: Seq[LocalDateTime] = reportingEntityDri
      .map(dri => timestampRegex.findFirstIn(dri))
      .collect {
        case Some(data) => LocalDateTime.parse(data, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
      }

    val timestampSeparator = Set('-', ':')

    reportingEntityData.filter(
      x =>
        x.reportingEntityDRI.id
          .contains(timestamps.sorted(_.compareTo(_)).reverse.head.toString.filterNot(timestampSeparator.contains)))

  }

  def query(c: String, r: String): Future[Option[ReportingEntityData]] =
    find(
      "$and" -> Json.arr(
        Json.obj("$or" -> Json.arr(
          Json.obj("cbcReportsDRI"      -> Json.obj("$regex" -> (".*" + c + ".*"))),
          Json.obj("additionalInfoDRI"  -> Json.obj("$regex" -> (".*" + c + ".*"))),
          Json.obj("reportingEntityDRI" -> Json.obj("$regex" -> (".*" + c + ".*")))
        )),
        Json.obj("reportingPeriod" -> r)
      )).map(_.headOption.map(_.toPublicModel))

  def queryModel(d: DocRefId): Future[Option[ReportingEntityData]] =
    find(
      "$or" -> Json.arr(
        Json.obj("cbcReportsDRI"      -> d.id),
        Json.obj("additionalInfoDRI"  -> d.id),
        Json.obj("reportingEntityDRI" -> d.id)
      )).map(_.headOption.map(_.toPublicModel))

  private def buildModifier(p: PartialReportingEntityData, r: ReportingEntityDataModel): JsObject = {
    val x: immutable.Seq[(String, JsValue)] = List(
      r.additionalInfoDRI match {
        case Left(_) => p.additionalInfoDRI.headOption.map(_.docRefId).map(i => "additionalInfoDRI" -> JsString(i.id))
        case Right(rest) =>
          p.additionalInfoDRI.headOption.map(_ =>
            "additionalInfoDRI" -> JsArray(mergeListsAddInfo(p, rest).map(JsString)))
      },
      p.cbcReportsDRI.headOption.map { _ =>
        "cbcReportsDRI" -> JsArray(mergeListsReports(p, r).map(d => JsString(d)))
      },
      p.reportingEntityDRI.corrDocRefId.map(_ => "reportingEntityDRI" -> JsString(p.reportingEntityDRI.docRefId.id)),
      Some("reportingRole"        -> JsString(p.reportingRole.toString)),
      Some("tin"                  -> JsString(p.tin.value)),
      Some("ultimateParentEntity" -> JsString(p.ultimateParentEntity.ultimateParentEntity)),
      p.reportingPeriod.map(rd => "reportingPeriod" -> JsString(rd.toString)),
      p.currencyCode.map(cc => "currencyCode"       -> JsString(cc)),
      p.entityReportingPeriod.map(
        erp =>
          "entityReportingPeriod" -> Json
            .obj("startDate" -> JsString(erp.startDate.toString), "endDate" -> JsString(erp.endDate.toString)))
    ).flatten

    Json.obj("$set" -> JsObject(x))

  }

  def mergeListsAddInfo(p: PartialReportingEntityData, additionalInfoDRI: List[DocRefId]) = {
    val databaseRefIds = additionalInfoDRI.map(_.id)
    val corrDocRefIds = p.additionalInfoDRI.filter(_.corrDocRefId.isDefined).map(_.corrDocRefId.get.cid.id)
    val notModifiedDocRefIds = databaseRefIds.diff(corrDocRefIds)
    val updatedDocRefIds = p.additionalInfoDRI.map(_.docRefId.id)
    updatedDocRefIds ++ notModifiedDocRefIds
  }

  def mergeListsReports(p: PartialReportingEntityData, r: ReportingEntityDataModel) = {
    val databaseRefIds = r.cbcReportsDRI.map(_.id).toList
    val corrDocRefIds = p.cbcReportsDRI.filter(_.corrDocRefId.isDefined).map(_.corrDocRefId.get.cid.id)
    val notModifiedDocRefIds = databaseRefIds.diff(corrDocRefIds)
    val updatedDocRefIds = p.cbcReportsDRI.map(_.docRefId.id)
    updatedDocRefIds ++ notModifiedDocRefIds
  }

  def updateCreationDate(d: DocRefId, c: LocalDate): Future[Int] =
    findAndUpdate(
      Json.obj("cbcReportsDRI" -> d.id),
      Json.obj("$set"          -> Json.obj("creationDate" -> c))
    ).map(_.value.size)

  def updateEntityReportingPeriod(d: DocRefId, erp: EntityReportingPeriod): Future[Boolean] =
    findAndUpdate(
      Json.obj("reportingEntityDRI" -> d.id),
      Json.obj(
        "$set" -> Json.obj("entityReportingPeriod" ->
          Json.obj("startDate" -> JsString(erp.startDate.toString), "endDate" -> JsString(erp.endDate.toString))))
    ).map(_.value.isDefined)

  def deleteCreationDate(d: DocRefId): Future[Int] =
    findAndUpdate(
      Json.obj("cbcReportsDRI" -> d.id),
      Json.obj("$unset"        -> Json.obj("creationDate" -> 1))
    ).map(_.value.size)

  def confirmCreationDate(d: DocRefId, c: LocalDate): Future[Int] =
    count(Json.obj("cbcReportsDRI" -> d.id, "creationDate" -> c))

  def deleteReportingPeriod(d: DocRefId): Future[Int] =
    findAndUpdate(
      Json.obj("cbcReportsDRI" -> d.id),
      Json.obj("$unset"        -> Json.obj("reportingPeriod" -> 1))
    ).map(_.value.size)

  def deleteReportingPeriodByRepEntDocRefId(d: DocRefId): Future[Int] =
    findAndUpdate(
      Json.obj("reportingEntityDRI" -> d.id),
      Json.obj("$unset"             -> Json.obj("entityReportingPeriod" -> 1))
    ).map(_.value.size)

  def updateAdditionalInfoDRI(d: DocRefId): Future[Int] =
    findAndUpdate(
      Json.obj("additionalInfoDRI" -> d.id),
      Json.obj("$set"              -> Json.obj("additionalInfoDRI" -> d.id))
    ).map(_.value.size)

  def datesAreOverlapping(existingData: List[ReportingEntityData], entityReportingPeriod: EntityReportingPeriod) = {
    val groupedData =
      existingData
        .filter(_.reportingPeriod.isDefined)
        .groupBy(_.reportingPeriod.get)
    val res = groupedData.map(data => getLatestReportingEntityData(data._2)).filterNot(_.isEmpty).map(_.head)

    val filteredDeletionsAndSamePeriod =
      res.filter(data => filterOutDeletion(data)).filter(p => p.reportingPeriod.get != entityReportingPeriod.endDate)

    //mainly for backward compatibility make sure reporting period doesn't overlap with the new submission
    //true = no overlapping
    val firstCheck: Boolean =
      filteredDeletionsAndSamePeriod.forall(d => !checkBySingleDate(entityReportingPeriod, d.reportingPeriod.get))

    val secondCheckList = filteredDeletionsAndSamePeriod.filter(_.entityReportingPeriod.isDefined)

    //Make sure when we have both dates that they don't overlap true = no overlapping
    val secondCheck: Boolean =
      secondCheckList.forall(d => !checkBothDates(entityReportingPeriod, d.entityReportingPeriod.get))

    !(firstCheck && secondCheck)
  }

  def filterOutDeletion(record: ReportingEntityData): Boolean = {
    val entDocRefId = record.reportingEntityDRI.id
    !(entDocRefId.contains("OECD3"))
  }

  def checkBySingleDate(entityReportingPeriod: EntityReportingPeriod, reportingPeriod: LocalDate) = {
    val check1 = reportingPeriod.isAfter(entityReportingPeriod.startDate) && reportingPeriod.isBefore(
      entityReportingPeriod.endDate)
    val check2 = reportingPeriod.isEqual(entityReportingPeriod.startDate)
    check1 || check2
  }

  def checkBothDates(erp1: EntityReportingPeriod, erp2: EntityReportingPeriod) = {
    val check1 = erp1.startDate.isAfter(erp2.startDate) && erp1.startDate.isBefore(erp2.endDate)
    val check2 = erp1.endDate.isAfter(erp2.startDate) && erp1.endDate.isBefore(erp2.endDate)
    val check3 = erp2.startDate.isAfter(erp1.startDate) && erp2.startDate.isBefore(erp1.endDate)
    val check4 = erp2.endDate.isAfter(erp1.startDate) && erp2.endDate.isBefore(erp1.endDate)
    val check5 = erp1.startDate.isEqual(erp2.startDate) || erp1.startDate.isEqual(erp2.endDate)
    val check6 = erp2.startDate.isEqual(erp1.endDate)
    val results = List(check1, check2, check3, check4, check5, check6)
    results.contains(true)

  }

}
