/*
 * Copyright 2018 HM Revenue & Customs
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

case class CountryCode(countryCode: String) {
  private val regex = """^[A-Z]{2}$"""
  def isValid: Boolean = countryCode.matches(regex)
}

object CountryCode {
  implicit val format = new Format[CountryCode] {
    override def reads(json: JsValue) = json.validate[String].map(CountryCode(_))

    override def writes(o: CountryCode) = JsString(o.countryCode)

  }
}

