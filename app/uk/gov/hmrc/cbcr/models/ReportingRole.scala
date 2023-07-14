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

import play.api.libs.json._

sealed trait ReportingRole

case object CBC701 extends ReportingRole
case object CBC702 extends ReportingRole
case object CBC703 extends ReportingRole
case object CBC704 extends ReportingRole

object ReportingRole {
  def parseFromString(s: String): Option[ReportingRole] = s.toLowerCase.trim match {
    case "cbc701" => Some(CBC701)
    case "cbc702" => Some(CBC702)
    case "cbc703" => Some(CBC703)
    case "cbc704" => Some(CBC704)
    case _        => None
  }

  implicit val format: Format[ReportingRole] = new Format[ReportingRole] {

    override def writes(o: ReportingRole): JsValue = JsString(o.toString)

    override def reads(json: JsValue): JsResult[ReportingRole] = json match {
      case o: JsString =>
        parseFromString(o.value)
          .fold[JsResult[ReportingRole]](JsError(s"Failed to parse $json as ReportingRole"))(JsSuccess(_))
      case _ => JsError(s"Failed to parse $json as ReportingRole")
    }
  }

}
