/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.mvc.ControllerComponents
import uk.gov.hmrc.cbcr.auth.CBCRAuth
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.repositories.DocRefIdRepository
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class DocRefIdController @Inject()(repo: DocRefIdRepository, auth: CBCRAuth, cc: ControllerComponents)(
  implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def query(docRefId: DocRefId) = auth.authCBCR { _ =>
    repo.query(docRefId).map {
      case DocRefIdResponses.Valid        => Ok
      case DocRefIdResponses.Invalid      => Conflict
      case DocRefIdResponses.DoesNotExist => NotFound
    }
  }

  def saveDocRefId(docRefId: DocRefId) = auth.authCBCR { _ =>
    repo.save2(docRefId).map {
      case DocRefIdResponses.Ok            => Ok
      case DocRefIdResponses.AlreadyExists => Conflict
      case DocRefIdResponses.Failed        => InternalServerError
    }
  }

  def saveCorrDocRefId(corrDocRefId: CorrDocRefId) = auth.authCBCR { implicit request =>
    val docRefId =
      request.body.asJson.getOrElse(throw new NotFoundException("No doc ref id found in the body")).as[DocRefId]
    repo.save2(corrDocRefId, docRefId).map {
      case (DocRefIdResponses.Invalid, _)                                   => BadRequest
      case (DocRefIdResponses.DoesNotExist, _)                              => NotFound
      case (DocRefIdResponses.Valid, Some(DocRefIdResponses.Ok))            => Ok
      case (DocRefIdResponses.Valid, Some(DocRefIdResponses.Failed))        => InternalServerError
      case (DocRefIdResponses.Valid, Some(DocRefIdResponses.AlreadyExists)) => BadRequest
      case (DocRefIdResponses.Valid, None)                                  => InternalServerError

    }
  }
}
