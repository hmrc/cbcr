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

import com.google.inject.Singleton
import javax.inject.Inject
import play.api.Configuration
import play.api.libs.json.{Format, Json}
import play.api.mvc.Action
import uk.gov.hmrc.cbcr.audit.AuditConnectorI
import uk.gov.hmrc.cbcr.models.DocRefIdRecord
import uk.gov.hmrc.cbcr.repositories.ReactiveDocRefIdRepository
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.ExecutionContext
@Singleton
class AdminService @Inject()(docRefIdRepo:ReactiveDocRefIdRepository,
                                      configuration:Configuration,
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