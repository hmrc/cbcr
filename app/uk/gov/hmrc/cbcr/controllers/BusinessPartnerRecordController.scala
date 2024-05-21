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

import play.api.mvc.ControllerComponents
import uk.gov.hmrc.cbcr.auth.AuthenticatedAction
import uk.gov.hmrc.cbcr.connectors.DESConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class BusinessPartnerRecordController @Inject()(
  connector: DESConnector,
  auth: AuthenticatedAction,
  cc: ControllerComponents)(implicit val ec: ExecutionContext)
    extends BackendController(cc) {

  def getBusinessPartnerRecord(utr: String) = Action.andThen(auth).async {
    connector.lookup(utr).map {
      case response if response.status == OK          => Ok(response.json)
      case response if response.status == BAD_REQUEST => BadRequest(response.json)
      case response if response.status == NOT_FOUND   => NotFound
      case _                                          => InternalServerError
    }
  }

}
