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
import play.api.libs.json._
import play.api.mvc.Action
import uk.gov.hmrc.cbcr.models.UploadFileResponse
import uk.gov.hmrc.cbcr.repositories.FileUploadRepository
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class FileUploadResponseController @Inject() (repo:FileUploadRepository) extends BaseController {

  def saveFileUploadResponse  = Action.async(parse.json) { implicit request =>
    request.body.validate[UploadFileResponse].fold(
      error    => Future.successful(BadRequest(JsError.toJson(error))),
      response => repo.save(response).map{
        case result if result.ok        => Ok
        case result                     => InternalServerError(result.writeErrors.mkString)
      }
    )
  }

  def retrieveFileUploadResponse(envelopeId: String) = Action.async { implicit request =>
    repo.get(envelopeId).map{
      case Some(obj) => Ok(Json.toJson(obj))
      case None      => NoContent
    }
  }

}

