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

package uk.gov.hmrc.cbcr.models

import play.api.libs.json.{Json, OFormat}

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, OffsetDateTime}
import scala.xml.NodeSeq

case class SubmissionDetails(cbcId: String, submissionTime: LocalDateTime, fileName: String)

object SubmissionDetails {

  def build(xml: NodeSeq, fileName: String, cbcId: String, submissionTime: LocalDateTime): SubmissionDetails =
    SubmissionDetails(
      cbcId = cbcId,
      submissionTime = submissionTime,
      fileName = fileName
    )

  implicit val format: OFormat[SubmissionDetails] = Json.format[SubmissionDetails]
}

case class SubmissionMetaData(submissionTime: String, conversationID: String, fileName: Option[String])

object SubmissionMetaData {
  val dateTimeFormat: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT

  def build(submissionTime: LocalDateTime, conversationID: String, fileName: String): SubmissionMetaData =
    SubmissionMetaData(
      dateTimeFormat.format(submissionTime.toInstant(OffsetDateTime.now().getOffset)),
      conversationID,
      Option(fileName)
    )
}