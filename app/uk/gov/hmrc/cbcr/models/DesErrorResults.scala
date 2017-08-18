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

package uk.gov.hmrc.cbcr.models

import play.api.libs.json.Json.toJson
import play.api.libs.json.{Json, Writes}
import play.api.mvc.Results.{BadRequest, Conflict, NotFound}

object DesErrorResults {

  case class DesErrorBody(code: String, reason: String)

  implicit val errorBodyWrites: Writes[DesErrorBody] = Json.writes[DesErrorBody]

  val GenericNotFound = genericNotFound("The remote endpoint has indicated that no data can be found.")
  def genericNotFound(reason: String) = NotFound(toJson(DesErrorBody("NOT_FOUND", reason)))
  val InvalidUtr =      BadRequest(toJson(DesErrorBody("INVALID_UTR", "Submission has not passed validation. Invalid parameter UTR.")))
  val InvalidPayload =  BadRequest(toJson(DesErrorBody("INVALID_PAYLOAD", "Submission has not passed validation. Invalid Payload.")))
  val SubscriptionExists =  Conflict(toJson(DesErrorBody("CONFLICT", "This UTR is already subscribed to MTD")))

}