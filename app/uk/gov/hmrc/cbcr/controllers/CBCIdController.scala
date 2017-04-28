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
import javax.inject._

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.{Backoff, BackoffSupervisor, ask}
import akka.util.Timeout
import configs.syntax._
import play.api.Configuration
import play.api.mvc.Action
import uk.gov.hmrc.cbcr.models.CBCId
import uk.gov.hmrc.cbcr.services.CBCIdGenCommands.{GenerateCBCId, GenerateCBCIdResponse}
import uk.gov.hmrc.cbcr.services.CBCIdGenerator
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.control.NonFatal

@Singleton
class CBCIdController @Inject() (system: ActorSystem,config:Configuration)(implicit ec:ExecutionContext) extends BaseController with ServicesConfig{

  val conf = config.underlying.getConfig("CBCId.controller")
  implicit val timeoutValue:Timeout = Timeout(conf.get[FiniteDuration]("timeout").value)
  val minBackoff: FiniteDuration = conf.get[FiniteDuration]("supervisor.minBackoff").value
  val maxBackoff: FiniteDuration = conf.get[FiniteDuration]("supervisor.maxBackoff").value

  val supervisor: Props = BackoffSupervisor.props(
    Backoff.onStop(
      childProps = CBCIdGenerator.props,
      childName = "cbc-id-generator",
      minBackoff = minBackoff,
      maxBackoff = maxBackoff,
      randomFactor = 0.0
    )
  )

  val cbcIdGenerator: ActorRef = system.actorOf(supervisor,"cbc-id-generator-supervisor")

  def getCBCId = Action.async {
    (cbcIdGenerator ? GenerateCBCId).mapTo[GenerateCBCIdResponse].map(_.value.fold(
      (error: Throwable) => InternalServerError(error.getMessage),
      (id: CBCId)        => Ok(id.value)
    )).recover{
      case NonFatal(e) => InternalServerError(e.getMessage)
    }
  }

}
