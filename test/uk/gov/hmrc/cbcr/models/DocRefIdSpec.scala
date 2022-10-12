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
import uk.gov.hmrc.cbcr.util.UnitSpec

class DocRefIdSpec extends UnitSpec {
  val validDocRefId = "GB2017RGXHCBC0100001024CBC40120170311T150000X1_1000000019OECD1ENT57"
  val correctionDocRefId = "GB2017RGXHCBC0100001024CBC40220170311T150000X1_1000000019OECD2ENT57"
  val deletionDocRefId = "GB2017RGXHCBC0100001024CBC40220170311T150000X1_1000000019OECD3ENT57"
  val unknownDocRefId = "GB2017RGXHCBC0100001024CBC40120170311T150000X1_1000000019DFGENT57"

  "DocRefIdRecord" should {

    "extract doc type indicator from valid doc ref id" in {
      DocRefIdRecord.extractDocTypeIndicator(validDocRefId) shouldBe Some("OECD1")
    }

    "make the doc ref id invalid if doc type indicator is OECD3" in {
      DocRefIdRecord.docRefIdValidity(deletionDocRefId) shouldBe false
    }

    "make the doc ref id valid if doc type indicator is different than OECD3" in {
      DocRefIdRecord.docRefIdValidity(validDocRefId) shouldBe true
      DocRefIdRecord.docRefIdValidity(correctionDocRefId) shouldBe true
    }

    "Make doc ref id flag valid if we cannot determine what doc type indicator has" in {
      DocRefIdRecord.docRefIdValidity(unknownDocRefId) shouldBe true
    }
  }

}
