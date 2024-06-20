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

package uk.gov.hmrc.cbcr.repositories

import cats.data.NonEmptyList
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.cbcr.controllers.MockAuth
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.util.UnitSpec

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.Future

class ReportingEntityDataRepoSpec extends UnitSpec with MockAuth with GuiceOneAppPerSuite {

  private val docRefId = DocRefId("GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD1REP")
  private val docRefIdForDelete = DocRefId("GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD1REP10")
  private val cbcId = CBCId.apply("XVCBC0000000056")
  private val reportingEntityDataRepository = app.injector.instanceOf[ReportingEntityDataRepo]

  private val creationDate = LocalDate.now
  private val updateForcreationDate = LocalDate.now.plusDays(5)
  private val reportingPeriod = LocalDate.parse("2019-10-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"))

  private val entityReportingperiod = EntityReportingPeriod(
    LocalDate.parse("2016-01-01", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
    LocalDate.parse("2016-03-31", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
  )

  private val reportingEntityData = ReportingEntityData(
    NonEmptyList(docRefId, Nil),
    List(docRefId),
    docRefId,
    TIN("3590617086", "CGI"),
    UltimateParentEntity("ABCCorp"),
    CBC701,
    Some(creationDate),
    Some(reportingPeriod),
    None,
    None
  )

  private val reportingEntityData1 = ReportingEntityData(
    NonEmptyList(docRefIdForDelete, Nil),
    List(docRefIdForDelete),
    docRefIdForDelete,
    TIN("3590617086", "CGI"),
    UltimateParentEntity("ABCCorp"),
    CBC701,
    Some(creationDate),
    Some(reportingPeriod),
    None,
    Some(entityReportingperiod)
  )

  private def red(dri: DocRefId, reportingPeriod: LocalDate, erp: Option[EntityReportingPeriod]) =
    ReportingEntityData(
      NonEmptyList(docRefId, Nil),
      List(docRefId),
      dri,
      TIN("3590617086", "CGI"),
      UltimateParentEntity("ABCCorp"),
      CBC701,
      Some(creationDate),
      Some(reportingPeriod),
      None,
      erp
    )

  private val doc1 = DocRefId("GB2017RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD1ENT1")
  private val doc2 = DocRefId("GB2017RGXVCBC0000000056CBC40120170311T100000X_7000000002OECD3ENT2")
  private val doc3 = DocRefId("GB2017RGXVCBC0000000056CBC40120170312T090000X_7000000002OECD1ENT3")
  private val doc4 = DocRefId("GB2018RGXVCBC0000000056CBC40120180312T090000X_7000000002OECD1ENT4")
  private val doc5 = DocRefId("GB2019RGXVCBC0000000056CBC40120190312T090000X_7000000002OECD1ENT5")
  private val doc6 = DocRefId("GB2019RGXVCBC0000000056CBC40120190312T090000X_7000000002OECD1ENT6")
  private val red1 = red(doc1, LocalDate.parse("2017-11-11"), None)
  private val red2 = red(doc2, LocalDate.parse("2017-11-11"), None)
  private val red3 = red(doc3, LocalDate.parse("2017-12-01"), None)
  private val red4 = red(
    doc4,
    LocalDate.parse("2018-12-01"),
    Some(
      EntityReportingPeriod(LocalDate.parse("2018-01-01"), LocalDate.parse("2018-12-01"))
    )
  )
  private val red5 = red(
    doc5,
    LocalDate.parse("2019-12-01"),
    Some(
      EntityReportingPeriod(LocalDate.parse("2019-01-01"), LocalDate.parse("2019-12-01"))
    )
  )

  "Calls to delete a DocRefId" should {
    "should delete that docRefId" in {

      val result = reportingEntityDataRepository.delete(docRefId)
      await(result).wasAcknowledged() shouldBe true

    }
  }

  "Calls to save" should {
    "should save the reportingEntityData in the database" in {

      val result = reportingEntityDataRepository.save(reportingEntityData)
      await(result).wasAcknowledged() shouldBe true

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

      val result = reportingEntityDataRepository.getLatestReportingEntityData(List(reportingEntityData))
      result.size shouldBe 1

    }
  }

  "Calls to confirmCreationDate" should {
    "should confirm the creationDate for the docRefId" in {

      val result = reportingEntityDataRepository.confirmCreationDate(docRefId, creationDate)
      await(result) shouldBe 1L

    }
  }

  "Calls to updateCreationDate" should {
    "should update the creationDate for the docRefId" in {

      val result = reportingEntityDataRepository.updateCreationDate(docRefId, updateForcreationDate)
      await(result) shouldBe 1L

    }
  }

  "Calls to query" should {
    "with additional reportingPeriod param should return the List of ReportingEntityData object for a given docRefId" in {

      val result: Future[Option[ReportingEntityData]] =
        reportingEntityDataRepository.query(docRefId.id, LocalDate.parse("2019-10-01"))
      await(result).get.reportingEntityDRI shouldBe docRefId

    }
  }

  "Calls to queryReportingEntity" should {
    "should return the ReportingEntityData object for a given docRefId" in {

      val result: Future[Option[ReportingEntityData]] = reportingEntityDataRepository.queryReportingEntity(docRefId)
      await(result).get.reportingEntityDRI shouldBe docRefId

    }
  }

  "Calls to updateAdditionalInfoDRI" should {
    "should update AdditionalInfoDRI docRefId for a given docRefId" in {

      val result = reportingEntityDataRepository.updateAdditionalInfoDRI(docRefId)
      await(result) shouldBe 1L

    }
  }

  "Calls to queryCbcId" should {
    "should return ReportingEntityData if it exists" in {

      val result: Future[Option[ReportingEntityData]] =
        reportingEntityDataRepository.queryCbcId(cbcId.get, reportingPeriod)
      await(result).get.ultimateParentEntity shouldBe UltimateParentEntity("ABCCorp")

    }
  }

  "Calls to queryTIN" should {
    "should return ReportingEntityData if it exists by criteria" in {

      val result: Future[Seq[ReportingEntityData]] =
        reportingEntityDataRepository.queryTIN("3590617086", LocalDate.parse("2019-10-01"))
      await(result).head.ultimateParentEntity shouldBe UltimateParentEntity("ABCCorp")

    }
  }

  "Calls to deleteReportingPeriod" should {
    "should delete the reporting period for a given docRefId" in {

      val result = reportingEntityDataRepository.deleteReportingPeriod(docRefId)
      await(result) shouldBe 1L

    }
  }

  "Calls to save a RepEntity" should {
    "should save the reportingEntityData in the database for deleting purposes" in {

      val result = reportingEntityDataRepository.save(reportingEntityData1)
      await(result).wasAcknowledged() shouldBe true

    }
  }

  "Calls to deleteReportingPeriod by RepEntDocRefId" should {
    "should delete the reporting period for a given docRefId" in {

      val result = reportingEntityDataRepository.deleteReportingPeriodByRepEntDocRefId(docRefIdForDelete)
      await(result) shouldBe 1L

    }
  }

  "Calls to deleteCreationDate" should {
    "should delete the reporting period for a given docRefId" in {

      val result = reportingEntityDataRepository.deleteCreationDate(docRefId)
      await(result) shouldBe 1L

    }
  }

  "Calls to delete a DocRefId as cleanup process" should {
    "should delete that docRefId" in {

      val result = reportingEntityDataRepository.delete(docRefId)
      await(result).wasAcknowledged() shouldBe true

      val result1 = reportingEntityDataRepository.delete(docRefIdForDelete)
      await(result1).wasAcknowledged() shouldBe true

    }
  }

  private val docRefId1 = DocRefId("GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD1REP1")
  private val docRefId2 = DocRefId("GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD1REP2")
  private val addDocRefId1 = DocRefId("GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD1ADD1")
  private val addDocRefId2 = DocRefId("GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD1ADD2")

  private val corr1 = CorrDocRefId(docRefId1)
  private val corr2 = CorrDocRefId(docRefId2)
  private val corr3 = CorrDocRefId(addDocRefId1)
  private val corr4 = CorrDocRefId(addDocRefId2)

  private val newDocRef1 = DocRefId("GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD2REP1")
  private val newDocRef2 = DocRefId("GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD2REP2")
  private val newAddInfo3 = DocRefId("GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD2ADD1")
  private val newAddInfo4 = DocRefId("GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD2ADD2")

  private val corrPair1 = DocRefIdPair(newDocRef1, Some(corr1))
  private val corrPair2 = DocRefIdPair(newDocRef2, Some(corr2))
  private val corrPair3 = DocRefIdPair(newAddInfo3, Some(corr3))
  private val corrPair4 = DocRefIdPair(newAddInfo4, Some(corr4))

  private def rData(reps: NonEmptyList[DocRefId], add: List[DocRefId]) = ReportingEntityDataModel(
    reps,
    Right(add),
    docRefId,
    TIN("3590617086", "CGI"),
    UltimateParentEntity("ABCCorp"),
    CBC701,
    Some(creationDate),
    Some(reportingPeriod),
    None,
    None
  )

  private def partialData(reps: List[DocRefIdPair], add: List[DocRefIdPair]) = PartialReportingEntityData(
    reps,
    add,
    DocRefIdPair(docRefId1, Some(corr1)),
    TIN("3590617086", "CGI"),
    UltimateParentEntity("ABCCorp"),
    CBC701,
    Some(creationDate),
    Some(reportingPeriod),
    None,
    None
  )
  "Updates to CBCReports and AdditionalInfo" should {

    "partial corrections should work as expected when correcting just one report and one add info" in {
      val res1 = reportingEntityDataRepository.mergeListsReports(
        partialData(List(corrPair1), List(corrPair3)),
        rData(NonEmptyList[DocRefId](docRefId1, List(docRefId2)), List(addDocRefId1, addDocRefId2))
      )
      val res2 = reportingEntityDataRepository
        .mergeListsAddInfo(partialData(List(corrPair1), List(corrPair3)), List(addDocRefId1, addDocRefId2))

      res1.equals(List(newDocRef1.id, docRefId2.id)) shouldBe true
      res2.equals(List(newAddInfo3.id, addDocRefId2.id)) shouldBe true
    }

    "correct only the reporting entity without affecting cbc reports and additional info" in {
      val res1 = reportingEntityDataRepository.mergeListsReports(
        partialData(List(), List()),
        rData(NonEmptyList[DocRefId](docRefId1, List(docRefId2)), List(addDocRefId1, addDocRefId2))
      )
      val res2 =
        reportingEntityDataRepository.mergeListsAddInfo(partialData(List(), List()), List(addDocRefId1, addDocRefId2))
      res1.equals(List(docRefId1.id, docRefId2.id)) shouldBe true
      res2.equals(List(addDocRefId1.id, addDocRefId2.id)) shouldBe true
    }

    "correct only either one of the additional info or one of the cbc reports" in {
      val res1 = reportingEntityDataRepository.mergeListsReports(
        partialData(List(corrPair2), List()),
        rData(NonEmptyList[DocRefId](docRefId1, List(docRefId2)), List(addDocRefId1, addDocRefId2))
      )
      val res2 = reportingEntityDataRepository
        .mergeListsAddInfo(partialData(List(), List(corrPair4)), List(addDocRefId1, addDocRefId2))

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
        List(addDocRefId1, addDocRefId2)
      )

      res1.equals(List(newDocRef1.id, newDocRef2.id)) shouldBe true
      res2.equals(List(newAddInfo3.id, newAddInfo4.id)) shouldBe true
    }

    "Filter out deletion should return false when doc ref id contains OECD3 and true otherwise" in {
      reportingEntityDataRepository.filterOutDeletion(reportingEntityData) shouldBe true
      val deletionDocRefId = DocRefId("GB2016RGXVCBC0000000056CBC40120170311T090000X_7000000002OECD3REP")
      val deletedData = reportingEntityData.copy(reportingEntityDRI = deletionDocRefId)
      reportingEntityDataRepository.filterOutDeletion(deletedData) shouldBe false
    }

    "Check by single date should return true if the dates are overlapping or false otherwise" in {
      val res1 = reportingEntityDataRepository.checkBySingleDate(
        EntityReportingPeriod(LocalDate.parse("2017-12-01"), LocalDate.parse("2018-11-30")),
        LocalDate.parse("2018-11-11")
      )
      res1 shouldBe true

      val res2 = reportingEntityDataRepository.checkBySingleDate(
        EntityReportingPeriod(LocalDate.parse("2017-12-01"), LocalDate.parse("2018-11-30")),
        LocalDate.parse("2017-11-11")
      )
      res2 shouldBe false

      val res3 = reportingEntityDataRepository.checkBySingleDate(
        EntityReportingPeriod(LocalDate.parse("2017-12-01"), LocalDate.parse("2018-11-30")),
        LocalDate.parse("2017-12-01")
      )
      res3 shouldBe true

      val res4 = reportingEntityDataRepository.checkBySingleDate(
        EntityReportingPeriod(LocalDate.parse("2017-12-01"), LocalDate.parse("2018-11-30")),
        LocalDate.parse("2018-11-30")
      )
      res4 shouldBe false
    }

    "Check both dates for overlapping" in {
      val res1 = reportingEntityDataRepository.checkBothDates(
        EntityReportingPeriod(LocalDate.parse("2017-12-02"), LocalDate.parse("2018-11-29")),
        EntityReportingPeriod(LocalDate.parse("2017-12-01"), LocalDate.parse("2018-11-30"))
      )
      res1 shouldBe true

      val res2 = reportingEntityDataRepository.checkBothDates(
        EntityReportingPeriod(LocalDate.parse("2017-12-01"), LocalDate.parse("2018-11-30")),
        EntityReportingPeriod(LocalDate.parse("2017-12-02"), LocalDate.parse("2018-11-29"))
      )
      res2 shouldBe true

      val res3 = reportingEntityDataRepository.checkBothDates(
        EntityReportingPeriod(LocalDate.parse("2017-12-01"), LocalDate.parse("2018-11-30")),
        EntityReportingPeriod(LocalDate.parse("2017-12-01"), LocalDate.parse("2018-05-29"))
      )
      res3 shouldBe true

      val res4 = reportingEntityDataRepository.checkBothDates(
        EntityReportingPeriod(LocalDate.parse("2017-12-01"), LocalDate.parse("2018-11-30")),
        EntityReportingPeriod(LocalDate.parse("2017-12-01"), LocalDate.parse("2018-11-30"))
      )
      res4 shouldBe true

      val res5 = reportingEntityDataRepository.checkBothDates(
        EntityReportingPeriod(LocalDate.parse("2017-12-01"), LocalDate.parse("2018-11-30")),
        EntityReportingPeriod(LocalDate.parse("2018-11-30"), LocalDate.parse("2019-11-29"))
      )
      res5 shouldBe true

      val res6 = reportingEntityDataRepository.checkBothDates(
        EntityReportingPeriod(LocalDate.parse("2018-12-01"), LocalDate.parse("2019-11-30")),
        EntityReportingPeriod(LocalDate.parse("2017-12-30"), LocalDate.parse("2018-12-01"))
      )
      res6 shouldBe true

      val res7 = reportingEntityDataRepository.checkBothDates(
        EntityReportingPeriod(LocalDate.parse("2017-12-01"), LocalDate.parse("2018-11-29")),
        EntityReportingPeriod(LocalDate.parse("2018-11-30"), LocalDate.parse("2019-11-29"))
      )
      res7 shouldBe false

      val res8 = reportingEntityDataRepository.checkBothDates(
        EntityReportingPeriod(LocalDate.parse("2018-11-30"), LocalDate.parse("2019-11-29")),
        EntityReportingPeriod(LocalDate.parse("2017-12-01"), LocalDate.parse("2018-11-29"))
      )
      res8 shouldBe false
    }

    "Check overlapping to make sure deleted records are ignored" in {
      val result1 = reportingEntityDataRepository.save(red1)
      await(result1).wasAcknowledged() shouldBe true
      val result2 = reportingEntityDataRepository.save(red2)
      await(result2).wasAcknowledged() shouldBe true
      val res = reportingEntityDataRepository
        .queryTINDatesOverlapping(
          "3590617086",
          EntityReportingPeriod(LocalDate.parse("2017-01-30"), LocalDate.parse("2017-11-29"))
        )
      val finalRes = await(res)
      finalRes shouldBe false
    }

    "Overlapping should work as expected if database records overlap with current submission" in {
      val result1 = reportingEntityDataRepository.save(red3)
      await(result1).wasAcknowledged() shouldBe true
      val res1 = reportingEntityDataRepository
        .queryTINDatesOverlapping(
          "3590617086",
          EntityReportingPeriod(LocalDate.parse("2017-01-30"), LocalDate.parse("2018-01-29"))
        )
      val finalRes1 = await(res1)
      finalRes1 shouldBe true

      val res2 = reportingEntityDataRepository
        .queryTINDatesOverlapping(
          "3590617086",
          EntityReportingPeriod(LocalDate.parse("2017-12-30"), LocalDate.parse("2018-01-29"))
        )
      val finalRes2 = await(res2)
      finalRes2 shouldBe false
    }

    "Overlapping should work as expected when database records overlap with current submission when having both dates" in {
      val result1 = reportingEntityDataRepository.save(red4)
      await(result1).wasAcknowledged() shouldBe true
      val result2 = reportingEntityDataRepository.save(red5)
      await(result2).wasAcknowledged() shouldBe true
      val res1 = reportingEntityDataRepository
        .queryTINDatesOverlapping(
          "3590617086",
          EntityReportingPeriod(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-11-01"))
        )
      val finalRes1 = await(res1)
      finalRes1 shouldBe false

      val res2 = reportingEntityDataRepository
        .queryTINDatesOverlapping(
          "3590617086",
          EntityReportingPeriod(LocalDate.parse("2017-12-30"), LocalDate.parse("2018-01-29"))
        )
      val finalRes2 = await(res2)
      finalRes2 shouldBe true

      val finalRes3 = await(res2)
      finalRes3 shouldBe true
    }
  }

  "Calls to updateReportingEntityPeriod" should {
    "should update the reportingEntityPeriod for the docRefId" in {

      val result: Future[Boolean] =
        reportingEntityDataRepository.updateEntityReportingPeriod(
          doc5,
          EntityReportingPeriod(LocalDate.parse("2020-01-30"), LocalDate.parse("2020-12-30"))
        )
      await(result) shouldBe true

      val updatedResult: Future[Option[ReportingEntityData]] =
        reportingEntityDataRepository.query(doc5.id, LocalDate.parse("2019-12-01"))
      await(updatedResult).get.entityReportingPeriod shouldBe Some(
        EntityReportingPeriod(LocalDate.parse("2020-01-30"), LocalDate.parse("2020-12-30"))
      )

    }

    "update functionality should work as expected when updating only entity reporting period but no doc ref ids updates" in {
      val partialData = PartialReportingEntityData(
        List(),
        List(),
        DocRefIdPair(doc5, None),
        TIN("3590617086", "CGI"),
        UltimateParentEntity("ABCCorp"),
        CBC701,
        Some(creationDate),
        Some(reportingPeriod),
        None,
        Some(EntityReportingPeriod(LocalDate.parse("2020-02-15"), LocalDate.parse("2020-11-30")))
      )
      val updatedRes = reportingEntityDataRepository.update(partialData)
      await(updatedRes) shouldBe true
    }

    "update functionality should work as expected when updating at least one DocRefID" in {
      val partialData = PartialReportingEntityData(
        List(),
        List(),
        DocRefIdPair(doc6, Some(CorrDocRefId(doc5))),
        TIN("3590617086", "CGI"),
        UltimateParentEntity("ABCCorp"),
        CBC701,
        Some(creationDate),
        Some(reportingPeriod),
        None,
        Some(EntityReportingPeriod(LocalDate.parse("2020-02-15"), LocalDate.parse("2020-11-30")))
      )
      val updatedRes = reportingEntityDataRepository.update(partialData)
      await(updatedRes) shouldBe true
    }
  }

}
