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

package uk.gov.hmrc.cbcr.repositories

import play.api.libs.json._
import reactivemongo.api.DefaultDB
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.cbcr.core.Opt
import uk.gov.hmrc.cbcr.models.{DbOperationResult, SaveAndRetrieve}
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SaveAndRetrieveRepository(implicit mongo: () => DefaultDB)
    extends ReactiveRepository[JsObject, BSONObjectID]("Save_And_Retrieve", mongo, implicitly[Format[JsObject]]) {

  def save(form: SaveAndRetrieve): Future[Opt[DbOperationResult]] = {
    val insert = collection.insert(form.value)
    checkUpdateResult(insert)
  }

  def retrieve(selector: JsObject): Future[Option[SaveAndRetrieve]] = {
    collection.find(selector).one[SaveAndRetrieve]
  }
}

