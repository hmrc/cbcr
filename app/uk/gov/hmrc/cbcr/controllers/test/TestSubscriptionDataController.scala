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

package uk.gov.hmrc.cbcr.controllers.test

import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.repositories.{DocRefIdRepository, MessageRefIdRepository, ReportingEntityDataRepo, SubscriptionDataRepository}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class TestSubscriptionDataController @Inject() (
  subRepo: SubscriptionDataRepository,
  docRefRepo: DocRefIdRepository,
  messageRefIdRepository: MessageRefIdRepository,
  reportingEntityDataRepo: ReportingEntityDataRepo,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def insertData(): Action[JsValue] = Action.async[JsValue](parse.json) { implicit request =>
    withJsonBody[SubscriptionDetails] {
      subRepo.save2(_).map(_ => Ok("data submitted successfully"))
    }
  }

  def deleteSubscription(utrs: String): Action[AnyContent] = Action.async {
    subRepo.clear(Utr(utrs)).map(_ => Ok)
  }

  def deleteSingleDocRefId(docRefIds: DocRefId): Action[AnyContent] = Action.async {
    docRefRepo.delete(docRefIds).map(_ => Ok)
  }

  def deleteSingleMessageRefId(messageRefIds: String): Action[AnyContent] = Action.async {
    messageRefIdRepository.delete(MessageRefId(messageRefIds)).map(_ => Ok)
  }

  def deleteReportingEntityData(docRefId: DocRefId): Action[AnyContent] = Action.async {
    reportingEntityDataRepo.delete(docRefId).map(wr => if (wr.getDeletedCount == 0) NotFound else Ok)
  }

  def dropReportingEntityDataCollection(): Action[AnyContent] = Action.async {
    reportingEntityDataRepo.removeAllDocs().map(_ => Ok("Successfully drop reporting entity data collection"))
  }

  def updateReportingEntityCreationDate(creationDate: String, docRefId: DocRefId): Action[AnyContent] = Action.async {
    val cd = LocalDate.parse(creationDate)
    reportingEntityDataRepo.updateCreationDate(docRefId, cd).map {
      case n if n > 0 => Ok
      case _          => NotModified
    }
  }

  def deleteReportingEntityCreationDate(docRefId: DocRefId): Action[AnyContent] = Action.async {
    reportingEntityDataRepo.deleteCreationDate(docRefId).map {
      case n if n > 0 => Ok
      case _          => NotModified
    }
  }

  def confirmReportingEntityCreationDate(creationDate: String, docRefId: DocRefId): Action[AnyContent] = Action.async {
    val cd = LocalDate.parse(creationDate)

    reportingEntityDataRepo.confirmCreationDate(docRefId, cd).map {
      case 1 => Ok
      case _ => NotFound
    }
  }

  def deleteReportingEntityReportingPeriod(docRefId: DocRefId): Action[AnyContent] = Action.async {
    reportingEntityDataRepo.deleteReportingPeriod(docRefId).map {
      case n if n > 0 => Ok
      case _          => NotModified
    }
  }

  def deleteReportingPeriodByRepEntDocRefId(docRefId: DocRefId): Action[AnyContent] = Action.async {
    reportingEntityDataRepo.deleteReportingPeriodByRepEntDocRefId(docRefId).map {
      case n if n > 0 => Ok
      case _          => NotModified
    }
  }

  def validateNumberOfCbcIdForUtr(utr: String): Action[AnyContent] = Action.async {
    subRepo.checkNumberOfCbcIdForUtr(utr).map {
      case n if n > 0 => Ok(Json.toJson(n))
      case _          => NotFound
    }
  }

  def updateReportingEntityAdditionalInfoDRI(docRefId: DocRefId): Action[AnyContent] = Action.async {
    reportingEntityDataRepo.updateAdditionalInfoDRI(docRefId).map {
      case n if n > 0 => Ok
      case _          => NotModified
    }
  }

  def dropSubscriptionDataCollection(): Action[AnyContent] = Action.async {
    subRepo.removeAll().map(_ => Ok("Successfully drop subscription data collection"))
  }

}
