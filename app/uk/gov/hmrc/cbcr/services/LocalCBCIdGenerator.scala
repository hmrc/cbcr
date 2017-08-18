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
import javax.inject._

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.{Backoff, BackoffSupervisor, ask}
import akka.util.Timeout
import configs.syntax._
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results._
import uk.gov.hmrc.cbcr.actors.CBCIdGenCommands.{GenerateCBCId, GenerateCBCIdResponse}
import uk.gov.hmrc.cbcr.actors.CBCIdGenerator
import uk.gov.hmrc.cbcr.models._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.control.NonFatal

@Singleton
class LocalCBCIdGenerator @Inject()(config:Configuration)(implicit system:ActorSystem) {

  private val conf = config.underlying.getConfig("CBCId")
  private implicit val timeoutValue: Timeout = Timeout(conf.get[FiniteDuration]("controller.timeout").value)
  private val minBackoff: FiniteDuration = conf.get[FiniteDuration]("controller.supervisor.minBackoff").value
  private val maxBackoff: FiniteDuration = conf.get[FiniteDuration]("controller.supervisor.maxBackoff").value

  private val supervisor: Props = BackoffSupervisor.props(
    Backoff.onStop(
      childProps = CBCIdGenerator.props,
      childName = "cbc-id-generator",
      minBackoff = minBackoff,
      maxBackoff = maxBackoff,
      randomFactor = 0.0
    )
  )

  private [services] lazy val cbcIdGenerator: ActorRef = system.actorOf(supervisor, "cbc-id-generator-supervisor")


  def generateCBCId()(implicit ec:ExecutionContext): Future[Result] = {
    (cbcIdGenerator ? GenerateCBCId).mapTo[GenerateCBCIdResponse].map(_.value.fold(
      (error: Throwable) => InternalServerError(error.getMessage),
      (id: CBCId)        => Ok(Json.obj("cbc-id" -> id.value))
    )).recover {
      case NonFatal(e) => InternalServerError(e.getMessage)
    }
  }
}
