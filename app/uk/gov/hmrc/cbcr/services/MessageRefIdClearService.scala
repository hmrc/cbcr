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

package uk.gov.hmrc.cbcr.services

import cats.instances.all.*
import cats.syntax.all.*
import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.cbcr.config.ApplicationConfig
import uk.gov.hmrc.cbcr.models.MessageRefId
import uk.gov.hmrc.cbcr.repositories.MessageRefIdRepository
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.chaining.scalaUtilChainingOps

@Singleton
class MessageRefIdClearService @Inject() (
  msgRefIdRepo: MessageRefIdRepository,
  configuration: ApplicationConfig,
  audit: AuditConnector
)(implicit ec: ExecutionContext) {

  lazy val logger: Logger = Logger(this.getClass)

  private val MSG_REFID_AUDIT = "CBCR-MsgRefIdClear"

  private val msgRefIds: Seq[MessageRefId] = configuration.msgRefIdsToClear.map(MessageRefId.apply)

  if (msgRefIds.nonEmpty) {
    logger.info(s"About to clear MsgRefIds:\n${msgRefIds.mkString("\n")}")
    def ignoreOutputAndHandleError(f: Future[?]): Future[Unit] =
      f.map(_ => ()).handleError(_.getMessage.pipe(logger.error(_)))
    Await.result(
      msgRefIds
        .map { id =>
          for {
            _   <- msgRefIdRepo.delete(id).pipe(ignoreOutputAndHandleError)
            err <- auditMsgRefIdClear(id)
          } yield err.foreach(logger.error(_))
        }
        .pipe(Future.sequence(_)),
      Duration(60, "second")
    )
  }

  private def auditMsgRefIdClear(msgRefId: MessageRefId): Future[Option[String]] = {
    val k =
      audit
        .sendExtendedEvent(
          ExtendedDataEvent(
            "Country-By-Country-Backend",
            MSG_REFID_AUDIT,
            detail = Json.obj(
              "msgRefId" -> Json.toJson(msgRefId)
            )
          )
        )
    k.map {
      case AuditResult.Success         => None
      case AuditResult.Failure(msg, _) => Some(s"failed to audit: $msg")
      case AuditResult.Disabled        => None
    }
  }
}
