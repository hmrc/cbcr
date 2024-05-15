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
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.cbcr.auth.AuthenticatedAction
import uk.gov.hmrc.cbcr.models.FileUploadResponse
import uk.gov.hmrc.cbcr.repositories.FileUploadRepository
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileUploadResponseController @Inject()(
  repo: FileUploadRepository,
  auth: AuthenticatedAction,
  cc: ControllerComponents)(implicit val ec: ExecutionContext)
    extends BackendController(cc) {

  lazy val logger: Logger = Logger(this.getClass)

  def saveFileUploadResponse: Action[FileUploadResponse] =
    Action.async(parse.json[FileUploadResponse]) { implicit request =>
      request.body.status match {
        case "AVAILABLE" | "DELETED" | "ERROR" => repo.save2(request.body).map(_ => Ok)
        case _                                 => Future.successful(Ok)
      }
    }

  def retrieveFileUploadResponse(envelopeId: String): Action[AnyContent] = Action.andThen(auth).async {
    repo.get(envelopeId).map {
      case Some(obj) => Ok(Json.toJson(obj))
      case None      => NoContent
    }
  }

}
