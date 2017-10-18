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
import play.api.libs.json.Json
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
          ContactName(s.subscriberContact.firstName.getOrElse(""), s.subscriberContact.lastName.getOrElse(""))
        )
      )
    }
  } 

  val doMigration: Boolean = configuration.underlying.get[Boolean]("CBCId.performMigration").valueOr(_ => false)

  if (doMigration) {

    Await.result(repo.getSubscriptions(DataMigrationCriteria.LOCAL_CBCID_CRITERIA).map { list =>
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

  val doFirstNameLastNameDataFix: Boolean = configuration.underlying.get[Boolean]("CBCId.doFirstNameLastNameDataFix").valueOr(_ => false)

  def splitName(name: Option[String]): (Option[String], Option[String]) = {
    name.fold[(Option[String], Option[String])]((None, None))(n => {
      val lst = n.split(" ").map(_.trim).toList
      val lastName = lst.last
      val firstName = lst.filter(f => f != lastName).mkString(" ")
      if(firstName.isEmpty)
        (Some(lastName), Some(lastName))
      else
        (Some(firstName), Some(lastName))
    })
  }

  if(doFirstNameLastNameDataFix) {
    Await.result(repo.getSubscriptions(DataMigrationCriteria.NAME_SPLIT_CRITERIA).map {
      list => {
        Logger.warn(s"Found ${list.size} Subscriptions to be fixed")
        val fixedList = list.map(sd => SubscriptionDetails(sd.businessPartnerRecord,
          SubscriberContact(name = None, splitName(sd.subscriberContact.name)._1,
            splitName(sd.subscriberContact.name)._2, sd.subscriberContact.phoneNumber, sd.subscriberContact.email), sd.cbcId, sd.utr))
        fixedList.foreach(f => {
          f.cbcId.fold(())(cbcid => repo.update(cbcid, f.subscriberContact))
          Logger.warn(s"Fixed ${f.subscriberContact}")
        })
      }
    }, 10.minutes )
  }

}
