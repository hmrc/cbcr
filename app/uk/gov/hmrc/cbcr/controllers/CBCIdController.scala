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

import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.cbcr.auth.AuthenticatedAction
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.services.SubscriptionHandlerImpl
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject._

@Singleton
class CBCIdController @Inject()(gen: SubscriptionHandlerImpl, auth: AuthenticatedAction, cc: ControllerComponents)
    extends BackendController(cc) {

  def subscribe: Action[SubscriptionDetails] =
    Action(parse.json[SubscriptionDetails]).andThen(auth).async { implicit request =>
      gen.createSubscription(request.body)
    }

  def updateSubscription(safeId: String): Action[CorrespondenceDetails] =
    Action(parse.json[CorrespondenceDetails]).andThen(auth).async { implicit request =>
      gen.updateSubscription(safeId, request.body)
    }

  def getSubscription(safeId: String): Action[AnyContent] = Action.andThen(auth).async { implicit request =>
    gen.getSubscription(safeId)
  }

  @inline implicit private def subscriptionDetailsToSubscriptionRequestBody(
    s: SubscriptionDetails): SubscriptionRequest =
    SubscriptionRequest(
      s.businessPartnerRecord.safeId,
      false,
      CorrespondenceDetails(
        s.businessPartnerRecord.address,
        ContactDetails(s.subscriberContact.email, s.subscriberContact.phoneNumber),
        ContactName(s.subscriberContact.firstName, s.subscriberContact.lastName)
      )
    )

}
