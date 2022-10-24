/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.Logger
import play.api.libs.json.{JsResult, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents, Result}
import uk.gov.hmrc.cbcr.auth.CBCRAuth
import uk.gov.hmrc.cbcr.connectors.SubscriptionConnector
import uk.gov.hmrc.cbcr.models.subscription.request.{CreateSubscriptionForCBCRequest, DisplaySubscriptionForCBCRequest}
import uk.gov.hmrc.cbcr.models.upscan.ErrorDetails
import uk.gov.hmrc.cbcr.services.SubscriptionCacheService
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class SubscriptionCacheController @Inject()(
  auth: CBCRAuth,
  subscriptionCacheService: SubscriptionCacheService,
  subscriptionConnector: SubscriptionConnector,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  private val logger: Logger = Logger(this.getClass)

  def storeSubscriptionDetails(): Action[JsValue] =
    auth.authCBCRWithJson(
      { implicit request =>
        val subscriptionRequest = request.body.validate[CreateSubscriptionForCBCRequest]

        subscriptionRequest.fold(
          invalid = _ => Future.successful(BadRequest("")),
          valid = createSubscription =>
            subscriptionCacheService
              .storeSubscriptionDetails(createSubscription.subscriptionID, createSubscription)
              .map { _ =>
                Ok
            }
        )
      },
      parse.json
    )

  def retrieveSubscription: Action[JsValue] =
    auth.authCBCRWithJson(
      { implicit request =>
        val displaySubscriptionResult: JsResult[DisplaySubscriptionForCBCRequest] =
          request.body.validate[DisplaySubscriptionForCBCRequest]

        displaySubscriptionResult.fold(
          invalid = _ => Future.successful(BadRequest("")),
          valid = subResult =>
            subscriptionCacheService
              .retrieveSubscriptionDetails(subResult.displaySubscriptionForCBCRequest.requestDetail.IDNumber)
              .flatMap {
                case Some(result) => Future.successful(Ok(Json.toJson(result)))
                case None =>
                  for {
                    httpResponse <- subscriptionConnector.displaySubscriptionForCBC(subResult)
                  } yield convertToResult(httpResponse)
            }
        )
      },
      parse.json
    )

  private def convertToResult(httpResponse: HttpResponse): Result =
    httpResponse.status match {
      case OK        => Ok(httpResponse.body)
      case NOT_FOUND => NotFound(httpResponse.body)

      case BAD_REQUEST =>
        logDownStreamError(httpResponse.body)
        BadRequest(httpResponse.body)

      case FORBIDDEN =>
        logDownStreamError(httpResponse.body)
        Forbidden(httpResponse.body)

      case METHOD_NOT_ALLOWED =>
        logDownStreamError(httpResponse.body)
        MethodNotAllowed(httpResponse.body)

      case CONFLICT =>
        logDownStreamError(httpResponse.body)
        Conflict(httpResponse.body)

      case INTERNAL_SERVER_ERROR =>
        logDownStreamError(httpResponse.body)
        InternalServerError(httpResponse.body)

      case _ =>
        logDownStreamError(httpResponse.body)
        ServiceUnavailable(httpResponse.body)
    }

  private def logDownStreamError(body: String): Unit = {
    val error = Try(Json.parse(body).validate[ErrorDetails])
    error match {
      case Success(JsSuccess(value, _)) =>
        logger.error(s"Error with submission: $value")
      case _ => logger.error("Error with submission but return is not a valid json")
    }
  }
}
