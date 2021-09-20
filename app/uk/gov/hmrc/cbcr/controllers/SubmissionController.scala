/*
 * Copyright 2021 HM Revenue & Customs
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

import org.slf4j.LoggerFactory
import play.api.Logging
import play.api.libs.json.{JsSuccess, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.cbcr.auth.CBCRAuth
import uk.gov.hmrc.cbcr.connectors.SubmissionConnector
import uk.gov.hmrc.cbcr.models.{ErrorDetails, SubmissionMetaData}
import uk.gov.hmrc.cbcr.services.{ContactService, TransformService}
import uk.gov.hmrc.http.HeaderNames.xSessionId
import uk.gov.hmrc.http.{HeaderNames, HttpResponse}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}
import scala.xml.NodeSeq

class SubmissionController @Inject()(
  cc: ControllerComponents,
  auth: CBCRAuth,
  contactService: ContactService,
  transformService: TransformService,
  submissionConnector: SubmissionConnector,
)(implicit ec: ExecutionContext)
    extends BackendController(cc) with Logging {

  def submitDocument: Action[NodeSeq] =
    auth.authCBCRWithXml(
      { implicit request =>
        {
          //receive xml and find import instructions
          val xml = transformService.addNameSpaceDefinitions(request.body)
          val fileName = (xml \ "fileName").text
          val submissionFile: NodeSeq = xml \ "file" \ "CBC_OECD"
          val cbcId = (xml \ "cbcId").text
          val submissionTime = LocalDateTime.now()
          val messageRefId = (xml \\ "MessageRefId").text

          val conversationID: String = hc
            .headers(HeaderNames.explicitlyIncludedHeaders)
            .find(_._1 == xSessionId)
            .map(
              n => n._2.replaceAll("session-", "")
            )
            .getOrElse(UUID.randomUUID().toString)

          val submissionMetaData = SubmissionMetaData.build(submissionTime, conversationID, fileName)

          for {
            subscriptionData <- contactService.getLatestContacts(cbcId)

            submissionXml: NodeSeq = transformService
              .addSubscriptionDetailsToSubmission(xml, subscriptionData, submissionMetaData)

            response <- submissionConnector.submitReport(submissionXml) map convertToResult
          } yield response
        }
      },
      parse.xml
    )

  private def convertToResult(httpResponse: HttpResponse): Result =
    httpResponse.status match {
      case OK        => Ok(httpResponse.body)
      case NOT_FOUND => NotFound(httpResponse.body)
      case BAD_REQUEST =>
        logDownStreamError(httpResponse.body)
        BadRequest(httpResponse.body)
      case FORBIDDEN =>
        logDownStreamError(httpResponse.body)
        Forbidden(httpResponse.body)
      case METHOD_NOT_ALLOWED =>
        logDownStreamError(httpResponse.body)
        MethodNotAllowed(httpResponse.body)
      case CONFLICT =>
        logDownStreamError(httpResponse.body)
        Conflict(httpResponse.body)
      case INTERNAL_SERVER_ERROR =>
        logDownStreamError(httpResponse.body)
        InternalServerError(httpResponse.body)
      case _ =>
        logDownStreamError(httpResponse.body)
        ServiceUnavailable(httpResponse.body)
    }

  private def logDownStreamError(body: String): Unit = {
    val error = Try(Json.parse(body).validate[ErrorDetails])
    error match {
      case Success(JsSuccess(value, _)) =>
        logger.error(s"Error with submission: ${value.errorDetail.sourceFaultDetail.map(_.detail.mkString)}")
      case _ => logger.error("Error with submission but return is not a valid json")
    }
  }

}
