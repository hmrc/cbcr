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

package uk.gov.hmrc.cbcr.services

import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{Json, Reads}
import play.api.mvc.Result
import play.api.mvc.Results._
import uk.gov.hmrc.cbcr.LoggerOps
import uk.gov.hmrc.cbcr.connectors.DESConnector
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemoteSubscription @Inject() (des: DESConnector)(implicit executionContext: ExecutionContext)
    extends SubscriptionHandler {

  lazy val logger: Logger = Logger(this.getClass)

  def checkResponse[T: Reads](response: HttpResponse)(f: T => Result)(implicit hc: HeaderCarrier): Result = {
    logger.info(s"Response body: ${response.body}")
    response.status match {
      case OK =>
        if (response.json != null) {
          response.json
            .validate[T]
            .fold[Result](
              errors => {
                logger.kibanaError(s"DES: Unable to de-serialise response:\n${response.body}\nErrors: $errors")
                InternalServerError
              },
              (t: T) => f(t)
            )
        } else {
          logger.kibanaError(s"DES: Response wasn't json. Got:\n${response.body}")
          InternalServerError
        }
      case FORBIDDEN   => Forbidden
      case NOT_FOUND   => NotFound
      case BAD_REQUEST => BadRequest
      case SERVICE_UNAVAILABLE =>
        logger.kibanaError(s"DES: Unavailable")
        ServiceUnavailable
      case status =>
        logger.kibanaError(s"DES: Got unexpected status $status with body:\n${response.body}")
        InternalServerError
    }
  }

  override def createSubscription(sub: SubscriptionRequest)(implicit hc: HeaderCarrier): Future[Result] =
    des
      .createSubscription(sub)
      .map(response =>
        checkResponse[SubscriptionResponse](response)(r => Ok(Json.obj("cbc-id" -> r.cbcSubscriptionID.value)))
      )

  override def updateSubscription(safeId: String, details: CorrespondenceDetails)(implicit
    headerCarrier: HeaderCarrier
  ): Future[Result] =
    des
      .updateSubscription(safeId, details)
      .map(response => checkResponse[UpdateResponse](response)(r => Ok(UpdateResponse.format.writes(r))))

  override def getSubscription(safeId: String)(implicit headerCarrier: HeaderCarrier): Future[Result] =
    des.getSubscription(safeId).map { response =>
      checkResponse[GetResponse](response)(r => Ok(GetResponse.format.writes(r)))
    }

}
