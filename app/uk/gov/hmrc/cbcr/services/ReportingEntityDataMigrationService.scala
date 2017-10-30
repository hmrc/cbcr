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

import cats.data.NonEmptyList
import configs.syntax._
import play.api.{Configuration, Logger}
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.repositories.ReportingEntityDataRepo

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._


class ReportingEntityDataMigrationService @Inject()(repo:ReportingEntityDataRepo, configuration:Configuration) {

  val doReportingEntityMigration: Boolean = configuration.underlying.get[Boolean]("CBCId.performReportingEntityMigration").valueOr(_ => false)

  if (doReportingEntityMigration) {

    def oldToNew(o:ReportingEntityDataOld):ReportingEntityData = ReportingEntityData(NonEmptyList(o.cbcReportsDRI,Nil),o.additionalInfoDRI,o.reportingEntityDRI,o.utr,o.ultimateParentEntity,o.reportingRole)

    val output = Await.result( repo.getAll.flatMap(f => Future.sequence(f.map(o => repo.update(oldToNew(o)).map(o.cbcReportsDRI -> _)))), 10.minutes)

    Logger.info(output.mkString("\n"))
  }

}
