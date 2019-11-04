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

import org.scalatest.prop.GeneratorDrivenPropertyChecks
import play.api.libs.json.Json
import uk.gov.hmrc.cbcr.util.UnitSpec
import org.scalatest.Matchers.defined

import scala.concurrent.duration._
import org.scalatest.Assertions._
import org.scalatest.Suite
import org.scalatest.FunSuite

import scala.concurrent.Await


class ReportingEntityDataSpec extends UnitSpec with Suite{

  val additionalInfoList =
    s"""
       |{
       |  "cbcReportsDRI": ["GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD1REP"],
       |  "additionalInfoDRI": ["GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD1REP"],
       |  "reportingEntityDRI": "GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD1REP",
       |  "tin": "3590617086",
       |  "ultimateParentEntity": "ABCCorp",
       |  "reportingRole": "CBC701",
       |  "creationDate": "2019-01-21",
       |  "reportingPeriod": "2019-01-21"
       |}
     """.stripMargin

  val additionalInfoDocRefId =
    s"""
       |{
       |  "cbcReportsDRI": ["GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD1REP"],
       |  "additionalInfoDRI": "GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD1REP",
       |  "reportingEntityDRI": "GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD1REP",
       |  "tin": "3590617086",
       |  "ultimateParentEntity": "ABCCorp",
       |  "reportingRole": "CBC701",
       |  "creationDate": "2019-01-21",
       |  "reportingPeriod": "2019-01-21"
       |}
     """.stripMargin

  val noAdditionalInfo =
    s"""
       |{
       |  "cbcReportsDRI": ["GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD1REP"],
       |  "reportingEntityDRI": "GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD1REP",
       |  "tin": "3590617086",
       |  "ultimateParentEntity": "ABCCorp",
       |  "reportingRole": "CBC701",
       |  "creationDate": "2019-01-21",
       |  "reportingPeriod": "2019-01-21"
       |}
     """.stripMargin

  val additionalInfoInvalid =
    s"""
       |{
       |  "cbcReportsDRI": ["GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD1REP"],
       |  "reportingEntityDRI": "GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD1REP",
       |  "tin": "3590617086",
       |  "ultimateParentEntity": "ABCCorp",
       |  "creationDate": "2019-01-21",
       |  "reportingPeriod": "2019-01-21"
       |}
     """.stripMargin

  "ReportingEntityData" should {
    "allow AdditionalInfo to contain a list of docRefIds" in{
      Json.parse(additionalInfoList).as[ReportingEntityData] shouldBe a [ReportingEntityData]
    }

    "allow AdditionalInfo to contain a docRefId" in {
      Json.parse(additionalInfoDocRefId).as[ReportingEntityData] shouldBe a [ReportingEntityData]
    }

    "allow AdditionalInfo to not exist" in {
      Json.parse(noAdditionalInfo).as[ReportingEntityData] shouldBe a [ReportingEntityData]
    }

    "fail if AdditionalInfo is invalid" in {
      val caught = intercept[Exception](Json.parse(additionalInfoInvalid).as[ReportingEntityData])
      assert(caught.getMessage contains("JsResultException"))
    }
  }

  "ReportingEntityDataModel" should {
     "create oldModel and set value to true" in {
      val oldModel = Json.parse(additionalInfoDocRefId).as[ReportingEntityDataModel]
       oldModel shouldBe a [ReportingEntityDataModel]
       oldModel.oldModel should equal(true)
    }

    "create oldModel and set value to false" in {
      val newModel = Json.parse(additionalInfoList).as[ReportingEntityDataModel]
      newModel shouldBe a [ReportingEntityDataModel]
      newModel.oldModel should equal(false)

    }

    "just create remaining role values" in {
      ReportingRole.parseFromString("CBC702")
      ReportingRole.parseFromString("CBC703")
    }
  }

}
