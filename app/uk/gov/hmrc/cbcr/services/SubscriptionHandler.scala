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

import javax.inject.{Inject,Singleton}

import play.api.Configuration
import play.api.mvc.Result
import uk.gov.hmrc.cbcr.models.{CorrespondenceDetails, SubscriptionRequest}

import scala.concurrent.Future
import configs.syntax._
import uk.gov.hmrc.play.http.HeaderCarrier

trait SubscriptionHandler {

  def createSubscription(sub:SubscriptionRequest)(implicit hc:HeaderCarrier) : Future[Result]
  def updateSubscription(safeId:String, details:CorrespondenceDetails)(implicit hc: HeaderCarrier) : Future[Result]
  def getSubscription(safeId:String)(implicit hc:HeaderCarrier) : Future[Result]

}

@Singleton
class SubscriptionHandlerImpl @Inject() (configuration: Configuration, localCBCIdGenerator: LocalSubscription, remoteCBCIdGenerator: RemoteSubscription) extends SubscriptionHandler{

  val conf      = configuration.underlying.getConfig("CBCId")
  val useDESApi = conf.get[Boolean]("useDESApi").value

  val handler:SubscriptionHandler = if(useDESApi) remoteCBCIdGenerator else localCBCIdGenerator

  override def createSubscription(sub: SubscriptionRequest)(implicit hc:HeaderCarrier) = handler.createSubscription(sub)

  override def updateSubscription(safeId: String, details: CorrespondenceDetails)(implicit hc:HeaderCarrier) = handler.updateSubscription(safeId,details)

  override def getSubscription(safeId: String)(implicit hc:HeaderCarrier) = handler.getSubscription(safeId)

}
