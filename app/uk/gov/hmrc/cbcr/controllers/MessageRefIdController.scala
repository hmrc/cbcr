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

package uk.gov.hmrc.cbcr.controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.cbcr.auth.CBCRAuth
import uk.gov.hmrc.cbcr.models.MessageRefId
import uk.gov.hmrc.cbcr.repositories.MessageRefIdRepository
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext

@Singleton
class MessageRefIdController @Inject()(repo: MessageRefIdRepository, auth: CBCRAuth, cc: ControllerComponents)(
  implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def save(messageRefId: String) = auth.authCBCR { _ =>
    repo.save(MessageRefId(messageRefId)).map { wr =>
      if (wr.ok) Ok
      else InternalServerError
    }
  }

  def exists(messageRefId: String) = auth.authCBCR { _ =>
    repo.exists(messageRefId).map(result => if (result) Ok else NotFound)
  }

}
