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

package uk.gov.hmrc.cbcr.controllers
import javax.inject._

import play.api.libs.json.Json

//import com.oracle.tools.packager.Log.Logger
import configs.syntax._
import play.api.Configuration
import play.api.mvc.{Action, Result}
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.services.{LocalCBCIdGenerator, RemoteCBCIdGenerator}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.{ExecutionContext, Future}

import play.api.Logger

@Singleton
class CBCIdController @Inject()(config:Configuration,
                                localGen: LocalCBCIdGenerator,
                                remoteGen: RemoteCBCIdGenerator)(implicit ec:ExecutionContext) extends BaseController with ServicesConfig{

  val conf = config.underlying.getConfig("CBCId")
  val useDESApi = conf.get[Boolean]("useDESApi").value

  def subscribe = Action.async(parse.json) { implicit request =>
    request.body.validate[SubscriptionDetails].fold[Future[Result]](
      _   => Future.successful(BadRequest),
      srb => if (useDESApi) {
        Logger.info(s"************* Use DESAPI $srb")
        remoteGen.generateCBCId(srb)
      } else {
        Logger.info("************* Don't use DES")
        localGen.generateCBCId()
      }
    )
  }

  @inline implicit private def subscriptionDetailsToSubscriptionRequestBody(s:SubscriptionDetails):SubscriptionRequest ={
    SubscriptionRequest(
      s.businessPartnerRecord.safeId,
      false,
      CorrespondenceDetails(
        s.businessPartnerRecord.address,
        ContactDetails(s.subscriberContact.email,s.subscriberContact.phoneNumber),
        ContactName(s.subscriberContact.firstName,s.subscriberContact.lastName)
      )
    )
  }

}
