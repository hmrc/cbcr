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
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.cbcr.models.{CBCId, SubscriberContact, SubscriptionDetails, Utr}
import uk.gov.hmrc.cbcr.repositories.SubscriptionDataRepository
import uk.gov.hmrc.play.microservice.controller.BaseController
import cats.instances.future._
import uk.gov.hmrc.cbcr.auth.CBCRAuth

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class SubscriptionDataController @Inject()(repo: SubscriptionDataRepository, auth: CBCRAuth) extends BaseController {

  def saveSubscriptionData(): Action[JsValue] = auth.authCBCRWithJson({ implicit request =>
    request.body.validate[SubscriptionDetails].fold(
      error => Future.successful(BadRequest(JsError.toJson(error))),
      response => repo.save(response).map {
        case result if !result.ok => InternalServerError(result.writeErrors.mkString)
        case _ => Ok
      }
    )
  }, parse.json)

  def updateSubscriberContactDetails(cbcId: CBCId) = auth.authCBCRWithJson({ implicit request =>
    request.body.validate[SubscriberContact].fold(
      error => Future.successful(BadRequest(JsError.toJson(error))),
      response => repo.update(cbcId, response).map {
        case result if !result => InternalServerError
        case _ => Ok
      }
    )
  }, parse.json)

  def clearSubscriptionData(cbcId: CBCId): Action[AnyContent] = auth.authCBCR { implicit request =>
    repo.clear(cbcId).cata[Result](
      NotFound,
      result => if (!result.ok) InternalServerError(result.writeErrors.mkString) else Ok("ok")
    )
  }

  def retrieveSubscriptionDataUtr(utr: Utr): Action[AnyContent] = auth.authCBCR { implicit request =>
    repo.get(utr).cata(
      NotFound,
      details => Ok(Json.toJson(details))
    )
  }

  def retrieveSubscriptionDataCBCId(cbcId: CBCId): Action[AnyContent] = auth.authCBCR { implicit request =>
    repo.get(cbcId).cata(
      NotFound,
      details => Ok(Json.toJson(details))
    )
  }

}
