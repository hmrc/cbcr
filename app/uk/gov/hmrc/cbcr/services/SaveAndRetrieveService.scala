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

import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.cbcr.core.Opt
import uk.gov.hmrc.cbcr.models.{DbOperationResult, SaveAndRetrieve}
import uk.gov.hmrc.cbcr.typeclasses._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object SaveService {
  def save(entity: SaveAndRetrieve, cbcId: String)(implicit save: Save[SaveAndRetrieve]): Future[Opt[DbOperationResult]] = {
    save(entity)
  }
}

object RetrieveService {
  def retrieve(cbcId: String, envelopeId: String)(implicit find: FindOne[SaveAndRetrieve]): Future[Option[SaveAndRetrieve]] = {

  val criteria = Json.obj("envelopeId" -> envelopeId, "status" -> "AVAILABLE")

    Logger.debug("retrieve json for the criteria: "+Json.stringify(criteria))
    find(criteria)
  }
}
