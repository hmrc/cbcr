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

package uk.gov.hmrc.cbcr.services

import play.api.http.Status.OK
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.cbcr.connectors.SubscriptionConnector
import uk.gov.hmrc.cbcr.models.subscription.SubscriptionDetails
import uk.gov.hmrc.cbcr.models.subscription.request.{DisplayRequestDetail, DisplaySubscriptionDetails, DisplaySubscriptionForCBCRequest, RequestCommonForDisplay}
import uk.gov.hmrc.cbcr.models.subscription.response.{DisplaySubscriptionForCBCResponse, ResponseDetail}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ContactService @Inject()(
  subscriptionCacheService: SubscriptionCacheService,
  subscriptionConnector: SubscriptionConnector) {

  def getLatestContacts(
    enrolmentID: String)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[SubscriptionDetails] =
    retrieveContactFromCacheOrHOD(enrolmentID: String) map { retrievedSubscription =>
      retrievedSubscription.map { sub =>
        val details: ResponseDetail = sub.displaySubscriptionForCBCResponse.responseDetail

        SubscriptionDetails(
          subscriptionID = details.subscriptionID,
          tradingName = details.tradingName,
          isGBUser = details.isGBUser,
          primaryContact = details.primaryContact.contactInformation,
          secondaryContact = details.secondaryContact.map(_.contactInformation)
        )

      } getOrElse (throw new Exception("Failed to retrieve and convert subscription"))
    }

  def retrieveContactFromCacheOrHOD(
    enrolmentID: String
  )(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Option[DisplaySubscriptionForCBCResponse]] = {
    val subscriptionForCBCRequest: DisplaySubscriptionForCBCRequest =
      DisplaySubscriptionForCBCRequest(
        DisplaySubscriptionDetails(
          RequestCommonForDisplay.apply(),
          DisplayRequestDetail(IDType = "CBC", IDNumber = enrolmentID)
        )
      )

    subscriptionCacheService.retrieveSubscriptionDetails(enrolmentID).flatMap {
      case Some(a) => Future.successful(Some(a))
      case None =>
        subscriptionConnector.displaySubscriptionForCBC(subscriptionForCBCRequest).map { response =>
          response.status match {
            case OK =>
              response.json.validate[DisplaySubscriptionForCBCResponse] match {
                case JsSuccess(response, _) => Some(response)
                case JsError(_)             => None
              }
            case _ => None
          }
        }
    }
  }

}
