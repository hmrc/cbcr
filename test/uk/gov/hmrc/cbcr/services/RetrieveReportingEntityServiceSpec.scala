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

package uk.gov.hmrc.cbcr.services

import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import uk.gov.hmrc.cbcr.repositories.ReportingEntityDataRepo
import uk.gov.hmrc.cbcr.util.{LogCapturing, UnitSpec}

import scala.concurrent.ExecutionContext.Implicits.global

class RetrieveReportingEntityServiceSpec extends LogCapturing with UnitSpec with GuiceOneAppPerSuite with ScalaFutures {

  private val reportingEntityDataRepo = app.injector.instanceOf[ReportingEntityDataRepo]
  private val configuration = app.injector.instanceOf[Configuration]

  private val retrieveReportingEntityService =
    new RetrieveReportingEntityService(reportingEntityDataRepo, configuration)

  "the val retrieveReportingEntity" should {
    "return true if it successfully matches the required values" in {
      retrieveReportingEntityService.retrieveReportingEntity shouldBe true
    }
  }
}
