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

import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.cbcr.util.SpecBase

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SubmissionDetailsSpec extends SpecBase {

  val submissionTime = LocalDateTime.now

  val submissionDetails = SubmissionDetails(
    cbcId = "enrolmentID",
    submissionTime = submissionTime,
    fileName = "fileName"
  )

  "SubmissionDetails" should {

    "must serialise SubmissionDetails in API calls" in {

      val json: JsObject = Json.obj(
        "cbcId"          -> "enrolmentID",
        "submissionTime" -> submissionTime.format(DateTimeFormatter.ISO_DATE_TIME),
        "fileName"       -> "fileName"
      )
      Json.toJson(submissionDetails).toString shouldBe json.toString
    }
  }
}
