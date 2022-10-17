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

package uk.gov.hmrc.cbcr.models

import java.time.LocalDate

import cats.data.NonEmptyList
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class EntityReportingPeriod(startDate: LocalDate, endDate: LocalDate)
object EntityReportingPeriod { implicit val format = Json.format[EntityReportingPeriod] }

object FormatNotEmptyList {
  implicit def formatNEL[A: Format]: Format[NonEmptyList[A]] = new Format[NonEmptyList[A]] {
    override def writes(o: NonEmptyList[A]): JsArray = JsArray(o.map(Json.toJson(_)).toList)

    override def reads(json: JsValue): JsResult[NonEmptyList[A]] =
      json
        .validate[List[A]]
        .flatMap(l =>
          NonEmptyList.fromList(l) match {
            case None    => JsError(s"Unable to serialise $json as NonEmptyList")
            case Some(a) => JsSuccess(a)
          })
        .orElse { json.validate[A].map(a => NonEmptyList(a, Nil)) }
  }
}

case class ReportingEntityData(
  cbcReportsDRI: NonEmptyList[DocRefId],
  additionalInfoDRI: List[DocRefId],
  reportingEntityDRI: DocRefId,
  tin: TIN,
  ultimateParentEntity: UltimateParentEntity,
  reportingRole: ReportingRole,
  creationDate: Option[LocalDate],
  reportingPeriod: Option[LocalDate],
  currencyCode: Option[String],
  entityReportingPeriod: Option[EntityReportingPeriod])

case class DocRefIdPair(docRefId: DocRefId, corrDocRefId: Option[CorrDocRefId])
object DocRefIdPair { implicit val format = Json.format[DocRefIdPair] }

case class PartialReportingEntityData(
  cbcReportsDRI: List[DocRefIdPair],
  additionalInfoDRI: List[DocRefIdPair],
  reportingEntityDRI: DocRefIdPair,
  tin: TIN,
  ultimateParentEntity: UltimateParentEntity,
  reportingRole: ReportingRole,
  creationDate: Option[LocalDate],
  reportingPeriod: Option[LocalDate],
  currencyCode: Option[String],
  entityReportingPeriod: Option[EntityReportingPeriod])

object PartialReportingEntityData {
  implicit val format = Json.format[PartialReportingEntityData]
}

object ReportingEntityData {
  import FormatNotEmptyList.formatNEL
  implicit val reads: Reads[ReportingEntityData] = (
    (JsPath \ "cbcReportsDRI").read[NonEmptyList[DocRefId]] and
      (JsPath \ "additionalInfoDRI")
        .read[List[DocRefId]]
        .orElse((JsPath \ "additionalInfoDRI").readNullable[DocRefId].map(_.toList)) and
      (JsPath \ "reportingEntityDRI").read[DocRefId] and
      (JsPath \ "tin").read[String].orElse((JsPath \ "utr").read[String]).map(TIN(_, "")) and
      (JsPath \ "ultimateParentEntity").read[UltimateParentEntity] and
      (JsPath \ "reportingRole").read[ReportingRole] and
      (JsPath \ "creationDate").readNullable[LocalDate] and
      (JsPath \ "reportingPeriod").readNullable[LocalDate] and
      (JsPath \ "currencyCode").readNullable[String] and
      (JsPath \ "entityReportingPeriod").readNullable[EntityReportingPeriod]
  )(ReportingEntityData.apply(_, _, _, _, _, _, _, _, _, _))

  implicit val writes = Json.writes[ReportingEntityData]

}

case class PartialReportingEntityDataModel(
  cbcReportsDRI: List[DocRefIdPair],
  additionalInfoDRI: List[DocRefIdPair],
  reportingEntityDRI: DocRefIdPair,
  tin: TIN,
  ultimateParentEntity: UltimateParentEntity,
  reportingRole: ReportingRole,
  creationDate: Option[LocalDate],
  reportingPeriod: Option[LocalDate],
  oldModel: Boolean,
  currencyCode: Option[String],
  entityReportingPeriod: Option[EntityReportingPeriod])

object PartialReportingEntityDataModel {
  implicit val format = Json.format[PartialReportingEntityDataModel]
}

case class ReportingEntityDataModel(
  cbcReportsDRI: NonEmptyList[DocRefId],
  additionalInfoDRI: List[DocRefId],
  reportingEntityDRI: DocRefId,
  tin: TIN,
  ultimateParentEntity: UltimateParentEntity,
  reportingRole: ReportingRole,
  creationDate: Option[LocalDate],
  reportingPeriod: Option[LocalDate],
  oldModel: Boolean,
  currencyCode: Option[String],
  entityReportingPeriod: Option[EntityReportingPeriod])

object ReportingEntityDataModel {
  import FormatNotEmptyList.formatNEL
  implicit val reads: Reads[ReportingEntityDataModel] = (
    (JsPath \ "cbcReportsDRI").read[NonEmptyList[DocRefId]] and
      (JsPath \ "additionalInfoDRI")
        .read[List[DocRefId]]
        .orElse((JsPath \ "additionalInfoDRI").readNullable[DocRefId].map(_.toList)) and
      (JsPath \ "reportingEntityDRI").read[DocRefId] and
      (JsPath \ "tin").read[String].orElse((JsPath \ "utr").read[String]).map(TIN(_, "")) and
      (JsPath \ "ultimateParentEntity").read[UltimateParentEntity] and
      (JsPath \ "reportingRole").read[ReportingRole] and
      (JsPath \ "creationDate").readNullable[LocalDate] and
      (JsPath \ "reportingPeriod").readNullable[LocalDate] and
      (JsPath \ "additionalInfoDRI")
        .read[List[DocRefId]]
        .map(_ => false)
        .orElse((JsPath \ "additionalInfoDRI").readNullable[DocRefId].map(_ => true)) and
      (JsPath \ "currencyCode").readNullable[String] and
      (JsPath \ "entityReportingPeriod").readNullable[EntityReportingPeriod]
  )(ReportingEntityDataModel.apply(_, _, _, _, _, _, _, _, _, _, _))

  implicit val writes = Json.writes[ReportingEntityDataModel]

}
