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

import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.{equal, regex}
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.Updates.{set, unset}
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions}
import org.mongodb.scala.result.{DeleteResult, InsertOneResult}
import play.api.libs.json._
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.services.AdminReportingEntityData
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.time._
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.chaining.scalaUtilChainingOps

case class DateRange(startDate: String, endDate: String)

object DateRange {
  implicit val format: Format[DateRange] = Json.format[DateRange]
}

@Singleton
class ReportingEntityDataRepo @Inject()(val mongo: MongoComponent)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[ReportingEntityDataModel](
      mongoComponent = mongo,
      collectionName = "ReportingEntityData",
      domainFormat = ReportingEntityDataModel.format,
      indexes = Seq(
        IndexModel(ascending("reportingEntityDRI"), IndexOptions().name("Reporting Entity DocRefId").unique(true))
      )
    ) {

  private implicit val dateFormat: Format[LocalDate] = ReportingEntityDataModel.dateFormat

  def delete(d: DocRefId): Future[DeleteResult] =
    collection
      .deleteMany(
        Filters.or(
          equal("cbcReportsDRI", d.id),
          equal("additionalInfoDRI", d.id),
          equal("reportingEntityDRI", d.id)
        )
      )
      .toFuture

  def removeAllDocs(): Future[DeleteResult] = collection.deleteMany(Filters.empty()).toFuture

  def save(f: ReportingEntityData): Future[InsertOneResult] =
    collection.insertOne(f.copy(creationDate = Some(LocalDate.now())).toDataModel).toFuture

  def update(p: ReportingEntityData): Future[Boolean] =
    collection
      .findOneAndReplace(
        equal("cbcReportsDRI", p.cbcReportsDRI.head.id),
        p.toDataModel
      )
      .toFutureOption()
      .map(_.isDefined)

  /**This is an admin endpoint**/
  def updateReportingEntityDRI(admin: AdminReportingEntityData, docRefId: DocRefId): Future[Boolean] = {
    import DocRefId.format
    collection
      .findOneAndUpdate(
        equal("reportingEntityDRI", docRefId.id),
        Seq(
          set("cbcReportsDRI", Codecs.toBson(admin.cbcReportsDRI)),
          set("additionalInfoDRI", Codecs.toBson(admin.additionalInfoDRI)),
          set("reportingEntityDRI", Codecs.toBson(admin.reportingEntityDRI)),
        )
      )
      .toFutureOption()
      .map(_.isDefined)
  }

  def update(p: PartialReportingEntityData): Future[Boolean] =
    if (p.additionalInfoDRI.flatMap(_.corrDocRefId).isEmpty &&
        p.cbcReportsDRI.flatMap(_.corrDocRefId).isEmpty &&
        p.reportingEntityDRI.corrDocRefId.isEmpty) {
      updateEntityReportingPeriod(
        p.reportingEntityDRI.docRefId,
        p.entityReportingPeriod.getOrElse(throw new RuntimeException("EntityReportingPeriod missing")))
    } else {
      val condition = {
        val conditions: Seq[Bson] =
          p.additionalInfoDRI.flatMap(_.corrDocRefId).map(c => equal("additionalInfoDRI", c.cid.id)) ++
            p.reportingEntityDRI.corrDocRefId.map(c => equal("reportingEntityDRI", c.cid.id)) ++
            p.cbcReportsDRI.flatMap(_.corrDocRefId).map(c => equal("cbcReportsDRI", c.cid.id))
        Filters.and(conditions: _*)
      }

      for {
        record <- collection.find(condition).headOption().map {
                   case Some(record) => record
                   case _ =>
                     throw new NoSuchElementException("Original report not found in Mongo, while trying to update.")
                 }
        modifier = buildModifier(p, record)
        update <- collection.findOneAndUpdate(condition, modifier).toFutureOption()
      } yield update.isDefined
    }

  /** Find a reportingEntity that has a reportingEntityDRI with the provided docRefId */
  def queryReportingEntity(d: DocRefId): Future[Option[ReportingEntityData]] =
    collection
      .find(equal("reportingEntityDRI", d.id))
      .map(_.toPublicModel)
      .headOption()

  def query(d: DocRefId): Future[Option[ReportingEntityData]] =
    collection
      .find(
        Filters.or(
          equal("cbcReportsDRI", d.id),
          equal("additionalInfoDRI", d.id),
          equal("reportingEntityDRI", d.id)
        )
      )
      .map(_.toPublicModel)
      .headOption

  def query(d: String): Future[Seq[ReportingEntityData]] =
    collection
      .find(
        Filters.or(
          regex("cbcReportsDRI", ".*" + d + ".*"),
          regex("additionalInfoDRI", ".*" + d + ".*"),
          regex("reportingEntityDRI", ".*" + d + ".*")
        )
      )
      .map(_.toPublicModel)
      .toFuture

  def queryCbcId(cbcId: CBCId, reportingPeriod: LocalDate): Future[Option[ReportingEntityData]] =
    collection
      .find(
        Filters.and(
          regex("reportingEntityDRI", ".*" + cbcId.toString + ".*"),
          equal("reportingPeriod", Codecs.toBson(reportingPeriod))
        )
      )
      .map(_.toPublicModel)
      .headOption

  def queryTIN(tin: String, reportingPeriod: String): Future[Seq[ReportingEntityData]] =
    collection
      .find(
        Filters.and(
          equal("tin", tin),
          equal("reportingPeriod", Codecs.toBson(LocalDate.parse(reportingPeriod)))
        )
      )
      .map(_.toPublicModel)
      .toFuture

  def queryTINDatesOverlapping(tin: String, entityReportingPeriod: EntityReportingPeriod): Future[Boolean] =
    collection
      .find(equal("tin", tin))
      .toFuture()
      .map(_.map(_.toPublicModel).pipe(datesAreOverlapping(_, entityReportingPeriod)))

  def getLatestReportingEntityData(reportingEntityData: Seq[ReportingEntityData]): Seq[ReportingEntityData] = {
    val timestampRegex = """\d{8}T\d{6}""".r
    val timestampSeparator = Set('-', ':')
    val searchedFor =
      reportingEntityData
        .map(
          _.reportingEntityDRI.id
            .pipe(timestampRegex.findFirstIn(_).get)
            .pipe(LocalDateTime.parse(_, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))))
        .max((a: LocalDateTime, b: LocalDateTime) => a.compareTo(b))
        .toString
        .filterNot(timestampSeparator.contains)
    reportingEntityData.filter(_.reportingEntityDRI.id.contains(searchedFor))
  }

  def query(c: String, r: String): Future[Option[ReportingEntityData]] =
    collection
      .find(
        Filters.and(
          Filters.or(
            regex("cbcReportsDRI", ".*" + c + ".*"),
            regex("additionalInfoDRI", ".*" + c + ".*"),
            regex("reportingEntityDRI", ".*" + c + ".*"),
          ),
          equal("reportingPeriod", Codecs.toBson(LocalDate.parse(r)))
        )
      )
      .map(_.toPublicModel)
      .headOption

  def queryModel(d: DocRefId): Future[Option[ReportingEntityData]] =
    collection
      .find(
        Filters.or(
          equal("cbcReportsDRI", d.id),
          equal("additionalInfoDRI", d.id),
          equal("reportingEntityDRI", d.id)
        )
      )
      .map(_.toPublicModel)
      .headOption

  private def buildModifier(p: PartialReportingEntityData, r: ReportingEntityDataModel): List[Bson] =
    List(
      r.additionalInfoDRI match {
        case Left(_) =>
          p.additionalInfoDRI.headOption.map(_.docRefId).map(i => set("additionalInfoDRI", i.id))
        case Right(rest) =>
          p.additionalInfoDRI.headOption.map(_ => set("additionalInfoDRI", mergeListsAddInfo(p, rest)))
      },
      p.cbcReportsDRI.headOption.map { _ =>
        set("cbcReportsDRI", mergeListsReports(p, r))
      },
      p.reportingEntityDRI.corrDocRefId.map(_ => set("reportingEntityDRI", p.reportingEntityDRI.docRefId.id)),
      Some(set("reportingRole", p.reportingRole.toString)),
      Some(set("tin", p.tin.value)),
      Some(set("ultimateParentEntity", p.ultimateParentEntity.ultimateParentEntity)),
      p.reportingPeriod.map(rd => set("reportingPeriod", rd.toString)),
      p.currencyCode.map(cc => set("currencyCode", cc)),
      p.entityReportingPeriod.map(erp =>
        set("entityReportingPeriod", Codecs.toBson(DateRange(erp.startDate.toString, erp.endDate.toString))))
    ).flatten

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

  def updateCreationDate(d: DocRefId, c: LocalDate): Future[Long] =
    collection
      .updateMany(
        equal("cbcReportsDRI", d.id),
        set("creationDate", c)
      )
      .toFuture
      .map(_.getModifiedCount)

  def updateEntityReportingPeriod(d: DocRefId, erp: EntityReportingPeriod): Future[Boolean] =
    collection
      .findOneAndUpdate(
        equal("reportingEntityDRI", d.id),
        set(
          "entityReportingPeriod",
          Codecs.toBson(DateRange(erp.startDate.toString, erp.endDate.toString))
        )
      )
      .headOption
      .map(_.isDefined)

  def deleteCreationDate(d: DocRefId): Future[Long] =
    collection
      .updateMany(
        equal("cbcReportsDRI", d.id),
        unset("creationDate")
      )
      .toFuture
      .map(_.getModifiedCount)

  def confirmCreationDate(d: DocRefId, c: LocalDate): Future[Long] =
    collection
      .countDocuments(
        Filters.and(
          equal("cbcReportsDRI", d.id),
          equal("creationDate", c)
        )
      )
      .toFuture

  def deleteReportingPeriod(d: DocRefId): Future[Long] =
    collection
      .updateMany(
        equal("cbcReportsDRI", d.id),
        unset("reportingPeriod")
      )
      .toFuture
      .map(_.getModifiedCount)

  def deleteReportingPeriodByRepEntDocRefId(d: DocRefId): Future[Long] =
    collection
      .updateMany(
        equal("reportingEntityDRI", d.id),
        unset("entityReportingPeriod")
      )
      .toFuture
      .map(_.getModifiedCount)

  def updateAdditionalInfoDRI(d: DocRefId): Future[Long] =
    collection
      .updateMany(
        equal("additionalInfoDRI", d.id),
        set("additionalInfoDRI", d.id)
      )
      .toFuture
      .map(_.getModifiedCount)

  def datesAreOverlapping(
    existingData: Seq[ReportingEntityData],
    entityReportingPeriod: EntityReportingPeriod): Boolean = {
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

  def checkBySingleDate(entityReportingPeriod: EntityReportingPeriod, reportingPeriod: LocalDate): Boolean = {
    val check1 = reportingPeriod.isAfter(entityReportingPeriod.startDate) && reportingPeriod.isBefore(
      entityReportingPeriod.endDate)
    val check2 = reportingPeriod.isEqual(entityReportingPeriod.startDate)
    check1 || check2
  }

  def checkBothDates(erp1: EntityReportingPeriod, erp2: EntityReportingPeriod): Boolean = {
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
