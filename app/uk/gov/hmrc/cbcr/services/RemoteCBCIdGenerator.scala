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

import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.mvc.Results._
import uk.gov.hmrc.cbcr.connectors.DESConnector
import uk.gov.hmrc.cbcr.models.{SubscriptionRequest, SubscriptionResponse}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemoteCBCIdGenerator @Inject()(val des: DESConnector) {

  def generateCBCId(sub: SubscriptionRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    Logger.info("in generateCBCId")

    def checkErrorStatus(response: HttpResponse) = {
      Logger.error(s"Error calling DES: Body: [${response.body}] Status: ${response.status} Headers: ${response.allHeaders}")
      response.status match {
        case FORBIDDEN => Forbidden
        case BAD_REQUEST => BadRequest
        case INTERNAL_SERVER_ERROR => InternalServerError
        case SERVICE_UNAVAILABLE => ServiceUnavailable
        case other =>
          InternalServerError
      }
    }


    des.subscribeToCBC(sub).map { response =>

      if (response.json != null) {
        response.json.validate[SubscriptionResponse].fold(errors => {
          Logger.error(s"JsonErrors found validating SubscriptionRequestResponse ${errors}")
          checkErrorStatus(response)},
          response => Ok(Json.obj("cbc-id" -> response.cbcSubscriptionID.value))
        )

      } else {
        checkErrorStatus(response)
      }
    }
  }


}
