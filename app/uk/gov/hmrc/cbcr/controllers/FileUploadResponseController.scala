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

import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.cbcr.auth.CBCRAuth
import uk.gov.hmrc.cbcr.models.UploadFileResponse
import uk.gov.hmrc.cbcr.repositories.FileUploadRepository
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileUploadResponseController @Inject()(repo: FileUploadRepository, auth: CBCRAuth, cc: ControllerComponents)(
  implicit val ec: ExecutionContext)
    extends BackendController(cc) {

  def saveFileUploadResponse = Action.async(parse.json) { implicit request =>
    request.body
      .validate[UploadFileResponse]
      .fold(
        error => Future.successful(BadRequest(JsError.toJson(error))),
        response =>
          repo.save(response).map {
            case result if result.ok => Ok
            case result              => InternalServerError(result.writeErrors.mkString)
        }
      )
  }

  def retrieveFileUploadResponse(envelopeId: String) = auth.authCBCR { implicit request =>
    repo.get(envelopeId).map {
      case Some(obj) => Ok(Json.toJson(obj))
      case None      => NoContent
    }
  }

}
