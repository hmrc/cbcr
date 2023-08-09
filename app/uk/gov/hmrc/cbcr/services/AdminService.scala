/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.cbcr.services

import java.time.LocalDate
import com.google.inject.Singleton

import javax.inject.Inject
import play.api.libs.json.{Format, Json, OFormat}
import play.api.mvc.ControllerComponents
import play.api.{Configuration, Logger}
import uk.gov.hmrc.cbcr.models.{CBCId, DocRefId, DocRefIdRecord, DocRefIdResponses}
import uk.gov.hmrc.cbcr.repositories.{DocRefIdRepository, ReactiveDocRefIdRepository, ReportingEntityDataRepo}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.format.DateTimeParseException
import scala.concurrent.{ExecutionContext, Future}
import scala.util.chaining.scalaUtilChainingOps
import scala.util.control.NonFatal

case class AdminReportingEntityData(
  cbcReportsDRI: List[DocRefId],
  additionalInfoDRI: Option[List[DocRefId]],
  reportingEntityDRI: DocRefId)

object AdminReportingEntityData {
  implicit val format: OFormat[AdminReportingEntityData] = Json.format[AdminReportingEntityData]
}

@Singleton
class AdminService @Inject()(
  docRefIdRepo: ReactiveDocRefIdRepository,
  configuration: Configuration,
  repo: ReportingEntityDataRepo,
  docRepo: DocRefIdRepository,
  runMode: RunMode,
  audit: AuditConnector,
  cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  lazy val logger: Logger = Logger(this.getClass)

  def showAllDocRef = Action.async {
    docRefIdRepo.findAll().map(_.toList.pipe(displayAllDocRefId).pipe(Json.toJson(_)).pipe(Ok(_)))
  }

  def countDocRefId(docs: List[DocRefIdRecord]): ListDocRefIdRecord =
    ListDocRefIdRecord(docs.filterNot(doc => doc.id.id.length < 200))

  def displayAllDocRefId(docs: List[DocRefIdRecord]): ListDocRefIdRecord =
    ListDocRefIdRecord(docs)

}

case class ListDocRefIdRecord(docs: List[DocRefIdRecord])

object ListDocRefIdRecord {
  implicit val format: Format[ListDocRefIdRecord] = Json.format[ListDocRefIdRecord]
}
