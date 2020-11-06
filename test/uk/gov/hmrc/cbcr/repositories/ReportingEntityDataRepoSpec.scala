/*
 * Copyright 2020 HM Revenue & Customs
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
import org.scalatestplus.play.OneAppPerSuite
import play.api.Configuration
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.{DefaultWriteResult, WriteResult}
import uk.gov.hmrc.cbcr.controllers.MockAuth
import uk.gov.hmrc.cbcr.models
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.services.AdminReportingEntityData
import uk.gov.hmrc.cbcr.util.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class ReportingEntityDataRepoSpec extends UnitSpec with MockAuth with OneAppPerSuite {

  val config = app.injector.instanceOf[Configuration]
  implicit val ec = app.injector.instanceOf[ExecutionContext]
  implicit val hc = HeaderCarrier()
  val writeResult = DefaultWriteResult(true, 1, Seq.empty, None, None, None)
  val notFoundWriteResult = DefaultWriteResult(true, 0, Seq.empty, None, None, None)
  lazy val reactiveMongoApi = app.injector.instanceOf[ReactiveMongoApi]
  val docRefId = DocRefId("GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD1REP")
  val cbcId = CBCId.apply("XVCBC0000000056")
  val corrRefId = CorrDocRefId(new DocRefId("corrRefId-SaveTest"))
  val reportingEntityDataRepository = new ReportingEntityDataRepo(reactiveMongoApi)

  val creationDate = LocalDate.now
  val updateForcreationDate = (LocalDate.now).plusDays(5)
  val reportingPeroid = LocalDate.parse("2019-10-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"))

  val reportingEntityData = ReportingEntityData(
    NonEmptyList(docRefId, Nil),
    List(docRefId),
    docRefId,
    TIN("3590617086", "CGI"),
    UltimateParentEntity("ABCCorp"),
    CBC701,
    Some(creationDate),
    Some(reportingPeroid),
    None,
    None
  )

  val adminReportingEntityData = AdminReportingEntityData(List(docRefId), Some(List(docRefId)), docRefId)

  "Calls to delete a DocRefId" should {
    "should delete that docRefId" in {

      val result: Future[WriteResult] = reportingEntityDataRepository.delete(docRefId)
      await(result.map(r => r.ok)) shouldBe true

    }
  }

  "Calls to save" should {
    "should save the reportingEntityData in the database" in {

      val result: Future[WriteResult] = reportingEntityDataRepository.save(reportingEntityData)
      await(result.map(r => r.ok)) shouldBe true

    }
  }

  "Calls to update" should {
    "should update the reportingEntityData in the database" in {

      val result: Future[Boolean] = reportingEntityDataRepository.update(reportingEntityData)
      await(result) shouldBe true

    }
  }

  "Calls to getLatestReportingEntityData" should {
    "should return the ReportingEntityDataList sorted in ascending order" in {

      val result: List[ReportingEntityData] =
        reportingEntityDataRepository.getLatestReportingEntityData(List(reportingEntityData))
      result.size shouldBe 1

    }
  }

  "Calls to confirmCreationDate" should {
    "should confirm the creationDate for the docRefId" in {

      val result: Future[Int] = reportingEntityDataRepository.confirmCreationDate(docRefId, creationDate)
      await(result) shouldBe 1

    }
  }

  "Calls to updateCreationDate" should {
    "should update the creationDate for the docRefId" in {

      val result: Future[Int] = reportingEntityDataRepository.updateCreationDate(docRefId, updateForcreationDate)
      await(result) shouldBe 1

    }
  }

  "Calls to updateReportingEntityDRI" should {
    "should update the reportingEntityDRI for the docRefId" in {

      val result: Future[Boolean] =
        reportingEntityDataRepository.updateReportingEntityDRI(adminReportingEntityData, docRefId)
      await(result) shouldBe true

    }
  }

  "Calls to query" should {
    "with additional reportingPeriod param should return the List of ReportingEntityData object for a given docRefId" in {

      val result: Future[Option[ReportingEntityData]] = reportingEntityDataRepository.query(docRefId.id, "2019-10-01")
      await(result.map(x => x.get.reportingEntityDRI)) shouldBe docRefId

    }
  }

  "Calls to queryReportingEntity" should {
    "should return the ReportingEntityData object for a given docRefId" in {

      val result: Future[Option[ReportingEntityData]] = reportingEntityDataRepository.queryReportingEntity(docRefId)
      await(result.map(x => x.get.reportingEntityDRI)) shouldBe docRefId

    }
  }

  "Calls to updateAdditionalInfoDRI" should {
    "should update AdditionalInfoDRI docRefId for a given docRefId" in {

      val result: Future[Int] = reportingEntityDataRepository.updateAdditionalInfoDRI(docRefId)
      await(result) shouldBe 1

    }
  }

  "Calls to queryCbcId" should {
    "should return ReportingEntityData if it exists" in {

      val result: Future[Option[ReportingEntityData]] =
        reportingEntityDataRepository.queryCbcId(cbcId.get, reportingPeroid)
      await(result.map(x => x.get.ultimateParentEntity)) shouldBe UltimateParentEntity("ABCCorp")

    }
  }

  "Calls to queryTIN" should {
    "should return ReportingEntityData if it exists by criteria" in {

      val result: Future[List[ReportingEntityData]] = reportingEntityDataRepository.queryTIN("3590617086", "2019-10-01")
      await(result.map(x => x.apply(0).ultimateParentEntity)) shouldBe UltimateParentEntity("ABCCorp")

    }
  }

  "Calls to deleteReportingPeriod" should {
    "should delete the reporting period for a given docRefId" in {

      val result: Future[Int] = reportingEntityDataRepository.deleteReportingPeriod(docRefId)
      await(result) shouldBe 1

    }
  }

  "Calls to deleteCreationDate" should {
    "should delete the reporting period for a given docRefId" in {

      val result: Future[Int] = reportingEntityDataRepository.deleteCreationDate(docRefId)
      await(result) shouldBe 1

    }
  }

  "Calls to delete a DocRefId as cleanup process" should {
    "should delete that docRefId" in {

      val result: Future[WriteResult] = reportingEntityDataRepository.delete(docRefId)
      await(result.map(r => r.ok)) shouldBe true

    }
  }

  val docRefId1 = DocRefId("GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD1REP1")
  val docRefId2 = DocRefId("GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD1REP2")
  val addDocRefId1 = DocRefId("GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD1ADD1")
  val addDocRefId2 = DocRefId("GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD1ADD2")

  val corr1 = CorrDocRefId(docRefId1)
  val corr2 = CorrDocRefId(docRefId2)
  val corr3 = CorrDocRefId(addDocRefId1)
  val corr4 = CorrDocRefId(addDocRefId2)

  val newDocRef1 = DocRefId("GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD2REP1")
  val newDocRef2 = DocRefId("GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD2REP2")
  val newAddInfo3 = DocRefId("GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD2ADD1")
  val newAddInfo4 = DocRefId("GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD2ADD2")

  val corrPair1 = DocRefIdPair(newDocRef1, Some(corr1))
  val corrPair2 = DocRefIdPair(newDocRef2, Some(corr2))
  val corrPair3 = DocRefIdPair(newAddInfo3, Some(corr3))
  val corrPair4 = DocRefIdPair(newAddInfo4, Some(corr4))

  def rData(reps: NonEmptyList[DocRefId], add: List[DocRefId]) = ReportingEntityDataModel(
    reps,
    add,
    docRefId,
    TIN("3590617086", "CGI"),
    UltimateParentEntity("ABCCorp"),
    CBC701,
    Some(creationDate),
    Some(reportingPeroid),
    false,
    None,
    None
  )

  def partialData(reps: List[DocRefIdPair], add: List[DocRefIdPair]) = PartialReportingEntityData(
    reps,
    add,
    DocRefIdPair(docRefId1, Some(corr1)),
    TIN("3590617086", "CGI"),
    UltimateParentEntity("ABCCorp"),
    CBC701,
    Some(creationDate),
    Some(reportingPeroid),
    None,
    None
  )
  "Updates to CBCReports and AdditionalInfo" should {

    "partial corrections should work as expected when correcting just one report and one add info" in {
      val res1 = reportingEntityDataRepository.mergeListsReports(
        partialData(List(corrPair1), List(corrPair3)),
        rData(NonEmptyList[DocRefId](docRefId1, List(docRefId2)), List(addDocRefId1, addDocRefId2)))
      val res2 = reportingEntityDataRepository.mergeListsAddInfo(
        partialData(List(corrPair1), List(corrPair3)),
        rData(NonEmptyList[DocRefId](docRefId1, List(docRefId2)), List(addDocRefId1, addDocRefId2)))

      res1.equals(List(newDocRef1.id, docRefId2.id)) shouldBe true
      res2.equals(List(newAddInfo3.id, addDocRefId2.id)) shouldBe true
    }

    "correct only the reporting entity without affecting cbc reports and additional info" in {
      val res1 = reportingEntityDataRepository.mergeListsReports(
        partialData(List(), List()),
        rData(NonEmptyList[DocRefId](docRefId1, List(docRefId2)), List(addDocRefId1, addDocRefId2)))
      val res2 = reportingEntityDataRepository.mergeListsAddInfo(
        partialData(List(), List()),
        rData(NonEmptyList[DocRefId](docRefId1, List(docRefId2)), List(addDocRefId1, addDocRefId2)))
      res1.equals(List(docRefId1.id, docRefId2.id)) shouldBe true
      res2.equals(List(addDocRefId1.id, addDocRefId2.id)) shouldBe true
    }

    "correct only either one of the additional info or one of the cbc reports" in {
      val res1 = reportingEntityDataRepository.mergeListsReports(
        partialData(List(corrPair2), List()),
        rData(NonEmptyList[DocRefId](docRefId1, List(docRefId2)), List(addDocRefId1, addDocRefId2)))
      val res2 = reportingEntityDataRepository.mergeListsAddInfo(
        partialData(List(), List(corrPair4)),
        rData(NonEmptyList[DocRefId](docRefId1, List(docRefId2)), List(addDocRefId1, addDocRefId2)))

      res1.equals(List(newDocRef2.id, docRefId1.id)) shouldBe true
      res2.equals(List(newAddInfo4.id, addDocRefId1.id)) shouldBe true
    }

    "full corrections for both reports and add info" in {
      val res1 = reportingEntityDataRepository.mergeListsReports(
        partialData(List(corrPair1, corrPair2), List(corrPair3, corrPair4)),
        rData(NonEmptyList[DocRefId](docRefId1, List(docRefId2)), List(addDocRefId1, addDocRefId2))
      )
      val res2 = reportingEntityDataRepository.mergeListsAddInfo(
        partialData(List(corrPair1, corrPair2), List(corrPair3, corrPair4)),
        rData(NonEmptyList[DocRefId](docRefId1, List(docRefId2)), List(addDocRefId1, addDocRefId2))
      )

      res1.equals(List(newDocRef1.id, newDocRef2.id)) shouldBe true
      res2.equals(List(newAddInfo3.id, newAddInfo4.id)) shouldBe true
    }
  }

}
