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

import play.api.libs.json.Json

case class DocRefIdPair(docRefId: DocRefId,corrDocRefId: Option[CorrDocRefId])
object DocRefIdPair{ implicit val format = Json.format[DocRefIdPair] }

case class ReportingEntityData(cbcReportsDRI:DocRefId,
                               additionalInfoDRI:Option[DocRefId],
                               reportingEntityDRI:DocRefId,
                               utr:Utr,
                               ultimateParentEntity: UltimateParentEntity,
                               reportingRole: ReportingRole)

object ReportingEntityData{ implicit val format = Json.format[ReportingEntityData] }

case class PartialReportingEntityData(cbcReportsDRI:Option[DocRefIdPair],
                                      additionalInfoDRI:Option[DocRefIdPair],
                                      reportingEntityDRI:DocRefIdPair,
                                      utr:Utr,
                                      ultimateParentEntity: UltimateParentEntity,
                                      reportingRole: ReportingRole)

object PartialReportingEntityData{ implicit val format = Json.format[PartialReportingEntityData] }
