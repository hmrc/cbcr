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

import play.api.libs.json._
import play.api.mvc.PathBindable

case class DocRefId(id:String)
object DocRefId{

  implicit val pathFormat = new PathBindable[DocRefId] {
    override def bind(key: String, value: String): Either[String, DocRefId] = Right(DocRefId(value))
    override def unbind(key: String, value: DocRefId): String = value.id
  }

  implicit val format = new Format[DocRefId] {
    override def writes(o: DocRefId): JsValue = JsString(o.id)

    override def reads(json: JsValue): JsResult[DocRefId] = json.asOpt[JsString].map(v => DocRefId(v.value)).fold[JsResult[DocRefId]](
      JsError(s"Unable to deserialise $json as a DocRefId"))(
      (id: DocRefId) => JsSuccess(id)
    )
  }
}
case class CorrDocRefId(cid:DocRefId)
object CorrDocRefId {


  implicit val pathFormat = new PathBindable[CorrDocRefId] {
    override def bind(key: String, value: String): Either[String, CorrDocRefId] = Right(CorrDocRefId(DocRefId(value)))
    override def unbind(key: String, value: CorrDocRefId): String = value.cid.id
  }

  implicit val format = new Format[CorrDocRefId] {
    override def writes(o: CorrDocRefId): JsValue = Json.obj("CorrDocRefId" -> o.cid.id)

    override def reads(json: JsValue): JsResult[CorrDocRefId] = json match {
      case JsObject(u) => u.get("CorrDocRefId").flatMap(_.asOpt[String]).fold[JsResult[CorrDocRefId]](
        JsError(s"Unable to deserialise $json as a CorrDocRefId"))(
        id  => JsSuccess(CorrDocRefId(DocRefId(id)))
      )
      case other => JsError(s"Unable to deserialise $other as a CorreDocRefId")
    }
  }
}


case class DocRefIdRecord(id:DocRefId,valid:Boolean)
object DocRefIdRecord {
  implicit val format = Json.format[DocRefIdRecord]
}

object DocRefIdResponses{

  sealed trait DocRefIdResponses extends Product with Serializable

  sealed trait DocRefIdSaveResponse extends DocRefIdResponses

  case object Ok extends DocRefIdSaveResponse
  case object AlreadyExists extends DocRefIdSaveResponse
  case object Failed extends DocRefIdSaveResponse

  sealed trait DocRefIdQueryResponse extends DocRefIdResponses

  case object Valid extends DocRefIdQueryResponse
  case object Invalid extends DocRefIdQueryResponse
  case object DoesNotExist extends DocRefIdQueryResponse

}
