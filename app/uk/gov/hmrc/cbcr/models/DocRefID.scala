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

import java.time.format.DateTimeFormatter
import play.api.libs.json._
import play.api.mvc.PathBindable
import org.apache.commons.codec.net.URLCodec

case class DocRefId(id: String)
object DocRefId {

  implicit val pathFormat: PathBindable[DocRefId] = new PathBindable[DocRefId] {
    override def bind(key: String, value: String): Either[String, DocRefId] = {
      val decodedValue = new URLCodec().decode(value)
      Right(DocRefId(decodedValue))
    }
    override def unbind(key: String, value: DocRefId): String = value.id
  }

  implicit val format: Format[DocRefId] = new Format[DocRefId] {
    override def writes(o: DocRefId): JsValue = JsString(o.id)

    override def reads(json: JsValue): JsResult[DocRefId] =
      json
        .asOpt[JsString]
        .map(v => DocRefId(v.value))
        .fold[JsResult[DocRefId]](JsError(s"Unable to deserialise $json as a DocRefId"))((id: DocRefId) =>
          JsSuccess(id)
        )
  }
}
case class CorrDocRefId(cid: DocRefId)
object CorrDocRefId {

  implicit val pathFormat: PathBindable[CorrDocRefId] = new PathBindable[CorrDocRefId] {
    override def bind(key: String, value: String): Either[String, CorrDocRefId] = {
      val decodedValue = new URLCodec().decode(value)
      Right(CorrDocRefId(DocRefId(decodedValue)))
    }
    override def unbind(key: String, value: CorrDocRefId): String = value.cid.id
  }

  implicit val format: Format[CorrDocRefId] = new Format[CorrDocRefId] {
    override def writes(o: CorrDocRefId): JsValue = JsString(o.cid.id)
    override def reads(json: JsValue): JsResult[CorrDocRefId] = DocRefId.format.reads(json).map(CorrDocRefId(_))
  }

}

case class DocRefIdRecord(id: DocRefId, valid: Boolean)
object DocRefIdRecord {
  // Keep in sync with any future frontend changes
  implicit val format: OFormat[DocRefIdRecord] = Json.format[DocRefIdRecord]
  val dateFmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
  val cbcRegex: String = CBCId.cbcRegex.init.tail // strip the ^ and $ characters from the cbcRegex
  val dateRegex = """\d{8}T\d{6}"""
  val messageRefIDRegex = ("""GB(\d{4})(\w{2})(""" + cbcRegex + """)(CBC40[1,2])(""" + dateRegex + """)(\w{1,56})""").r
  val docRefIdRegex = s"""($messageRefIDRegex)_(.{1,30})(OECD[0123])(ENT|REP|ADD)(.{0,25})""".r

  def extractDocTypeIndicator(docRefId: String): Option[String] = docRefId match {
    case docRefIdRegex(_, _, _, _, _, _, _, _, docType, _, _) => Some(docType)
    case _                                                    => None
  }

  def docRefIdValidity(docRefId: String): Boolean = DocRefIdRecord.extractDocTypeIndicator(docRefId) match {
    case Some(docTypeIndicator: String) if docTypeIndicator == "OECD3" => false
    case _                                                             => true
  }
}

object DocRefIdResponses {

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
