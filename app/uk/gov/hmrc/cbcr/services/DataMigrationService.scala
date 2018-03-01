/*
 * Copyright 2018 HM Revenue & Customs
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
import play.api.libs.json.Json
import play.api.{Configuration, Logger}
import uk.gov.hmrc.cbcr.connectors.DESConnector
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.repositories.SubscriptionDataRepository
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

class DataMigrationService @Inject() (repo:SubscriptionDataRepository, des:DESConnector,
                                      configuration:Configuration,
                                      runMode: RunMode) {


  private def migrationRequest(s: SubscriptionDetails): Option[MigrationRequest] = {
    s.cbcId.map { id =>
      MigrationRequest(
        s.businessPartnerRecord.safeId,
        id.value,
        CorrespondenceDetails(
          s.businessPartnerRecord.address,
          ContactDetails(s.subscriberContact.email, s.subscriberContact.phoneNumber),
          ContactName(s.subscriberContact.firstName.getOrElse(""), s.subscriberContact.lastName.getOrElse(""))
        )
      )
    }
  }

  val doMigration: Boolean = configuration.underlying.get[Boolean](s"${runMode.env}.CBCId.performMigration").valueOr(_ => false)
  Logger.info(s"doMigration set to: $doMigration")

  if (doMigration) {
    val output = repo.getSubscriptions(DataMigrationCriteria.LOCAL_CBCID_CRITERIA).flatMap { list =>
      Logger.warn(s"Migrating old CBCId to ETMP as idempotent function: got ${list.size} subscriptions to migrate from mongo")
      list.foldLeft(Future.successful(List.empty[String]))((eventualStrings: Future[List[String]], details: SubscriptionDetails) =>
        migrationRequest(details).fold(eventualStrings)(mr => eventualStrings.flatMap(ls => migrate(mr).map(s => s::ls)))
      )
    }

    output.map(msgs => Logger.info(msgs.mkString("\n")))

  }

  private def migrate(mr: MigrationRequest): Future[String] = {
    des.createMigration(mr).map(res =>
      if (res.status == 200) s"${mr.cBCId} -------> Migrated"
      else s"${mr.cBCId} -------> FAILED with status code ${res.status}\n${res.body}")
  }

}
