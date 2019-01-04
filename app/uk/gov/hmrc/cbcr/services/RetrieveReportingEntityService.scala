/*
 * Copyright 2019 HM Revenue & Customs
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
import uk.gov.hmrc.cbcr.audit.AuditConnectorI
import uk.gov.hmrc.cbcr.repositories.ReportingEntityDataRepo
import play.api.{Configuration, Logger}
import uk.gov.hmrc.cbcr.models.DocRefId

import scala.concurrent.{ExecutionContext, Future}

class RetrieveReportingEntityService @Inject() (repo:ReportingEntityDataRepo,
                                                configuration:Configuration,
                                                runMode: RunMode,
                                                audit: AuditConnectorI) (implicit ex: ExecutionContext) {

  val retrieveReportingEntity: Boolean = configuration.getBoolean(s"${runMode.env}.retrieve.ReportingEntity").getOrElse(false)
  Logger.info(s"auditSubscriptions set to: $retrieveReportingEntity")

  if (retrieveReportingEntity) {
    val docRefId: DocRefId = DocRefId(configuration.getString(s"${runMode.env}.retrieve.docRefId").getOrElse(""))
    Logger.info(s"docRefId to retireve = ${docRefId.id}")

    repo.query(docRefId).map(red => red match {
      case Some(red) => Logger.info(s"reportingEntityData for docRefId ${docRefId.id} = ${Json.toJson(red)}")
      case _ => Logger.info(s"No ReportingEntityData found for docRefId ${docRefId.id}")
    })
  }
}
