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
import uk.gov.hmrc.cbcr.models.{CBCId, SubscriptionDetails, Utr}
import uk.gov.hmrc.cbcr.repositories.SubscriptionDataRepository
import uk.gov.hmrc.play.microservice.controller.BaseController
import cats.instances.future._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class SubscriptionDataController @Inject() (repo:SubscriptionDataRepository) extends BaseController {

  def saveSubscriptionData(): Action[JsValue] = Action.async(parse.json) { implicit request =>

    Logger.debug("Country by Country-backend: CBCR Save subscription data")

    request.body.validate[SubscriptionDetails].fold(
      error    => Future.successful(BadRequest(JsError.toJson(error))),
      response => repo.save(response).map {
        case result if !result.ok => InternalServerError(result.writeErrors.mkString)
        case _ => Ok
      }
    )
  }

  def clearSubscriptionData(cbcId:String):Action[AnyContent] = Action.async{ implicit request =>

    Logger.debug("Country by Country-backend: CBCR clear subscription data")

    repo.clear(cbcId).fold[Result](NotFound){
      case result if !result.ok => InternalServerError(result.writeErrors.mkString)
      case _ => Ok
    }

  }

  def retrieveSubscriptionDataUtr(utr:Utr):Action[AnyContent] = Action.async { implicit request =>
    repo.get(utr).map{
      case Some(obj) => Ok(Json.toJson(obj))
      case None      => NotFound
    }
  }

  def retrieveSubscriptionDataCBCId(cbcId:CBCId):Action[AnyContent] = Action.async{ implicit request =>
    repo.get(cbcId).map{
      case Some(obj) => Ok(Json.toJson(obj))
      case None      => NotFound
    }
  }

}
