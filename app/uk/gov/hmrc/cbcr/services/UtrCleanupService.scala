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

package uk.gov.hmrc.cbcr.services

import javax.inject.Inject

import cats.data.EitherT
import configs.syntax._
import play.api.{Configuration, Logger}
import uk.gov.hmrc.cbcr.repositories.SubscriptionDataRepository
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.cbcr.models.{SubscriptionDetails, Utr}
import play.api.libs.json.Json
import reactivemongo.api.commands.WriteResult
import uk.gov.hmrc.BackendAuditConnector
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.config.AppName
import cats.instances.future._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import uk.gov.hmrc.http.HeaderCarrier


class UtrCleanupService @Inject() (repo:SubscriptionDataRepository,
                                   configuration:Configuration,
                                   runMode: RunMode)
                                  (implicit ec:ExecutionContext){
  lazy val audit: AuditConnector = BackendAuditConnector
  private val UTR_AUDIT = "CBCR-UtrCleanUp"
  private val UTR_DELETE = "CBCR-UtrDeleteSuccess"

  private def auditUtr(s: SubscriptionDetails, auditType: String): EitherT[Future,String,Unit] = {
    implicit val hc = HeaderCarrier()
    Logger.info(s"creating auditing event $auditType for UTR ${s.utr}")

    EitherT[Future,String,Unit](for {
      result <- audit.sendExtendedEvent(ExtendedDataEvent(AppName.appName, auditType,
        tags = hc.toAuditTags(auditType, "N/A"),
        detail = Json.toJson(s)
      )).map {
        case AuditResult.Success          => Right(())
        case AuditResult.Failure(msg, _)  => Left(s"failed to audit: $msg")
        case AuditResult.Disabled         => Right(())
      }
    } yield result)
  }

  private def deleteUtr(utr: Utr, s: SubscriptionDetails): EitherT[Future,String, Unit] =
      EitherT(repo.clear(utr).map {
        case w if w.ok && w.n == 1 => Right(())
        case _                     => Left(s"Failed to delete UTR: $utr")
      })

  val doUtrAudit: Boolean = configuration.underlying.get[Boolean](s"${runMode.env}.UTR.audit").valueOr(_ => false)
  Logger.info(s"UTR.audit set to: $doUtrAudit")
  val doUtrDelete: Boolean = configuration.underlying.get[Boolean](s"${runMode.env}.UTR.delete").valueOr(_ => false)

  if (doUtrAudit) {
    val utrs: String = configuration.underlying.get[String](s"${runMode.env}.UTR.utrs").valueOr(_ => "")
    for (utr <- utrs.split("_").map(_.trim).toList) {
      Logger.info(s"utr: $utr")

      if (Utr(utr).isValid){
        repo.get(Utr(utr)).value.onComplete{
          case Success(Some(s)) =>
            val result = for {
            _ <- auditUtr(s,UTR_AUDIT)
            _ <- EitherT.cond[Future](doUtrDelete,(),"Deletion not enabled")
            _ <- deleteUtr(Utr(utr),s)
            _ <- auditUtr(s,UTR_DELETE)
          } yield ()

            result.fold(
              error => Logger.error(s"Something went wrong: $error"),
              _     => Logger.info(s"Successfully audited and deleted subscription data for $utr")
            )

          case Success(None)    => Logger.error(s"utr: $utr not found")
          case Failure(t)       => Logger.error("get utr failed with error", t)
        }
      } else {
        Logger.warn(s"utr: $utr is invalid")
      }
    }
  }
}
