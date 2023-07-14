/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.libs.json.{Json, Writes}

case class MigrationRequest(safeId: String, cBCId: String, correspondenceDetails: CorrespondenceDetails)

object MigrationRequest {
  implicit val migrationWriter: Writes[MigrationRequest] = new Writes[MigrationRequest] {
    override def writes(o: MigrationRequest) = Json.obj(
      "safeId"            -> o.safeId,
      "cbcRegNumber"      -> o.cBCId,
      "isMigrationRecord" -> true,
      "correspondenceDetails" -> Json.obj(
        "contactAddress" -> EtmpAddress.formats.writes(o.correspondenceDetails.contactAddress),
        "contactDetails" -> Json.obj(
          "emailAddress" -> o.correspondenceDetails.contactDetails.email.value,
          "phoneNumber"  -> o.correspondenceDetails.contactDetails.phoneNumber.number
        ),
        "contactName" -> ContactName.format.writes(o.correspondenceDetails.contactName)
      )
    )

  }
}
