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

package uk.gov.hmrc.cbcr.services

import javax.inject.{Inject, Singleton}

import uk.gov.hmrc.cbcr.connectors.DESConnector
import uk.gov.hmrc.cbcr.models.{SubscriptionRequestBody2, SubscriptionRequestResponse}
import play.api.Logger
import play.api.mvc.Results._
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

import play.api.Logger

@Singleton
class RemoteCBCIdGenerator @Inject() (val des:DESConnector) {

  def generateCBCId(sub:SubscriptionRequestBody2)(implicit hc:HeaderCarrier, ec:ExecutionContext): Future[Result] = {
    Logger.info("in generateCBCId")
    des.subscribeToCBC(sub).map(response =>
      response.json.validate[SubscriptionRequestResponse].fold(
        _   => response.status match {
          case FORBIDDEN             => Forbidden
          case BAD_REQUEST           => BadRequest
          case INTERNAL_SERVER_ERROR =>
            Logger.error("internal server error")
            InternalServerError
          case SERVICE_UNAVAILABLE   => ServiceUnavailable
          case other                 =>
            Logger.error(s"DES returned an undocumented ErrorCode: $other")
            InternalServerError
        },
        response => Ok(Json.obj("cbc-id" -> response.cbcSubscriptionID.value))
      )
    )
  }

}
