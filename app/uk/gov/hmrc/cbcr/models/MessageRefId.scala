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

package uk.gov.hmrc.cbcr.models

import play.api.libs.json._

case class MessageRefId(id:String)
object MessageRefId {
  implicit val format = new OFormat[MessageRefId] {
    override def writes(o: MessageRefId): JsObject = Json.obj("messageRefId" -> o.id)

    override def reads(json: JsValue): JsResult[MessageRefId] = json match {
      case o:JsObject =>
        val result = o.value.get("messageRefId").flatMap(_.asOpt[String])
        result.fold[JsResult[MessageRefId]](JsError(s"Unable to parse MessageRefId: $o"))(v => JsSuccess(MessageRefId(v)))
      case other => JsError(s"Unable to parse MessageRefId: $other")
    }
  }
}
