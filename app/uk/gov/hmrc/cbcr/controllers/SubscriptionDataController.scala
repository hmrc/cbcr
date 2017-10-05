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

import play.api.{Configuration, Logger}
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.cbcr.models._
import uk.gov.hmrc.cbcr.repositories.SubscriptionDataRepository
import uk.gov.hmrc.play.microservice.controller.BaseController
import cats.instances.all._
import cats.syntax.all._
import uk.gov.hmrc.cbcr.connectors.DESConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import configs.syntax._

@Singleton
class SubscriptionDataController @Inject() (repo:SubscriptionDataRepository,des:DESConnector, configuration:Configuration) extends BaseController {

  private def migrationRequest(s:SubscriptionDetails):Option[MigrationRequest] = {
    s.cbcId.map{id =>
      MigrationRequest(
        s.businessPartnerRecord.safeId,
        id.value,
        CorrespondenceDetails(
          s.businessPartnerRecord.address,
          ContactDetails(s.subscriberContact.email,s.subscriberContact.phoneNumber),
          ContactName(s.subscriberContact.firstName,s.subscriberContact.lastName)
        )
      )
    }
  }

  val doMigration: Boolean = configuration.underlying.get[Boolean]("CBCId.performMigration").valueOr(_ => false)

  if(doMigration) {
    Await.result(repo.getAllMigrations().map { list =>
      Logger.info(s"Got ${list.size} subscriptions to migrate from mongo")
      list.foreach { sd =>
        migrationRequest(sd).fold(Logger.error(s"No cbcID found for $sd")
        )(mr => Await.result(des.createMigration(mr).map(sd -> _), 1.minute).map { res =>
          if (res.status != 200) {
            Logger.error(s"${sd.cbcId} -------> FAILED with status code ${res.status}\n${res.body}")
          } else {
            Logger.info(s"${sd.cbcId} -------> Migrated")
          }
        })
      }
    }, 10.minutes)
  }


  def saveSubscriptionData(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[SubscriptionDetails].fold(
      error    => Future.successful(BadRequest(JsError.toJson(error))),
      response => repo.save(response).map {
        case result if !result.ok => InternalServerError(result.writeErrors.mkString)
        case _ => Ok
      }
    )
  }

  def updateSubscriberContactDetails(cbcId:CBCId) = Action.async(parse.json) { implicit request =>
    request.body.validate[SubscriberContact].fold(
      error    => Future.successful(BadRequest(JsError.toJson(error))),
      response => repo.update(cbcId,response).map {
        case result if !result => InternalServerError
        case _                 => Ok
      }
    )
  }

  def clearSubscriptionData(cbcId:CBCId):Action[AnyContent] = Action.async{ implicit request =>
    repo.clear(cbcId).cata[Result](
      NotFound,
      result => if(!result.ok) InternalServerError(result.writeErrors.mkString) else Ok("ok")
    )
  }

  def retrieveSubscriptionDataUtr(utr:Utr):Action[AnyContent] = Action.async { implicit request =>
    repo.get(utr).cata(
      NotFound,
      details => Ok(Json.toJson(details))
    )
  }

  def retrieveSubscriptionDataCBCId(cbcId:CBCId):Action[AnyContent] = Action.async{ implicit request =>
    repo.get(cbcId).cata(
      NotFound,
      details => Ok(Json.toJson(details))
    )
  }

}
