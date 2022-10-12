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

import javax.inject.Inject
import play.api.libs.json.Json
import uk.gov.hmrc.cbcr.repositories.ReportingEntityDataRepo
import play.api.{Configuration, Logger}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import scala.concurrent.ExecutionContext

class RetrieveReportingEntityService @Inject()(
  repo: ReportingEntityDataRepo,
  configuration: Configuration,
  runMode: RunMode,
  audit: AuditConnector)(implicit ex: ExecutionContext) {

  lazy val logger: Logger = Logger(this.getClass)

  val retrieveReportingEntity: Boolean =
    configuration.getOptional[Boolean](s"${runMode.env}.retrieve.ReportingEntity").getOrElse(false)

  logger.info(s"retrieveReportingEntity set to: $retrieveReportingEntity")

  if (retrieveReportingEntity) {
    val docRefId: String = configuration.getOptional[String](s"${runMode.env}.retrieve.docRefId").getOrElse("")
    logger.info(s"docRefId to retireve = $docRefId")

    repo
      .query(docRefId)
      .map(
        red =>
          if (red.nonEmpty)
            red.foreach(r => logger.info(s"reportingEntityData for doc RefId $docRefId = ${Json.toJson(r)}"))
          else logger.info(s"no reportingEntityData found for docRefIds matching $docRefId"))
  }
}
