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

import configs.syntax._
import play.api.{Configuration, Logger}
import uk.gov.hmrc.cbcr.repositories.{DocRefIdRepository, MessageRefIdRepository, Purgeable, ReportingEntityDataRepo}

import scala.util.{Failure, Success, Try}

class PurgeSubscriptionDataService @Inject()(configuration:Configuration, repEntRepo:ReportingEntityDataRepo,
                                             docRefIdRepo:DocRefIdRepository, mesRefIdRepo:MessageRefIdRepository) {


  val purgeables: List[Purgeable] =  List(repEntRepo, docRefIdRepo, mesRefIdRepo)
  val purgeSubscriptionData: Boolean = configuration.underlying.get[Boolean]("CBCId.purgeSubscriptionData").valueOr(_ => false)

  if(purgeSubscriptionData) {
    Logger.warn("About to delete all submission data")

    Try(
      purgeables.map(purgeable => purgeable.deleteAll())
    ) match {
      case Success(_) => Logger.warn("Completed purging all the Submission Data")
      case Failure(e) => Logger.error("Failed to purge the Submission Data", e)
    }

  }

}
