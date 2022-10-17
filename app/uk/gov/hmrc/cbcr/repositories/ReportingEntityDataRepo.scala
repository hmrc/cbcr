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
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.{Cursor, ReadConcern, WriteConcern}
import reactivemongo.api.commands.{Collation, WriteResult}
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import reactivemongo.play.json.collection.JSONCollection
import uk.gov.hmrc.cbcr.models._
import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}
import play.api.Logger
import uk.gov.hmrc.cbcr.services.AdminReportingEntityData

import scala.concurrent.duration.FiniteDuration

@Singleton
class ReportingEntityDataRepo @Inject()(protected val mongo: ReactiveMongoApi)(implicit ec: ExecutionContext)
    extends IndexBuilder {

  lazy val logging: Logger = Logger(this.getClass)

  override protected val collectionName: String = "ReportingEntityData"
  override protected val cbcIndexes: List[CbcIndex] = List(CbcIndex("Reporting Entity DocRefId", "reportingEntityDRI"))

  val repository: Future[JSONCollection] = mongo.database.map(_.collection[JSONCollection](collectionName))

  def delete(d: DocRefId) = {
    val criteria = Json.obj(
      "$or" -> Json.arr(
        Json.obj("cbcReportsDRI"      -> d.id),
        Json.obj("additionalInfoDRI"  -> d.id),
        Json.obj("reportingEntityDRI" -> d.id)
      ))
    repository.flatMap(_.delete().one(criteria))
  }

  def removeAllDocs() = repository.flatMap { collection =>
    collection.delete(false).one(Json.obj())
  }

  def save(f: ReportingEntityData): Future[WriteResult] =
    repository.flatMap(_.insert(ordered = false).one(f.copy(creationDate = Some(LocalDate.now()))))

  def update(p: ReportingEntityData): Future[Boolean] = {

    val criteria = Json.obj("cbcReportsDRI" -> p.cbcReportsDRI.head.id)

    for {
      collection <- repository
      update     <- collection.update(ordered = false).one(criteria, p)
    } yield update.ok

  }

  /**This is an admin endpoint**/
  def updateReportingEntityDRI(adminReportingEntityData: AdminReportingEntityData, docRefId: DocRefId) = {
    val selector = Json.obj("reportingEntityDRI" -> docRefId.id)

    val update = Json.obj("$set" -> Json.toJson(adminReportingEntityData))

    for {
      collection <- repository
      update     <- collection.update(ordered = false).one(selector, update)
    } yield update.ok

  }

  def update(p: PartialReportingEntityData): Future[Boolean] =
    if (p.additionalInfoDRI.flatMap(_.corrDocRefId).isEmpty &&
        p.cbcReportsDRI.flatMap(_.corrDocRefId).isEmpty &&
        p.reportingEntityDRI.corrDocRefId.isEmpty) {
      updateEntityReportingPeriod(
        p.reportingEntityDRI.docRefId,
        p.entityReportingPeriod.getOrElse(throw new RuntimeException("EntityReportingPeriod missing")))
    } else {
      val criteria: JsObject = buildUpdateCriteria(p)
      for {
        record: ReportingEntityDataModel <- query(criteria)
                                             .map(_ match {
                                               case None =>
                                                 throw new NoSuchElementException(
                                                   "Original report not found in Mongo, while trying to update.")
                                               case Some(record) => record
                                             })
        modifier = buildModifier(p, record)
        collection <- repository
        update <- collection.findAndModify(
                   criteria,
                   collection.updateModifier(modifier),
                   None,
                   None,
                   false,
                   WriteConcern.Default,
                   Option.empty[FiniteDuration],
                   Option.empty[Collation],
                   Seq.empty
                 )
      } yield update.lastError.exists(_.updatedExisting)
    }

  /** Find a reportingEntity that has a reportingEntityDRI with the provided docRefId */
  def queryReportingEntity(d: DocRefId): Future[Option[ReportingEntityData]] =
    repository.flatMap(_.find(Json.obj("reportingEntityDRI" -> d.id), None).one[ReportingEntityData])

  def query(d: DocRefId): Future[Option[ReportingEntityData]] = {
    val criteria = Json.obj(
      "$or" -> Json.arr(
        Json.obj("cbcReportsDRI"      -> d.id),
        Json.obj("additionalInfoDRI"  -> d.id),
        Json.obj("reportingEntityDRI" -> d.id)
      ))
    repository.flatMap(_.find(criteria, None).one[ReportingEntityData])
  }

  private def query(c: JsObject): Future[Option[ReportingEntityDataModel]] =
    repository.flatMap(_.find(c, None).one[ReportingEntityDataModel])

  def query(d: String): Future[List[ReportingEntityData]] = {
    val criteria = Json.obj(
      "$or" -> Json.arr(
        Json.obj("cbcReportsDRI"      -> Json.obj("$regex" -> (".*" + d + ".*"))),
        Json.obj("additionalInfoDRI"  -> Json.obj("$regex" -> (".*" + d + ".*"))),
        Json.obj("reportingEntityDRI" -> Json.obj("$regex" -> (".*" + d + ".*")))
      ))
    logging.info(s"ReportingEntityData retrieval query criteria: $criteria")
    repository.flatMap(
      _.find(criteria, None)
        .cursor[ReportingEntityData]()
        .collect[List](-1, Cursor.FailOnError[List[ReportingEntityData]]()))
  }

  def queryCbcId(cbcId: CBCId, reportingPeriod: LocalDate): Future[Option[ReportingEntityData]] = {
    val criteria = Json.obj(
      "reportingEntityDRI" -> Json.obj("$regex" -> (".*" + cbcId.toString + ".*")),
      "reportingPeriod"    -> reportingPeriod.toString
    )

    logging.info(s"ReportingEntityData retrieval query criteria: $criteria")
    repository.flatMap(_.find(criteria, None).one[ReportingEntityData])
  }

  def queryTIN(tin: String, reportingPeriod: String): Future[List[ReportingEntityData]] = {
    val criteria = Json.obj("tin" -> tin, "reportingPeriod" -> reportingPeriod)

    val result: Future[List[ReportingEntityData]] = repository.flatMap(
      _.find(criteria, None)
        .cursor[ReportingEntityData]()
        .collect[List](-1, Cursor.FailOnError[List[ReportingEntityData]]()))

    result.map(x => getLatestReportingEntityData(x))

  }

  def queryTINDatesOverlapping(tin: String, entityReportingPeriod: EntityReportingPeriod) = {
    val criteria = Json.obj("tin" -> tin)
    val result: Future[List[ReportingEntityData]] = repository.flatMap(
      _.find(criteria, None)
        .cursor[ReportingEntityData]()
        .collect[List](-1, Cursor.FailOnError[List[ReportingEntityData]]()))

    result.map(d => datesAreOverlapping(d, entityReportingPeriod))

  }

  def getLatestReportingEntityData(reportingEntityData: List[ReportingEntityData]) = {
    val timestampRegex = """\d{8}T\d{6}""".r

    implicit val localDateTimeOrdering = new Ordering[LocalDateTime] {
      def compare(x: LocalDateTime, y: LocalDateTime): Int =
        x.compareTo(y)
    }

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
          .contains(timestamps.sorted.reverse.head.toString.filterNot(timestampSeparator.contains)))

  }

  def query(c: String, r: String): Future[Option[ReportingEntityData]] = {
    val criteria = Json.obj(
      "$and" -> Json.arr(
        Json.obj("$or" -> Json.arr(
          Json.obj("cbcReportsDRI"      -> Json.obj("$regex" -> (".*" + c + ".*"))),
          Json.obj("additionalInfoDRI"  -> Json.obj("$regex" -> (".*" + c + ".*"))),
          Json.obj("reportingEntityDRI" -> Json.obj("$regex" -> (".*" + c + ".*")))
        )),
        Json.obj("reportingPeriod" -> r)
      ))
    repository.flatMap(_.find(criteria, None).one[ReportingEntityData])
  }

  def queryModel(d: DocRefId): Future[Option[ReportingEntityDataModel]] = {
    logging.info(s"query reportingEntityDataModel with docRefId: ${d.id}")
    val criteria = Json.obj(
      "$or" -> Json.arr(
        Json.obj("cbcReportsDRI"      -> d.id),
        Json.obj("additionalInfoDRI"  -> d.id),
        Json.obj("reportingEntityDRI" -> d.id)
      ))
    repository.flatMap(_.find(criteria, None).one[ReportingEntityDataModel]).map(_.map(_.upgraded))
  }

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

  private def buildUpdateCriteria(p: PartialReportingEntityData): JsObject = {
    val l: immutable.Seq[JsObject] =
      p.additionalInfoDRI.map(_.corrDocRefId.map(c => Json.obj("additionalInfoDRI" -> c.cid.id))).flatten ++
        p.reportingEntityDRI.corrDocRefId.map(c => Json.obj("reportingEntityDRI" -> c.cid.id)) ++
        p.cbcReportsDRI.map(_.corrDocRefId.map(c => Json.obj("cbcReportsDRI" -> c.cid.id))).flatten

    Json.obj("$and" -> JsArray(l))
  }

  def updateCreationDate(d: DocRefId, c: LocalDate): Future[Int] = {
    val criteria = Json.obj("cbcReportsDRI" -> d.id)
    for {
      collection <- repository
      update     <- collection.update(ordered = false).one(criteria, Json.obj("$set" -> Json.obj("creationDate" -> c)))
    } yield update.nModified
  }

  def updateEntityReportingPeriod(d: DocRefId, erp: EntityReportingPeriod): Future[Boolean] = {
    val criteria = Json.obj("reportingEntityDRI" -> d.id)
    for {
      collection <- repository
      update <- collection
                 .update(ordered = false)
                 .one(
                   criteria,
                   Json.obj(
                     "$set" -> Json.obj(
                       "entityReportingPeriod" ->
                         Json.obj(
                           "startDate" -> JsString(erp.startDate.toString),
                           "endDate"   -> JsString(erp.endDate.toString))))
                 )
    } yield update.ok
  }

  def deleteCreationDate(d: DocRefId): Future[Int] = {
    val criteria = Json.obj("cbcReportsDRI" -> d.id)
    for {
      collection <- repository
      update     <- collection.update(ordered = false).one(criteria, Json.obj("$unset" -> Json.obj("creationDate" -> 1)))
    } yield update.nModified
  }

  def confirmCreationDate(d: DocRefId, c: LocalDate): Future[Int] = {
    val criteria = Json.obj("cbcReportsDRI" -> d.id, "creationDate" -> c)
    for {
      collection <- repository
      found <- collection.count(
                selector = Some(criteria),
                limit = None,
                hint = None,
                skip = 0,
                readConcern = ReadConcern.Available)
    } yield found.toInt
  }

  def deleteReportingPeriod(d: DocRefId): Future[Int] = {
    val criteria = Json.obj("cbcReportsDRI" -> d.id)
    for {
      collection <- repository
      update     <- collection.update(ordered = false).one(criteria, Json.obj("$unset" -> Json.obj("reportingPeriod" -> 1)))
    } yield update.nModified
  }

  def deleteReportingPeriodByRepEntDocRefId(d: DocRefId): Future[Int] = {
    val criteria = Json.obj("reportingEntityDRI" -> d.id)
    for {
      collection <- repository
      update <- collection
                 .update(ordered = false)
                 .one(criteria, Json.obj("$unset" -> Json.obj("entityReportingPeriod" -> 1)))
    } yield update.nModified
  }

  def updateAdditionalInfoDRI(d: DocRefId): Future[Int] = {
    val criteria = Json.obj("additionalInfoDRI" -> d.id)
    for {
      collection <- repository
      update <- collection
                 .update(ordered = false)
                 .one(criteria, Json.obj("$set" -> Json.obj("additionalInfoDRI" -> d.id)))
    } yield update.nModified
  }

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
