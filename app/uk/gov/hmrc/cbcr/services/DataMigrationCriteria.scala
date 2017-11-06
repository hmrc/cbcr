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

import play.api.Configuration
import play.api.libs.json.Json
import configs.syntax._

object DataMigrationCriteria {

  def PRIVATE_BETA_CRITERIA(configuration: Configuration) = {

    val result = for {
      safeId1 <- configuration.underlying.get[String]("CBCId.safeId1")
      safeId2 <- configuration.underlying.get[String]("CBCId.safeId2")
      key      = "businessPartnerRecord.safeId"
    } yield Json.obj ("$or" -> Json.arr (Json.obj (key -> safeId1), Json.obj (key -> safeId2)))

    result.valueOrElse(Json.obj("cbcId" -> Json.obj("$regex" -> "X[A-Z]CBC00.*")))
  }

  val LOCAL_CBCID_CRITERIA = Json.obj("cbcId" -> Json.obj("$regex" -> "X[A-Z]CBC0.*"))

  val NAME_SPLIT_CRITERIA = Json.obj("subscriberContact.name" -> Json.obj("$exists" -> true),
    "subscriberContact.firstName" -> Json.obj("$exists" -> false))

  val ALL_SUBSCRIPTIONS = Json.obj()
}
