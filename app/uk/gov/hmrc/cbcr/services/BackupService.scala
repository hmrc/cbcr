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
import cats.syntax.all._
import cats.instances.all._
import reactivemongo.api.commands.WriteResult

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import configs.syntax._

class BackupService @Inject()(configuration:Configuration, subscriptionDetailsRepo: SubscriptionDataRepository)(implicit ec:ExecutionContext) {

  val doBackup: Boolean     = configuration.underlying.get[Boolean]("CBCId.performBackup").valueOr(_ => false)

  if(doBackup) {
    backup()
  }

  def backup()  =  {
    Logger.warn(s"Backing up Subscription Data to a new Collection")
    subscriptionDetailsRepo.getSubscriptions(Json.obj()).flatMap { lsd =>
      subscriptionDetailsRepo.backup(lsd).sequence[Future,WriteResult].map { l =>
        val total = l.map(_.n).sum
        if(total != lsd.size){
          "Backup of Subscription data failed"
        } else {
          "Backup of Subscription data successful"
        }
      }
    }.onComplete{
      case Success(s) => Logger.warn(s)
      case Failure(f) => Logger.error(f.getMessage(), f)
    }
  }


}
