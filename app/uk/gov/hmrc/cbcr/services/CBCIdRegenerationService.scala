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

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

import cats.data.EitherT
import cats.instances.all._
import cats.syntax.all._
import configs.syntax._
import play.api.libs.json.Json
import play.api.{Configuration, Logger}
import reactivemongo.api.commands.WriteResult
import uk.gov.hmrc.AuditConnector
import uk.gov.hmrc.cbcr.connectors.{DESConnector, EmailConnectorImpl}
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.repositories.SubscriptionDataRepository
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.{DataEvent, ExtendedDataEvent}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

class CBCIdRegenerationService @Inject() (emailService:EmailConnectorImpl, repo:SubscriptionDataRepository, des:DESConnector, configuration:Configuration, local:LocalSubscription) {

  val doRegenerate: Boolean = configuration.underlying.get[Boolean]("CBCId.regenerate").valueOr(_ => false)

  lazy val audit :AuditConnector = AuditConnector

  val REGENERATED_CBCID_TEMPLATE_ID: String = "cbcr_cbcid_regeneration"

  def auditCBCIdRegeneration(sd:SubscriptionDetails,oldCBCId:Option[CBCId],newCBCId:CBCId) : EitherT[Future,String, AuditResult.Success.type] = {
    EitherT(audit.sendExtendedEvent(ExtendedDataEvent("Country-By-Country-Backend", "CBCIdRegenerated",
      tags = Map(
        "oldCBCid" -> oldCBCId.map(_.toString).getOrElse(""),
        "newCBCId" -> newCBCId.toString
      ),
      detail = Json.toJson(sd)
      )).map {
        case AuditResult.Success         => Right(AuditResult.Success)
        case AuditResult.Disabled        => Right(AuditResult.Success)
        case AuditResult.Failure(msg, _) => Left(s"Unable to audit a CBCRegeneration: $msg")
      }
    )
  }

  if (doRegenerate) {
    implicit val hc = HeaderCarrier()
    val output = EitherT.right[Future,String,List[SubscriptionDetails]](repo.getSubscriptions(DataMigrationCriteria.PRIVATE_BETA_CRITERIA(configuration))).flatMap{ list =>
      val result = list.map{ sd =>
        for {
          newId <- EitherT[Future,String,CBCId](local.createCBCId.map(_.value.toEither.leftMap(t => s"Failed to generate CBCId: ${t.getMessage}")))
          oldId  = sd.cbcId
          _     <- EitherT.right[Future,String,WriteResult](repo.clear(sd.utr))
          _     <- EitherT.right[Future,String,WriteResult](repo.save(sd.copy(cbcId = Some(newId))))
          email  = Email(List(sd.subscriberContact.email.value),REGENERATED_CBCID_TEMPLATE_ID, Map(
            "cbcrId" -> newId.toString,
            "received_at" -> LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd 'at' HH:mm")),
            "f_name" -> sd.subscriberContact.firstName.getOrElse(""),
            "s_name" -> sd.subscriberContact.lastName.getOrElse(""))
          )
          _     <- EitherT.right[Future,String,HttpResponse](emailService.sendEmail(email))
          _     <- auditCBCIdRegeneration(sd,oldId,newId)
        } yield s"Generated new CBCId ${newId.value} replacing old CBCId ${oldId.map(_.value)} for UTR: ${sd.utr}"
      }

      result.sequence[({type λ[α] = EitherT[Future,String,α]})#λ,String]

    }

    output.fold(
      e => Logger.error(s"Failed to regenerate all CBCIds: $e"),
      o => Logger.error(s"Successfully regenerated all CBCIds:\n${o.mkString("\n")}")
    ).onFailure{
      case NonFatal(e) => Logger.error(s"Failed to regenerate all CBCIds: $e",e)
    }


  } else {
    Logger.error("Not doing regeneration")
  }

}

