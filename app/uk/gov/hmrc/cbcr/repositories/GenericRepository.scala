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


import cats.data.OptionT
import play.api.libs.json._
import reactivemongo.api.DefaultDB
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.cbcr.core.{ServiceResponse, ServiceResponseOpt}
import uk.gov.hmrc.cbcr.models.DbOperationResult
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by max on 04/04/17.
  */
class GenericRepository[A](name:String)(implicit mongo: () => DefaultDB, format:OFormat[A], m: Manifest[A])
  extends ReactiveRepository[A, BSONObjectID](name, mongo, format){

  def save(form: A): ServiceResponse[DbOperationResult] = checkUpdateResult(collection.insert(form))

  def retrieve(selector: JsObject): ServiceResponseOpt[A] = OptionT(collection.find(selector).one[A])

}
