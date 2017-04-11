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

import cats.instances.future._
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.Action
import uk.gov.hmrc.cbcr.models.UploadFileResponse
import uk.gov.hmrc.cbcr.repositories.GenericRepository
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/** Controller that deals with saving and retrieving FileUploadResponse data */
class FileUploadResponseController(implicit val repository: GenericRepository[UploadFileResponse]) extends BaseController{

  def saveFileUploadResponse(cbcId: String)  = Action.async(parse.json) { implicit request =>
    Logger.debug(s"Country by Country-backend: CBCR Save the file upload response")

    request.body.validate[UploadFileResponse].fold(
      error    => Future.successful(BadRequest(JsError.toJson(error))),
      response => repository.save(response).fold(
        state    => state.asResult,
        dbResult => dbResult.asResult
      )
    )

  }

  def retrieveFileUploadResponse(cbcId: String, envelopeId: String) = Action.async { implicit request =>
    Logger.debug("Country by Country-backend: CBCR Retrieve the file upload response")

    val criteria = Json.obj("envelopeId" -> envelopeId, "status" -> "AVAILABLE")

    repository.retrieve(criteria).cata(
      NotFound,
      response => Ok(Json.toJson(response))
    )

  }

}

