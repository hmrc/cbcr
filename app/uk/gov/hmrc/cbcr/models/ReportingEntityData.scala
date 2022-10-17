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

object FormatOption {
  implicit def formatOption[T: Format]: Format[Option[T]] = new Format[Option[T]] {
    override def writes(option: Option[T]): JsValue = option match {
      case Some(left) => Json.toJson(left)
      case None       => JsNull
    }

    override def reads(json: JsValue): JsResult[Option[T]] =
      if (json == JsNull) JsSuccess(None) else json.validate[T].map(Some(_))
  }
}

object FormatEither {
  implicit def formatEither[L: Format, R: Format]: Format[Either[L, R]] = new Format[Either[L, R]] {
    override def writes(either: Either[L, R]): JsValue = either match {
      case Left(left)   => Json.toJson(left)
      case Right(right) => Json.toJson(right)
    }

    override def reads(json: JsValue): JsResult[Either[L, R]] =
      json.validate[L].map(Left(_)).orElse { json.validate[R].map(Right(_)) }
  }
}

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
  )(ReportingEntityData.apply _)

  implicit val writes = Json.writes[ReportingEntityData]

}

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

case class ReportingEntityDataModel(
  cbcReportsDRI: NonEmptyList[DocRefId],
  additionalInfoDRI: Either[Option[DocRefId], List[DocRefId]],
  reportingEntityDRI: DocRefId,
  tin: TIN,
  ultimateParentEntity: UltimateParentEntity,
  reportingRole: ReportingRole,
  creationDate: Option[LocalDate],
  reportingPeriod: Option[LocalDate],
  currencyCode: Option[String],
  entityReportingPeriod: Option[EntityReportingPeriod]) {

  def upgraded(): ReportingEntityDataModel =
    copy(additionalInfoDRI = additionalInfoDRI match {
      case Left(Some(value)) => Right(List(value))
      case Left(None) => Right(List())
      case value => value
    })
}

object ReportingEntityDataModel {
  import FormatNotEmptyList.formatNEL
  import FormatEither.formatEither
  import FormatOption.formatOption

  implicit val format: Format[ReportingEntityDataModel] = Json.format[ReportingEntityDataModel]
}
