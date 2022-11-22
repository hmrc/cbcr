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

package uk.gov.hmrc.cbcr.services

import java.time.LocalDate
import com.google.inject.Singleton

import javax.inject.Inject
import play.api.libs.json.{Format, Json}
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
  implicit val format = Json.format[AdminReportingEntityData]
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

  def adminDocRefIdquery(d: DocRefId) = Action.async {
    repo
      .query(d)
      .map {
        case None       => NotFound
        case Some(data) => Ok(Json.toJson(data))
      }
      .recover {
        case NonFatal(t) =>
          logger.error(s"Exception thrown trying to query for ReportingEntityData: ${t.getMessage}", t)
          InternalServerError
      }

  }

  def adminQueryTin(tin: String, reportingPeriod: String) = Action.async {
    try {
      repo
        .queryTIN(tin, LocalDate.parse(reportingPeriod))
        .map { reportEntityData =>
          if (reportEntityData.isEmpty) NotFound else Ok(Json.toJson(reportEntityData.head))
        }
        .recover {
          case NonFatal(t) =>
            logger.error(s"Exception thrown trying to query for ReportingEntityData: ${t.getMessage}", t)
            InternalServerError
        }
    } catch {
      case e: DateTimeParseException => Future.successful(BadRequest(s"Invalid reporting period ${e.getMessage}}"))
    }
  }

  def adminQueryCbcId(cbcId: CBCId, reportingPeriod: String) = Action.async {
    repo
      .queryCbcId(cbcId, LocalDate.parse(reportingPeriod))
      .map {
        case None       => NotFound
        case Some(data) => Ok(Json.toJson(data))
      }
      .recover {
        case NonFatal(t) =>
          logger.error(s"Exception thrown trying to query for ReportingEntityData: ${t.getMessage}", t)
          InternalServerError
      }

  }

  def editDocRefId(id: DocRefId) = Action.async {
    docRepo.edit(id) map {
      case n if n > 0 => Ok
      case _          => NotModified
    }
  }

  def saveDocRefId(id: DocRefId) = Action.async {
    docRepo.save2(id).map {
      case DocRefIdResponses.Ok            => Ok
      case DocRefIdResponses.AlreadyExists => Conflict
      case DocRefIdResponses.Failed        => InternalServerError
    }

  }

  def editReportingEntityData(docRefId: DocRefId) = Action.async(parse.json[AdminReportingEntityData]) {
    implicit request =>
      repo.updateReportingEntityDRI(request.body, docRefId).map {
        case true => Ok
        case false =>
          Ok(
            "Reporting entity was not updated due to an error. Please check if the json provided is correct" + Json
              .toJson(request.body))
      }
  }

}

case class ListDocRefIdRecord(docs: List[DocRefIdRecord])

object ListDocRefIdRecord {
  implicit val format: Format[ListDocRefIdRecord] = Json.format[ListDocRefIdRecord]
}
