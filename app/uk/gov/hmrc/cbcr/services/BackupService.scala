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

import javax.inject.Inject

import play.api.libs.json.Json
import play.api.{Configuration, Logger}
import uk.gov.hmrc.cbcr.models.SubscriptionDetails
import uk.gov.hmrc.cbcr.repositories.SubscriptionDataRepository

import scala.concurrent.Await
import scala.concurrent.duration._

class BackupService @Inject()(configuration:Configuration, subscriptionDetailsRepo: SubscriptionDataRepository) {


  def backup: Boolean =  {
    Logger.warn(s"Backing up Subscription Data to a new Collection")
    val lsd: List[SubscriptionDetails] = Await.result(subscriptionDetailsRepo.getSubscriptions(Json.obj()), 10 seconds)
    val result = lsd.size == subscriptionDetailsRepo.backup(lsd).map(f => Await.result(f, 10 seconds)).map(wr => wr.n).sum
    if(result)
      Logger.warn("Backup of Subscription data successful")
    else {
      Logger.error("Backup of Subscription data failed")
    }

    result
  }


}
