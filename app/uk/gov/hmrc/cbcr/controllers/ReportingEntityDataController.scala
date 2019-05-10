/*
 * Copyright 2019 HM Revenue & Customs
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

import java.time.LocalDate

import javax.inject.{Inject, Singleton}
import org.omg.CosNaming.NamingContextPackage.NotFound
import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.cbcr.auth.CBCRAuth
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.repositories.ReportingEntityDataRepo
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class ReportingEntityDataController @Inject()(repo: ReportingEntityDataRepo, auth: CBCRAuth)(implicit ec: ExecutionContext) extends BaseController {


  def save() = auth.authCBCRWithJson({ implicit request =>
    request.body.validate[ReportingEntityData].fold(
      error => {
        Logger.error(s"Unable to de-serialise request as a ReportingEntityData: ${error.mkString}")
        Future.successful(BadRequest)
      },
      (data: ReportingEntityData) => repo.save(data).map {
        case result if result.ok => Ok
        case result => InternalServerError(result.writeErrors.mkString)
      }

    )
  }, parse.json)

  def update() = auth.authCBCRWithJson({ implicit request =>
    request.body.validate[PartialReportingEntityData].fold(
      error => {
        Logger.error(s"Unable to de-serialise request as a PartialReportingEntityData: ${error.mkString}")
        Future.successful(BadRequest)
      },
      (data: PartialReportingEntityData) => repo.update(data).map {
        case true => Ok
        case false => NotModified
      }
    )
  }, parse.json)

  def queryDocRefId(d: DocRefId) = auth.authCBCR { implicit request =>
    repo.queryReportingEntity(d).map {
      case None => NotFound
      case Some(data) => Ok(Json.toJson(data))
    }.recover {
      case NonFatal(t) =>
        Logger.error(s"Exception thrown trying to query for ReportingEntityData: ${t.getMessage}", t)
        InternalServerError
    }

  }

  def query(d: DocRefId) = auth.authCBCR { implicit request =>
    repo.query(d).map {
      case None => NotFound
      case Some(data) => Ok(Json.toJson(data))
    }.recover {
      case NonFatal(t) =>
        Logger.error(s"Exception thrown trying to query for ReportingEntityData: ${t.getMessage}", t)
        InternalServerError
    }

  }

  def queryCbcId(cbcId: CBCId, reportingPeriod: String) = auth.authCBCR { implicit request =>

    repo.queryCbcId(cbcId, LocalDate.parse(reportingPeriod)).map {
      case None => NotFound
      case Some(data) => Ok(Json.toJson(data))
    }.recover {
      case NonFatal(t) =>
        Logger.error(s"Exception thrown trying to query for ReportingEntityData: ${t.getMessage}", t)
        InternalServerError
    }

  }

  def queryTin(tin: String) = auth.authCBCR { implicit request =>
    repo.queryTIN(tin).map { reportEntityData =>

      if (reportEntityData.isEmpty) NotFound else Ok(Json.toJson(reportEntityData.head))
    }.recover {
      case NonFatal(t) =>
        Logger.error(s"Exception thrown trying to query for ReportingEntityData: ${t.getMessage}", t)
        InternalServerError
    }
  }

  def queryModel(d: DocRefId) = auth.authCBCR { implicit request =>
    repo.queryModel(d).map {
      case None => NotFound
      case Some(data) => Ok(Json.toJson(data))
    }.recover {
      case NonFatal(t) =>
        Logger.error(s"Exception thrown trying to query for ReportingEntityData: ${t.getMessage}", t)
        InternalServerError
    }

  }

}
