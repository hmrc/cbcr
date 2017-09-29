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

import javax.inject.{Inject, Singleton}

import play.api.mvc.{Action, BodyParsers}
import uk.gov.hmrc.cbcr.auth.CBCRAuth
import uk.gov.hmrc.cbcr.connectors.DESConnector
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext

@Singleton
class BusinessPartnerRecordController @Inject() (connector:DESConnector, auth:CBCRAuth)(implicit val ec:ExecutionContext) extends BaseController{

  def getBusinessPartnerRecord(utr:String) = auth.authCBCR{ implicit request =>
    connector.lookup(utr).map {
      case response if response.status == OK          => Ok(response.json)
      case response if response.status == BAD_REQUEST => BadRequest(response.json)
      case response if response.status == NOT_FOUND   => NotFound
      case _                                          => InternalServerError
    }
  }

}
