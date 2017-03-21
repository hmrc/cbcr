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

import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json._
import play.api.mvc.{Action, Result}
import uk.gov.hmrc.cbcr.models.SaveAndRetrieve
import uk.gov.hmrc.cbcr.repositories.SaveAndRetrieveRepository
import uk.gov.hmrc.cbcr.services.{RetrieveService, SaveService}
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global

class SaveAndRetrieveController(val messagesApi: MessagesApi)(implicit repository: SaveAndRetrieveRepository) extends SaveAndRetrieveHelperController {

  def saveFileUploadResponse(cbcId: String) = saveFileUploadResponseData(cbcId)

  def retrieveFileUploadResponse(cbcId: String, envelopeId: String) = retrieveFileUploadResponseData(cbcId, envelopeId)

}

trait SaveAndRetrieveHelperController extends BaseController with I18nSupport {

  def saveFileUploadResponseData(cbcId: String)(implicit repo: SaveAndRetrieveRepository): Action[SaveAndRetrieve] = Action.async(parse.json[SaveAndRetrieve]) { implicit request =>
    Logger.debug("Country by Country-backend: CBCR Save the file upload response")

    SaveService.save(request.body, cbcId).map {
      case Left(err) => err.toResult
      case Right(dbSuccess) => dbSuccess.toResult
    }
  }

  def retrieveFileUploadResponseData(cbcId: String, envelopeId: String)(implicit repo: SaveAndRetrieveRepository) = Action.async { implicit request =>
    Logger.debug("Country by Country-backend: CBCR Retrieve the file upload response")

    RetrieveService.retrieve(cbcId, envelopeId).map {
      case Some(x) => Ok(x.value)
      case None => Ok(Json.obj())
    }
  }
}