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

package uk.gov.hmrc.cbcr.controllers

import javax.inject.{Inject, Singleton}

import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Action
import uk.gov.hmrc.cbcr.models.{DocRefId, PartialReportingEntityData, ReportingEntityData}
import uk.gov.hmrc.cbcr.repositories.ReportingEntityDataRepo
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class ReportingEntityDataController@Inject() (repo:ReportingEntityDataRepo)(implicit ec:ExecutionContext) extends BaseController{


  def save() = Action.async(parse.json) { implicit request =>
    request.body.validate[ReportingEntityData].fold(
      error                       => {
        Logger.error(s"Unable to de-serialise request as a ReportingEntityData: ${error.mkString}")
        Future.successful(BadRequest)
      },
      (data: ReportingEntityData) => repo.save(data).map{
        case result if result.ok        => Ok
        case result                     => InternalServerError(result.writeErrors.mkString)
      }

    )
  }

  def update() = Action.async(parse.json) { implicit request =>
    request.body.validate[PartialReportingEntityData].fold(
      error                       => {
        Logger.error(s"Unable to de-serialise request as a PartialReportingEntityData: ${error.mkString}")
        Future.successful(BadRequest)
      },
      (data: PartialReportingEntityData) => repo.update(data).map{
        case true  => Ok
        case false => NotModified
      }
    )
  }

  def query(d:DocRefId) = Action.async{ implicit request =>
    repo.query(d).map{
      case None       => NotFound
      case Some(data) => Ok(Json.toJson(data))
    }.recover{
      case NonFatal(t) =>
        Logger.error(s"Exception thrown trying to query for ReportingEntityData: ${t.getMessage}", t)
        InternalServerError
    }

  }

}
