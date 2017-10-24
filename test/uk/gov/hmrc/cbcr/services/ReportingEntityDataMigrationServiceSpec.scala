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

package uk.gov.hmrc.cbcr.services

import cats.data.NonEmptyList
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.Configuration
import uk.gov.hmrc.cbcr.controllers.MockAuth
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.repositories.ReportingEntityDataRepo
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class ReportingEntityDataMigrationServiceSpec  extends UnitSpec with MockitoSugar with MockAuth with OneAppPerSuite{


  val red1 = ReportingEntityDataOld(DocRefId("docref1"),None,DocRefId("docrefa1"),Utr("7000000002"),UltimateParentEntity("foo1"),CBC701)
  val red2 = ReportingEntityDataOld(DocRefId("docref2"),None,DocRefId("docrefa2"),Utr("9000000001"),UltimateParentEntity("foo2"),CBC701)
  val red3 = ReportingEntityDataOld(DocRefId("docref3"),None,DocRefId("docrefa3"),Utr("7000000002"),UltimateParentEntity("foo3"),CBC701)
  val red4 = ReportingEntityDataOld(DocRefId("docref4"),None,DocRefId("docrefa4"),Utr("2000000002"),UltimateParentEntity("foo4"),CBC701)
  val red5 = ReportingEntityDataOld(DocRefId("docref5"),None,DocRefId("docrefa5"),Utr("1000000008"),UltimateParentEntity("foo5"),CBC701)

  val config = app.injector.instanceOf[Configuration]

  "migrate all the ReportingEntityData that have been previously generated" when {

    "performReportingEntityMigration has been set to true " in {

      val store = mock[ReportingEntityDataRepo]
      when(store.getAll) thenReturn Future.successful(List(red1,red2,red3,red4,red5))
      when(store.update(any[ReportingEntityData])) thenReturn Future.successful(true)

      new ReportingEntityDataMigrationService(store,config ++ Configuration("CBCId.performReportingEntityMigration" -> true))

      verify(store, times(5)).update(any[ReportingEntityData])
    }
  }

  "update the ReportingEntityDataOld model to the new ReportingEntityData model" in {
    val store = mock[ReportingEntityDataRepo]
    when(store.getAll) thenReturn Future.successful(List(red1,red2,red3,red4,red5))
    when(store.update(any[ReportingEntityData])) thenReturn Future.successful(true)

    new ReportingEntityDataMigrationService(store,config ++ Configuration("CBCId.performReportingEntityMigration" -> true))

    verify(store, times(1)).update(ReportingEntityData(NonEmptyList(red1.cbcReportsDRI,Nil),red1.additionalInfoDRI,red1.reportingEntityDRI,red1.utr,red1.ultimateParentEntity,red1.reportingRole))
    verify(store, times(1)).update(ReportingEntityData(NonEmptyList(red2.cbcReportsDRI,Nil),red2.additionalInfoDRI,red2.reportingEntityDRI,red2.utr,red2.ultimateParentEntity,red2.reportingRole))
    verify(store, times(1)).update(ReportingEntityData(NonEmptyList(red3.cbcReportsDRI,Nil),red3.additionalInfoDRI,red3.reportingEntityDRI,red3.utr,red3.ultimateParentEntity,red3.reportingRole))
    verify(store, times(1)).update(ReportingEntityData(NonEmptyList(red4.cbcReportsDRI,Nil),red4.additionalInfoDRI,red4.reportingEntityDRI,red4.utr,red4.ultimateParentEntity,red3.reportingRole))
    verify(store, times(1)).update(ReportingEntityData(NonEmptyList(red5.cbcReportsDRI,Nil),red5.additionalInfoDRI,red5.reportingEntityDRI,red5.utr,red5.ultimateParentEntity,red5.reportingRole))
  }

}
