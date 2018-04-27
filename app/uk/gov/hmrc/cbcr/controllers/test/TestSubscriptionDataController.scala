/*
 * Copyright 2018 HM Revenue & Customs
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

import java.time.LocalDate

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.repositories.{DocRefIdRepository, MessageRefIdRepository, ReportingEntityDataRepo, SubscriptionDataRepository}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

@Singleton
class TestSubscriptionDataController @Inject()(subRepo: SubscriptionDataRepository,
                                               docRefRepo: DocRefIdRepository,
                                               messageRefIdRepository: MessageRefIdRepository,
                                               reportingEntityDataRepo: ReportingEntityDataRepo
                                              )(implicit ec: ExecutionContext) extends BaseController with ServicesConfig {

  def insertData() = Action.async[JsValue](parse.json) {
    implicit request =>
      withJsonBody[SubscriptionDetails] {
        subRepo.save(_) map {
          _.ok match {
            case true => Ok("data submitted successfully")
            case false =>
              InternalServerError("error submitting data")
          }
        }
      }
  }

  def deleteSubscription(utrs: String): Action[AnyContent] = Action.async {
    implicit request => {
      val utr = Utr(utrs)
      subRepo.clear(utr).map {
        case w if w.ok => Ok
        case _         => InternalServerError
      }
    }
  }

  def deleteSingleDocRefId(docRefIds: String): Action[AnyContent] = Action.async {
    implicit request => {
      val docRefId = DocRefId(docRefIds)
      docRefRepo.delete(docRefId).map {
        case w if w.ok  => Ok
        case _          => InternalServerError
      }
    }
  }

  def deleteSingleMessageRefId(messageRefIds: String): Action[AnyContent] = Action.async {
    implicit request => {
      val messageRefId = MessageRefId(messageRefIds)
      messageRefIdRepository.delete(messageRefId).map {
        case w if w.ok  => {
          Ok
        }
        case _          => {
          InternalServerError
        }
      }
    }
  }

  def deleteReportingEntityData(docRefId:String) = Action.async{ implicit request =>
    reportingEntityDataRepo.delete(DocRefId(docRefId)).map(wr => if(wr.n == 0) NotFound else Ok)
  }

  def updateReportingEntityCreationDate(docRefId:String, creationDate: String) = Action.async {
    implicit request => {
      val dri = DocRefId(docRefId)
      val cd = LocalDate.parse(creationDate)

      reportingEntityDataRepo.updateCreationDate(dri, cd).map {
        case true => Ok
        case false => NotModified
      }
    }
  }

  def deleteReportingEntityCreationDate(docRefId:String) = Action.async {
    implicit request => {
      val dri = DocRefId(docRefId)

      reportingEntityDataRepo.deleteCreationDate(dri).map {
        case true => Ok
        case false => NotModified
      }
    }
  }

}
