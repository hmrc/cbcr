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

import play.api.mvc.{Action, Result}
import uk.gov.hmrc.cbcr.models.Email
import uk.gov.hmrc.cbcr.services.EmailService
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.Future

@Singleton
class CBCREmailController @Inject()(email: EmailService) extends BaseController {

  //todo have email validation?
  def sendEmail = Action.async(parse.json) { implicit request =>
    request.body.validate[Email].fold[Future[Result]](
      _ => Future.successful(BadRequest),
      emailCBCR => email.sendEmail(emailCBCR)
    )
  }
}