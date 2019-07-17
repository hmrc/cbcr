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

import java.time.LocalDate

import com.google.inject.Singleton
import javax.inject.Inject
import play.api.{Configuration, Logger}
import play.api.libs.json.{Format, Json}
import play.api.mvc.Action
import uk.gov.hmrc.cbcr.audit.AuditConnectorI
import uk.gov.hmrc.cbcr.models.DocRefIdRecord
import uk.gov.hmrc.cbcr.repositories.ReactiveDocRefIdRepository
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.cbcr.models.{CBCId, DocRefId, DocRefIdRecord}
import uk.gov.hmrc.cbcr.repositories.{ReactiveDocRefIdRepository, ReportingEntityDataRepo}

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

@Singleton
class AdminService @Inject()(docRefIdRepo:ReactiveDocRefIdRepository,
                                      configuration:Configuration,
                             repo:ReportingEntityDataRepo,
                                      runMode: RunMode,
                                      audit: AuditConnectorI)(implicit ec:ExecutionContext) extends BaseController {



def showAllDocRef = Action.async {
  implicit request =>

    docRefIdRepo.findAll().map( response => Ok(Json.toJson(displayAllDocRefId(response))))

}


  def countDocRefId(docs : List[DocRefIdRecord]): ListDocRefIdRecord = {
    ListDocRefIdRecord(docs.filterNot(doc => doc.id.id.length < 200))
  }


  def displayAllDocRefId(docs : List[DocRefIdRecord]): ListDocRefIdRecord = {
    ListDocRefIdRecord(docs)
  }

  def adminDocRefIdquery(d: DocRefId) = Action.async { implicit request =>
    repo.query(d).map {
      case None => NotFound
      case Some(data) => Ok(Json.toJson(data))
    }.recover {
      case NonFatal(t) =>
        Logger.error(s"Exception thrown trying to query for ReportingEntityData: ${t.getMessage}", t)
        InternalServerError
    }

  }

  def adminQueryTin(tin: String, reportingPeriod: String) = Action.async { implicit request =>
    repo.queryTIN(tin, reportingPeriod).map { reportEntityData =>

      if (reportEntityData.isEmpty) NotFound else Ok(Json.toJson(reportEntityData.head))
    }.recover {
      case NonFatal(t) =>
        Logger.error(s"Exception thrown trying to query for ReportingEntityData: ${t.getMessage}", t)
        InternalServerError
    }
  }

  def adminQueryCbcId(cbcId: CBCId, reportingPeriod: String) = Action.async { implicit request =>

    repo.queryCbcId(cbcId, LocalDate.parse(reportingPeriod)).map {
      case None => NotFound
      case Some(data) => Ok(Json.toJson(data))
    }.recover {
      case NonFatal(t) =>
        Logger.error(s"Exception thrown trying to query for ReportingEntityData: ${t.getMessage}", t)
        InternalServerError
    }

  }
//  def checkDocRefIdLength(docs:DocRefIdRecord): Option[DocRefIdRecord] = {
//    Logger.warn("Finding docRefIds that are greater than 200")
//    Option(docs)
//  }

  //def findAllSubmissions = docRefIdRepo.findAll.map(x => checkDocRefIdLength(x))

  //def showAllCollection = Logger.warn(s" something something: \n $findAllSubmissions")
}
case class ListDocRefIdRecord(docs: List[DocRefIdRecord])
object ListDocRefIdRecord {
  implicit val format:Format[ListDocRefIdRecord] = Json.format[ListDocRefIdRecord]
}