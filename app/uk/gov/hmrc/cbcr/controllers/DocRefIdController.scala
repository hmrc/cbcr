/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.Configuration
import play.api.mvc.Action
import uk.gov.hmrc.cbcr.auth.CBCRAuth
import uk.gov.hmrc.cbcr.models.{DocRefIdResponses, _}
import uk.gov.hmrc.cbcr.repositories.DocRefIdRepository
import uk.gov.hmrc.cbcr.services.RunMode
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DocRefIdController @Inject()(repo: DocRefIdRepository,config: Configuration, auth: CBCRAuth, runMode: RunMode)(implicit ec: ExecutionContext) extends BaseController {

  def query(docRefId: DocRefId) = auth.authCBCR  { implicit request =>
    repo.query(docRefId).map {
      case DocRefIdResponses.Valid => Ok
      case DocRefIdResponses.Invalid => Conflict
      case DocRefIdResponses.DoesNotExist => NotFound
    }
  }

  def saveDocRefId(docRefId: DocRefId) = auth.authCBCR { implicit request =>
    repo.save(docRefId).map {
      case DocRefIdResponses.Ok => Ok
      case DocRefIdResponses.AlreadyExists => Conflict
      case DocRefIdResponses.Failed => InternalServerError
    }
  }

  def saveCorrDocRefId(corrDocRefId: CorrDocRefId) = auth.authCBCR { implicit request =>
    val docRefId = request.body.asJson.getOrElse(throw new NotFoundException("No doc ref id found in the body")).as[DocRefId]
    repo.save(corrDocRefId, docRefId).map {
      case (DocRefIdResponses.Invalid, _) => BadRequest
      case (DocRefIdResponses.DoesNotExist, _) => NotFound
      case (DocRefIdResponses.Valid, Some(DocRefIdResponses.Ok)) => Ok
      case (DocRefIdResponses.Valid, Some(DocRefIdResponses.Failed)) => InternalServerError
      case (DocRefIdResponses.Valid, Some(DocRefIdResponses.AlreadyExists)) => BadRequest
      case (DocRefIdResponses.Valid, None) => InternalServerError

    }
  }

  def deleteDocRefId(docRefId: DocRefId) = Action.async{ implicit request =>
    if(config.underlying.getBoolean(s"${runMode.env}.CBCId.enableTestApis")) {
      repo.delete(docRefId).map {
        case w if w.ok && w.n >= 1 => Ok
        case w if w.ok && w.n == 0 => NotFound
        case _                     => InternalServerError
      }
    } else {
      Future.successful(NotImplemented)
    }
  }

}
