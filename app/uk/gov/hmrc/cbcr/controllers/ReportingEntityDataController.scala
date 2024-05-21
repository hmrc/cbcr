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

package uk.gov.hmrc.cbcr.controllers

import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.cbcr.auth.AuthenticatedAction
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.repositories.ReportingEntityDataRepo
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.LocalDate
import java.time.format.DateTimeParseException
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class ReportingEntityDataController @Inject()(
  repo: ReportingEntityDataRepo,
  auth: AuthenticatedAction,
  cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  lazy val logger: Logger = Logger(this.getClass)

  def save(): Action[ReportingEntityData] = Action(parse.json[ReportingEntityData]).andThen(auth).async { request =>
    repo.save(request.body).map(_ => Ok)
  }

  def update(): Action[PartialReportingEntityData] =
    Action(parse.json[PartialReportingEntityData]).andThen(auth).async { request =>
      repo.update(request.body).map {
        case true  => Ok
        case false => NotModified
      }
    }

  def queryDocRefId(d: DocRefId): Action[AnyContent] = Action.andThen(auth).async {
    repo
      .queryReportingEntity(d)
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

  def query(d: DocRefId): Action[AnyContent] = Action.andThen(auth).async {
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

  def queryCbcId(cbcId: CBCId, reportingPeriod: String): Action[AnyContent] = Action.andThen(auth).async {
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

  def queryTin(tin: String, reportingPeriod: String): Action[AnyContent] = Action.andThen(auth).async {
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

  def isOverlapping(tin: String, startDate: String, endDate: String): Action[AnyContent] = Action.andThen(auth).async {
    repo
      .queryTINDatesOverlapping(tin, EntityReportingPeriod(LocalDate.parse(startDate), LocalDate.parse(endDate)))
      .map { result =>
        if (result) Ok(Json.toJson(DatesOverlap(true))) else Ok(Json.toJson(DatesOverlap(false)))
      }
      .recover {
        case NonFatal(t) =>
          logger
            .error(s"Exception thrown trying to query for ReportingEntityData for overlapping rule: ${t.getMessage}", t)
          InternalServerError
      }
  }

  def queryModel(d: DocRefId): Action[AnyContent] = Action.andThen(auth).async { _ =>
    repo
      .queryModel(d)
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

}
