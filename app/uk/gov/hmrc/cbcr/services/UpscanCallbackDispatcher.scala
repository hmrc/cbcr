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

package uk.gov.hmrc.cbcr.services

import org.slf4j.LoggerFactory
import uk.gov.hmrc.cbcr.models.upscan._

import javax.inject.Inject
import scala.concurrent.Future

class UpscanCallbackDispatcher @Inject()(sessionStorage: UploadProgressTracker) {
  private val logger = LoggerFactory.getLogger(getClass)

  def handleCallback(callback: CallbackBody): Future[Boolean] = {
    logger.debug("\n\nHandling the callback\n\n")
    val uploadStatus: UploadStatus = callback match {
      case s: ReadyCallbackBody =>
        logger.debug(s"ReadyCallbackBody: $s")
        UploadedSuccessfully(
          s.uploadDetails.fileName,
          s.uploadDetails.fileMimeType,
          s.downloadUrl.getFile,
          Some(s.uploadDetails.size))
      case s: FailedCallbackBody if s.failureDetails.failureReason == "QUARANTINE" =>
        logger.debug(s"FailedCallbackBody, QUARANTINE: $s")
        Quarantined
      case s: FailedCallbackBody if s.failureDetails.failureReason == "REJECTED" =>
        logger.debug(s"FailedCallbackBody, REJECTED: $s")
        UploadRejected(s.failureDetails)
      case f: FailedCallbackBody =>
        logger.debug(s"FailedCallbackBody: $f")
        Failed
    }
    sessionStorage.registerUploadResult(callback.reference, uploadStatus)
  }

}
