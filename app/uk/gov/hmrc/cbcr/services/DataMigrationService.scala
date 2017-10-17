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

import cats.instances.all._
import cats.syntax.all._
import configs.syntax._
import play.api.{Configuration, Logger}
import uk.gov.hmrc.cbcr.connectors.DESConnector
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.repositories.SubscriptionDataRepository

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._


class DataMigrationService @Inject() (repo:SubscriptionDataRepository, des:DESConnector,
                                      configuration:Configuration) {


  private def migrationRequest(s: SubscriptionDetails): Option[MigrationRequest] = {
    s.cbcId.map { id =>
      MigrationRequest(
        s.businessPartnerRecord.safeId,
        id.value,
        CorrespondenceDetails(
          s.businessPartnerRecord.address,
          ContactDetails(s.subscriberContact.email, s.subscriberContact.phoneNumber),
          ContactName(s.subscriberContact.firstName, s.subscriberContact.lastName)
        )
      )
    }
  } 

  val doMigration: Boolean = configuration.underlying.get[Boolean]("CBCId.performMigration").valueOr(_ => false)

  if (doMigration) {
    Await.result(repo.getAllMigrations().map { list =>
      Logger.warn(s"Migrating old CBCId to ETMP as idempotent function: got ${list.size} subscriptions to migrate from mongo")
      list.foreach { sd =>
        migrationRequest(sd).fold(Logger.error(s"No cbcID found for $sd")
        )(mr => Await.result(des.createMigration(mr).map(sd -> _), 1.minute).map { res =>
          if (res.status != 200) {
            Logger.error(s"${sd.cbcId} -------> FAILED with status code ${res.status}\n${res.body}")
          } else {
            Logger.warn(s"${sd.cbcId} -------> Migrated")
          }
        })
      }
    }, 10.minutes)
  }

}
