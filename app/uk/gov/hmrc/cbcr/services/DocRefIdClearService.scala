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

import javax.inject.{Inject, Singleton}
import cats.instances.all._
import cats.syntax.all._
import configs.syntax._
import play.api.libs.json.Json
import play.api.{Configuration, Logger}
import uk.gov.hmrc.cbcr.models.DocRefId
import uk.gov.hmrc.cbcr.repositories.{DocRefIdRepository, ReportingEntityDataRepo}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.chaining.scalaUtilChainingOps

@Singleton
class DocRefIdClearService @Inject()(
  docRefIdRepo: DocRefIdRepository,
  reportingEntityDataRepo: ReportingEntityDataRepo,
  configuration: Configuration,
  runMode: RunMode,
  audit: AuditConnector)(implicit ec: ExecutionContext) {

  lazy val logger: Logger = Logger(this.getClass)

  private val DOCREFID_AUDIT = "CBCR-DocRefIdClear"

  val docRefIds: List[DocRefId] = configuration.underlying
    .get[String](s"${runMode.env}.DocRefId.clear")
    .valueOr(_ => "")
    .split("_")
    .toList
    .map(DocRefId.apply)

  if (docRefIds.nonEmpty) {
    logger.info(s"About to clear DocRefIds:\n${docRefIds.mkString("\n")}")
    def ignoreOutputAndHandleError(f: Future[_]): Future[Unit] =
      f.map(_ => ()).handleError(_.getMessage.pipe(logger.error(_)))
    Await.result(
      docRefIds
        .map { d =>
          for {
            _   <- docRefIdRepo.delete(d).pipe(ignoreOutputAndHandleError)
            _   <- reportingEntityDataRepo.delete(d).pipe(ignoreOutputAndHandleError)
            err <- auditDocRefIdClear(d)
          } yield err.foreach(logger.error(_))
        }
        .pipe(Future.sequence(_)),
      Duration(60, "second")
    )
  }

  private def auditDocRefIdClear(docRefId: DocRefId): Future[Option[String]] = {
    val k =
      audit
        .sendExtendedEvent(
          ExtendedDataEvent(
            "Country-By-Country-Backend",
            DOCREFID_AUDIT,
            detail = Json.obj(
              "docRefId" -> Json.toJson(docRefId)
            )))
    k.map {
      case AuditResult.Success         => None
      case AuditResult.Failure(msg, _) => Some(s"failed to audit: $msg")
      case AuditResult.Disabled        => None
    }
  }
}
