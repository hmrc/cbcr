/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.cbcr.services

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.libs.json.Json
import uk.gov.hmrc.cbcr.util.UnitSpec

class DataMigrationCriteriaSpec extends UnitSpec with GuiceOneAppPerSuite {

  "DataMigrationCriteria" should {
    import DataMigrationCriteria._

    "produce an jsObject when result cannot get the correct values from the configuration" in {
      val failCaseJson = Json.obj("cbcId" -> Json.obj("$regex" -> "X[A-Z]CBC00.*"))
      val configuration = Configuration()

      PRIVATE_BETA_CRITERIA(configuration) shouldBe failCaseJson
    }

    "produce a jsObject when result has correct values from the configuration" in {

      val key = "businessPartnerRecord.safeId"
      val config = Configuration("CBCId.safeId1" -> "id1", "CBCId.safeId2" -> "id2")
      val successCaseJson = Json.obj("$or"       -> Json.arr(Json.obj(key -> "id1"), Json.obj(key -> "id2")))

      PRIVATE_BETA_CRITERIA(config) shouldBe successCaseJson
    }
  }
}
