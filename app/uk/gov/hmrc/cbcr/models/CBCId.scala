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

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import play.api.libs.json._
import play.api.mvc.PathBindable
import uk.gov.hmrc.domain.Modulus23Check

/** A CBCId defined as at 15 digit reference using a modulus 23 check digit Digit 1 is an 'X' Digit 2 is the check digit
  * Digits 3-5 is the short name 'CBC' Digits 6-9 are '0000' for the private beta generated CBCIds Digits 6-9 are '0100'
  * for the public beta generated CBCIds Digits 6-9 are '1000' for the ETMP generated CBCIds Digits 10-15 are for the id
  * sequence e.g. '000001' - '999999'
  *
  * Note: This is a hard limit of 999999 unique CBCIds
  */
class CBCId private (val value: String) {
  override def toString: String = value
}

object CBCId extends Modulus23Check {

  implicit val pathFormat: PathBindable[CBCId] = new PathBindable[CBCId] {

    override def bind(key: String, value: String): Either[String, CBCId] = CBCId(value).toRight("Invalid CBCId")
    override def unbind(key: String, value: CBCId): String = value.value

  }

  implicit val cbcIdFormat: Format[CBCId] = new Format[CBCId] {
    override def writes(o: CBCId): JsValue = JsString(o.value)

    override def reads(json: JsValue): JsResult[CBCId] = json match {
      case JsString(value) =>
        CBCId(value).fold[JsResult[CBCId]](
          JsError(s"CBCId is invalid: $value")
        )(cbcid => JsSuccess(cbcid))
      case other => JsError(s"CBCId is invalid: $other")
    }
  }
  def apply(s: String): Option[CBCId] =
    if (isValidCBC(s) && isCheckCorrect(s, 1)) {
      Some(new CBCId(s))
    } else {
      None
    }

  private[models] val cbcRegex = """^X[A-Z]CBC\d{10}$"""
  private def isValidCBC(s: String): Boolean = s.matches(cbcRegex)

  def create(i: Int): Validated[Throwable, CBCId] =
    if (i > 999999 || i < 0) {
      Invalid(new IllegalArgumentException("CBCId ranges from 0-999999"))
    } else {
      val sequenceNumber = f"$i%06d"
      val id = s"CBC0100$sequenceNumber"
      val checkChar = calculateCheckCharacter(id)
      CBCId(s"X$checkChar" + id).fold[Validated[Throwable, CBCId]](
        Invalid(new Exception(s"Generated CBCId did not validate: $id"))
      )(cbcId => Valid(cbcId))
    }

}
