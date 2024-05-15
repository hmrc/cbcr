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
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.cbcr.auth.AuthenticatedAction
import uk.gov.hmrc.cbcr.connectors.DESConnector
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.repositories.SubscriptionDataRepository
import uk.gov.hmrc.mongo.play.json.Codecs
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class SubscriptionDataController @Inject()(
  repo: SubscriptionDataRepository,
  des: DESConnector,
  auth: AuthenticatedAction,
  configuration: Configuration,
  cc: ControllerComponents)(implicit val ec: ExecutionContext)
    extends BackendController(cc) {

  def saveSubscriptionData(): Action[SubscriptionDetails] =
    Action(parse.json[SubscriptionDetails]).andThen(auth).async { request =>
      repo.save2(request.body).map(_ => Ok)
    }

  def updateSubscriberContactDetails(cbcId: CBCId): Action[SubscriberContact] =
    Action(parse.json[SubscriberContact]).andThen(auth).async { request =>
      repo.update(equal("cbcId", Codecs.toBson(cbcId)), request.body).map {
        case true  => Ok
        case false => InternalServerError
      }
    }

  def clearSubscriptionData(cbcId: CBCId): Action[AnyContent] = Action.andThen(auth).async {
    repo
      .clearCBCId(cbcId)
      .map(r => if (r.getDeletedCount > 0) Ok("ok") else NotFound)
  }

  def retrieveSubscriptionDataUtr(utr: Utr): Action[AnyContent] = Action.andThen(auth).async {
    repo
      .get(utr)
      .map {
        case None          => NotFound
        case Some(details) => Ok(Json.toJson(details))
      }
  }

  def retrieveSubscriptionDataCBCId(cbcId: CBCId): Action[AnyContent] = Action.andThen(auth).async {
    repo
      .get(cbcId)
      .map {
        case None          => NotFound
        case Some(details) => Ok(Json.toJson(details))
      }
  }
}
