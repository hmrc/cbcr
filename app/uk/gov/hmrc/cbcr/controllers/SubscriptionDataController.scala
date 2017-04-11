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
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.cbcr.models.DbOperationResult.dbResult
import uk.gov.hmrc.cbcr.models.SubscriptionData
import uk.gov.hmrc.cbcr.repositories.GenericRepository
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by max on 03/04/17.
  */
class SubscriptionDataController(implicit val repository:GenericRepository[SubscriptionData]) extends BaseController {

  def saveSubscriptionData(): Action[JsValue] = Action.async(parse.json) { implicit request =>

    Logger.debug("Country by Country-backend: CBCR Save subscription data")

    request.body.validate[SubscriptionData].fold(
      error    => Future.successful(BadRequest(JsError.toJson(error))),
      response => repository.save(response).fold(
        state  => state.asResult,
        result => result.asResult
      )
    )
  }


  def retrieveSubscriptionData(cbcId:String):Action[AnyContent] = Action.async{ implicit request =>

    Logger.debug("Country by Country-backend: CBCR Retrieve subscription data")

    val criteria = Json.obj("cbcId" -> cbcId)

    repository.retrieve(criteria).cata(
      NotFound,
      (data: SubscriptionData) => Ok(Json.toJson(data))
    )

  }


}
