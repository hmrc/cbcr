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

package uk.gov.hmrc.cbcr.services

import cats.data.NonEmptyList
import ch.qos.logback.classic.Level
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{Configuration, Logger}
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.repositories.ReportingEntityDataRepo
import uk.gov.hmrc.cbcr.util.{LogCapturing, UnitSpec}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.ExecutionContext.Implicits.global

class RetrieveReportingEntityServiceSpec
    extends LogCapturing
    with UnitSpec
    with GuiceOneAppPerSuite
    with ScalaFutures {

  private val reportingEntityDataRepo =
    app.injector.instanceOf[ReportingEntityDataRepo]
  private val configuration = app.injector.instanceOf[Configuration]
  private val runMode = new RunMode(configuration)
  private val audit = app.injector.instanceOf[AuditConnector]
  private val mockRunMode = mock[RunMode]
  val mockReportingEntityDataRepo = mock[ReportingEntityDataRepo]


  private val retrieveReportingEntityService =
    new RetrieveReportingEntityService(reportingEntityDataRepo,
                                       configuration,
                                       runMode,
                                       audit)
  private val retrieveReportingEntityServiceWithMockRunMode =
    new RetrieveReportingEntityService(reportingEntityDataRepo,
                                       configuration,
                                       mockRunMode,
                                       audit)

  private val tin = TIN("value", "issuedBy")
  private val emptyDocRefId = DocRefId("")
  private val ultimateParentEntity = UltimateParentEntity("")
  private val reportingEntityData =
    ReportingEntityData(NonEmptyList(emptyDocRefId, List.empty),
                        List.empty,
                        DocRefId(""),
                        tin,
                        ultimateParentEntity,
                       reportingRole = CBC701,
                        None,
                        None)

  private def isRetrieveReportingEntityTrue(
      rrEntityService: RetrieveReportingEntityService) =
    rrEntityService.retrieveReportingEntity

  "the val retrieveReportingEntity" should {
    "return true if it successfully matches the required values" in {
      isRetrieveReportingEntityTrue(retrieveReportingEntityService) shouldBe true
    }
    "return false if the configuration value does not match any of the required values" in {
      when(mockRunMode.env) thenReturn "wrongEnv"
      isRetrieveReportingEntityTrue(
        retrieveReportingEntityServiceWithMockRunMode) shouldBe false
    }

    "log info if the configuration value does not match any of the required values" in {
      withCaptureOfLoggingFrom(Logger) { logs =>
        new RetrieveReportingEntityService(reportingEntityDataRepo,
                                           configuration,
                                           mockRunMode,
                                           audit)
        when(mockRunMode.env) thenReturn "wrongEnv"
        logs.count(_.getLevel == Level.INFO) shouldBe 1
      }
    }
  }
}
