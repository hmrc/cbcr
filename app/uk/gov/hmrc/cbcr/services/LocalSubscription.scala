/*
 * Copyright 2022 HM Revenue & Customs
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
import java.time.LocalDateTime
import javax.inject._
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results._
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.repositories.SubscriptionDataRepository
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import cats.instances.future._
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class LocalSubscription @Inject()(
  config: Configuration,
  repo: SubscriptionDataRepository,
  cbcIdGenerator: CBCIdGenerator)(implicit ec: ExecutionContext)
    extends SubscriptionHandler {

  def createCBCId =
    cbcIdGenerator.generateCbcId.fold(
      (error: Throwable) => throw error,
      (id: CBCId) => Future.successful(id)
    )

  override def createSubscription(sub: SubscriptionRequest)(implicit hc: HeaderCarrier): Future[Result] =
    createCBCId
      .map { id: CBCId =>
        Ok(Json.obj("cbc-id" -> id.value))
      }
      .recover {
        case NonFatal(e) => InternalServerError(e.getMessage)
      }

  override def updateSubscription(safeId: String, details: CorrespondenceDetails)(implicit hc: HeaderCarrier) =
    Future.successful(Ok(Json.toJson(UpdateResponse(LocalDateTime.now))))

  override def getSubscription(safeId: String)(implicit hc: HeaderCarrier) =
    repo
      .get(safeId)
      .map {
        case None => NotFound
        case Some(sd) =>
          val response = GetResponse(
            safeId,
            ContactName(sd.subscriberContact.firstName, sd.subscriberContact.lastName),
            ContactDetails(sd.subscriberContact.email, sd.subscriberContact.phoneNumber),
            sd.businessPartnerRecord.address
          )
          Ok(Json.toJson(response))
      }
}
