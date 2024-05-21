/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.mvc.Results.Unauthorized
import play.api.mvc.{ActionRefiner, ControllerComponents, Request, Result}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.affinityGroup
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthenticatedAction @Inject()(val authConnector: AuthConnector, cc: ControllerComponents)(
  implicit val executionContext: ExecutionContext)
    extends ActionRefiner[Request, Request] with AuthorisedFunctions {
  private def isAgentOrOrganisation(group: AffinityGroup): Boolean =
    group.toString.contains("Agent") || group.toString.contains("Organisation")

  private val AuthProvider: AuthProviders = AuthProviders(GovernmentGateway)

  override def refine[A](request: Request[A]): Future[Either[Result, Request[A]]] = {
    implicit val hc = HeaderCarrierConverter.fromRequest(request)
    authorised(AuthProvider)
      .retrieve(affinityGroup) {
        case Some(affinityG) if isAgentOrOrganisation(affinityG) => Future.successful(Right(request))
        case _                                                   => Future.successful(Left(Unauthorized))
      }
      .recover {
        case e: NoActiveSession => Left(Unauthorized(e.reason))
      }
  }
}
