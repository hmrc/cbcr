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

package uk.gov.hmrc.cbcr.models

import cats.data.NonEmptyList
import play.api.libs.json._

case class ReportingEntityDataOld(cbcReportsDRI:DocRefId,
                               additionalInfoDRI:Option[DocRefId],
                               reportingEntityDRI:DocRefId,
                               utr:Utr,
                               ultimateParentEntity: UltimateParentEntity,
                               reportingRole: ReportingRole)

object ReportingEntityDataOld{ implicit val format = Json.format[ReportingEntityDataOld] }

case class ReportingEntityData(cbcReportsDRI:NonEmptyList[DocRefId],
                               additionalInfoDRI:Option[DocRefId],
                               reportingEntityDRI:DocRefId,
                               utr:Utr,
                               ultimateParentEntity: UltimateParentEntity,
                               reportingRole: ReportingRole)

case class DocRefIdPair(docRefId: DocRefId,corrDocRefId: Option[CorrDocRefId])
object DocRefIdPair{ implicit val format = Json.format[DocRefIdPair] }

case class PartialReportingEntityData(cbcReportsDRI:List[DocRefIdPair],
                                      additionalInfoDRI:Option[DocRefIdPair],
                                      reportingEntityDRI:DocRefIdPair,
                                      utr:Utr,
                                      ultimateParentEntity: UltimateParentEntity,
                                      reportingRole: ReportingRole)

object PartialReportingEntityData {
  implicit def formatNEL[A:Format] = new Format[NonEmptyList[A]] {
    override def writes(o: NonEmptyList[A]) = JsArray(o.map(Json.toJson(_)).toList)

    override def reads(json: JsValue) = json.validate[List[A]].flatMap(l => NonEmptyList.fromList(l) match {
      case None    => JsError(s"Unable to serialise $json as NonEmptyList")
      case Some(a) => JsSuccess(a)
    }).orElse{ json.validate[A].map(a => NonEmptyList(a,Nil)) }
  }

  implicit val format = Json.format[PartialReportingEntityData]
}

object ReportingEntityData{
  import PartialReportingEntityData.formatNEL
  implicit val format = Json.format[ReportingEntityData]

}
