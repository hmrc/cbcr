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

package uk.gov.hmrc.cbcr.repositories

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import cats.data.NonEmptyList
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.OneAppPerSuite
import play.api.Configuration
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.{DefaultWriteResult, WriteResult}
import uk.gov.hmrc.cbcr.controllers.MockAuth
import uk.gov.hmrc.cbcr.models.{CBC701, CorrDocRefId, DocRefId, ReportingEntityData, TIN, UltimateParentEntity}
import uk.gov.hmrc.cbcr.util.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class ReportingEntityDataRepoSpec extends UnitSpec with MockAuth with OneAppPerSuite {

  val config                          = app.injector.instanceOf[Configuration]
  implicit val ec                     = app.injector.instanceOf[ExecutionContext]
  implicit val hc                     = HeaderCarrier()
  val writeResult                     = DefaultWriteResult(true,1,Seq.empty,None,None,None)
  val notFoundWriteResult             = DefaultWriteResult(true,0,Seq.empty,None,None,None)
  lazy val reactiveMongoApi           = app.injector.instanceOf[ReactiveMongoApi]
  val docRefId                        = DocRefId("GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD1REP")
  val corrRefId                       = CorrDocRefId(new DocRefId("corrRefId-SaveTest"))
  val reportingEntityDataRepository   = new ReportingEntityDataRepo(reactiveMongoApi)

  val creationDate                    = LocalDate.now
  val reportingPeroid                 = LocalDate.parse("2019-10-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"))

  val reportingEntityData = ReportingEntityData(NonEmptyList(docRefId, Nil), List(docRefId), docRefId,
                                TIN("3590617086", "CGI"), UltimateParentEntity("ABCCorp"), CBC701, Some(creationDate),Some(reportingPeroid))

  "Calls to delete a DocRefId" should {
    "should delete that docRefId" in {

      val result: Future[WriteResult] = reportingEntityDataRepository.delete(docRefId)
      await(result.map(r => r.ok)) shouldBe true

    }
  }

  "Calls to removeAllDocs" should {
    "should delete all docs" in {

      val result: Future[WriteResult] = reportingEntityDataRepository.removeAllDocs()
      await(result.map(r => r.ok)) shouldBe true

    }
  }

  "Calls to save" should {
    "should save the reportingEntityData in trhe database" in {

      val result: Future[WriteResult] = reportingEntityDataRepository.save(reportingEntityData)
      await(result.map(r => r.ok)) shouldBe true

    }
  }

  "Calls to confirmCreationDate" should {
    "should confirm the creationDate for the docRefId" in {

      val result: Future[Int] = reportingEntityDataRepository.confirmCreationDate(docRefId, creationDate)
      await(result) shouldBe 1

    }
  }

  "Calls to query" should {
    "should return the ReportingEntityData object for a given docRefId" in {

      val result: Future[Option[ReportingEntityData]] = reportingEntityDataRepository.query(docRefId)
      await(result.map(x => x.get.reportingEntityDRI)) shouldBe 1

    }
  }





}
