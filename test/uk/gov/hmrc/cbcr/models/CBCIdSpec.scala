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

import org.scalacheck.Gen
import org.scalatest.WordSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks

class CBCIdSpec extends WordSpec with GeneratorDrivenPropertyChecks{

  val arbBigNum = Gen.posNum[Int].map(_ + 999999)

  "A CBCId" should {
    "provide a smart constructor that only allows the creation of CBCIds" when {
      "they start with an X" in {
        CBCId("ZGCBC0000000001") shouldBe None
      }
      "they are 15 chars long" in {
        CBCId("XHCBC000000001") shouldBe None
      }
      "they have a sequence number less than 1000000" in {
        CBCId("XMCBC0001000000") shouldBe None
      }
      "they have the correct check digit" in {
        val valid1 = "XGCBC0000000001"
        val valid2 = "XQCBC0000000761"
        val valid3 = "XSCBC0000004521"
        val valid4 = "XVCBC0000026102"
        val valid5 = "XTCBC0000612834"
        val fromEtmp = "XHCBC1000000037"

        CBCId(valid1) shouldBe defined
        CBCId(valid2) shouldBe defined
        CBCId(valid3) shouldBe defined
        CBCId(valid4) shouldBe defined
        CBCId(valid5) shouldBe defined
        CBCId(fromEtmp) shouldBe defined

        val inValid1 = "XGCBC0000000002"
        val inValid2 = "XQCBC0000000762"
        val inValid3 = "XSCBC0000004531"
        val inValid4 = "XVCBC0000026122"
        val inValid5 = "XTCBC0000612830"


        CBCId(inValid1) shouldBe None
        CBCId(inValid2) shouldBe None
        CBCId(inValid3) shouldBe None
        CBCId(inValid4) shouldBe None
        CBCId(inValid5) shouldBe None
      }
    }
    "allow the creation of valid CBCIds over the whole possible range" in {
      for (i <- 1 until 1000000) {
        CBCId.create(i).isValid shouldBe true
      }
    }
    "not allow creating CBCIds outside of the valid range of 1-999999" in {
      forAll(Gen.negNum[Int]){ n =>
        CBCId.create(n).isInvalid shouldBe true
      }
      forAll(Gen.posNum[Int].map(_ + 999999)){ n =>
        CBCId.create(n).isInvalid shouldBe true
      }
    }
  }
}
