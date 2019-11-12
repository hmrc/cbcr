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

package uk.gov.hmrc.cbcr.actors

import org.scalatest.concurrent.Eventually
import org.scalatest.{Matchers, WordSpecLike}
import uk.gov.hmrc.cbcr.models.CBCId
import uk.gov.hmrc.cbcr.services.CBCIdGenerator

class CBCIdGeneratorSpec extends WordSpecLike with Matchers with Eventually {

  val generator = new CBCIdGenerator

  "The CBCIdGenerator actor" should {

    "return a new CBCId when requested" in {
      val result = generator.generateCbcId()
      result.isValid shouldBe true
    }

    "return a new CBCId on each request" in {
      val id1 = generator.generateCbcId().toOption

      val id2 = generator.generateCbcId().toOption
      id1.map(_.value) == id2.map(_.value) shouldBe false
    }

    "recover correctly when restarted" in {
      //create new generator
      val newGenerator = new CBCIdGenerator

      val response = newGenerator.generateCbcId()
      response.toOption.map(_.value) shouldEqual CBCId("XTCBC0100000001").map(_.value)

    }
  }

}
