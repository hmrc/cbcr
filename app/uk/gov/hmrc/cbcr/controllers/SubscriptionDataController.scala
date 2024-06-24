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

import org.mongodb.scala.model.Filters.equal

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.cbcr.auth.CBCRAuth
import uk.gov.hmrc.cbcr.connectors.DESConnector
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.repositories.SubscriptionDataRepository
import uk.gov.hmrc.mongo.play.json.Codecs
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionDataController @Inject() (
  repo: SubscriptionDataRepository,
  des: DESConnector,
  auth: CBCRAuth,
  configuration: Configuration,
  cc: ControllerComponents
)(implicit val ec: ExecutionContext)
    extends BackendController(cc) {

  def saveSubscriptionData(): Action[JsValue] =
    auth.authCBCRWithJson(
      implicit request =>
        request.body
          .validate[SubscriptionDetails]
          .fold(
            error => Future.successful(BadRequest(JsError.toJson(error))),
            response => repo.save2(response).map(_ => Ok)
          ),
      parse.json
    )

  def updateSubscriberContactDetails(cbcId: CBCId) =
    auth.authCBCRWithJson(
      implicit request =>
        request.body
          .validate[SubscriberContact]
          .fold(
            error => Future.successful(BadRequest(JsError.toJson(error))),
            response =>
              repo.update(equal("cbcId", Codecs.toBson(cbcId)), response).map {
                case true  => Ok
                case false => InternalServerError
              }
          ),
      parse.json
    )

  def clearSubscriptionData(cbcId: CBCId): Action[AnyContent] = auth.authCBCR { _ =>
    repo
      .clearCBCId(cbcId)
      .map(r => if (r.getDeletedCount > 0) Ok("ok") else NotFound)
  }

  def retrieveSubscriptionDataUtr(utr: Utr): Action[AnyContent] = auth.authCBCR { _ =>
    repo
      .get(utr)
      .map {
        case None          => NotFound
        case Some(details) => Ok(Json.toJson(details))
      }
  }

  def retrieveSubscriptionDataCBCId(cbcId: CBCId): Action[AnyContent] = auth.authCBCR { _ =>
    repo
      .get(cbcId)
      .map {
        case None          => NotFound
        case Some(details) => Ok(Json.toJson(details))
      }
  }
}
