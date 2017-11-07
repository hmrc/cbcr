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

import javax.inject.Inject

import com.ning.http.util.{Base64 => NingBase64}
import configs.syntax._
import play.api.libs.json.{JsObject, Json}
import play.api.{Configuration, Logger}
import uk.gov.hmrc.cbcr.audit.AuditConnectorI
import uk.gov.hmrc.cbcr.models.SubscriberContact
import uk.gov.hmrc.cbcr.repositories.SubscriptionDataRepository
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class SubscriptionDataUpdateService @Inject()(repo:SubscriptionDataRepository,
                                              configuration:Configuration,
                                              auditConnector:AuditConnectorI)(implicit ec:ExecutionContext)  {

  def decode(str: String): String = {
    Try(NingBase64.decode(str)) match {
      case Success(decoded) => new String(decoded)
      case Failure(_) => throw new RuntimeException(s"$str was not base64 encoded correctly or at all")
    }
  }

  def getSubscriberData(u: String): (JsObject, SubscriberContact) = {
    val safeId: String = configuration.underlying.get[String](s"users.${u}.safeId").valueOr(_ => "")
    val criteria = Json.obj("businessPartnerRecord.safeId" -> Json.toJson(safeId))
    val decoded: String = decode(configuration.underlying.get[String](s"users.${u}.sc").valueOr(_ => ""))
    val sc: SubscriberContact = Json.fromJson[SubscriberContact](Json.parse(decoded)).get
    (criteria, sc)
  }



  val doValidation: Boolean = configuration.underlying.get[Boolean]("CBCId.performDataUpdate").valueOr(_ => false)

  if (doValidation) {
    val x: Integer = configuration.underlying.get[Integer]("users.count").valueOr(_ => 0)
    1 to x foreach(n => {
      val usr1 = getSubscriberData(s"user${n}")

      def audit(result:String) = {
        auditConnector.sendExtendedEvent(ExtendedDataEvent("Country-By-Country", "CBCRSubsriberContactUpdate",
          tags = Map("result" -> result),
          detail = Json.toJson(usr1._2)))
      }

      repo.update(usr1._1, usr1._2).map(result =>
        if (result.booleanValue()) {
          Logger.info(s"validation succeeded for safeId: ${usr1._1}")
          audit("success")
        }
        else {
          Logger.info(s"validation failed for safeId: ${usr1._1}")
          audit("failure")
        }
      )
    })
  }
}
