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

package uk.gov.hmrc.cbcr.auth

import javax.inject.{Inject, Singleton}
import play.api.libs.json.JsValue
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.affinityGroup
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CBCRAuth @Inject()(val microServiceAuthConnector: AuthConnector,
                         cc: ControllerComponents)(implicit val ec: ExecutionContext) extends BackendController(cc) with AuthorisedFunctions {
  override def authConnector: AuthConnector = microServiceAuthConnector

  private val AuthProvider: AuthProviders = AuthProviders(GovernmentGateway)
  private type AuthAction[A] = Request[A] => Future[Result]

  private def isAgentOrOrganisation(group: AffinityGroup): Boolean = {
    group.toString.contains("Agent") || group.toString.contains("Organisation")
  }

  def authCBCR(action: AuthAction[AnyContent]): Action[AnyContent] = Action.async {
    implicit request ⇒ authCommon(action)
  }

  def authCBCRWithJson(action: AuthAction[JsValue], json: BodyParser[JsValue]): Action[JsValue] =
    Action.async(json) { implicit request ⇒ authCommon(action) }

  def authCommon[A](action: AuthAction[A])(implicit request: Request[A]): Future[Result] = {
    implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers)
    authorised(AuthProvider).retrieve(affinityGroup) {
      case Some(affinityG) if isAgentOrOrganisation(affinityG) ⇒ action(request)
      case _ => Future.successful(Unauthorized)
    }.recover[Result] {
      case e: NoActiveSession => Unauthorized(e.reason)
    }
  }
}
