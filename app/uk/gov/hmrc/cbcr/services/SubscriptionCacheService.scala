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

import uk.gov.hmrc.cbcr.models.subscription.request.CreateSubscriptionForCBCRequest
import uk.gov.hmrc.cbcr.models.subscription.response.{DisplaySubscriptionForCBCResponse, ResponseCommon, ResponseDetail, SubscriptionForCBCResponse}
import uk.gov.hmrc.cbcr.models.subscription._
import uk.gov.hmrc.cbcr.repositories.SubscriptionCacheRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubscriptionCacheService @Inject()(cacheRepository: SubscriptionCacheRepository) {

  def storeSubscriptionDetails(id: String, subscription: CreateSubscriptionForCBCRequest): Future[Boolean] =
    cacheRepository.set(id, subscription)

  def retrieveSubscriptionDetails(id: String)(
    implicit ec: ExecutionContext): Future[Option[DisplaySubscriptionForCBCResponse]] =
    //Fake response message from our cached details
    cacheRepository.get(id).map { result =>
      result.map { subRequest =>
        val requestDetail = subRequest.subscription.requestDetail
        DisplaySubscriptionForCBCResponse(
          SubscriptionForCBCResponse(
            ResponseCommon("", None, "", None),
            ResponseDetail(
              subRequest.subscriptionID,
              requestDetail.tradingName,
              requestDetail.isGBUser,
              PrimaryContact(requestDetail),
              requestDetail.secondaryContact.map(SecondaryContact(_))
            )
          )
        )
      }
    }

}
