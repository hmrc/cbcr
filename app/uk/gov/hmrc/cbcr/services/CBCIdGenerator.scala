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

import akka.actor.ActorLogging
import akka.persistence.PersistentActor
import cats.data.Validated
import uk.gov.hmrc.cbcr.models.CBCId
import uk.gov.hmrc.cbcr.services.CBCIdGenCommands.{CbcIdIncrementEvent, GenerateCBCId, GenerateCBCIdResponse}


class CBCIdGenerator extends PersistentActor with ActorLogging{

  private var cbcIdCount: Int = 0

  def incCbcIdCount() : Unit = cbcIdCount = cbcIdCount + 1

  override def persistenceId: String = "CBCIdGenerator"

  override def receiveCommand: Receive = {
    case GenerateCBCId =>
      val newCbcId = CBCId.create(cbcIdCount+1)
      newCbcId.fold(
        error => log.error(error,s"Failed to generate CBCId: ${error.getMessage}"),
        _     => persist(CbcIdIncrementEvent)(_ => incCbcIdCount())
      )
      sender() ! GenerateCBCIdResponse(newCbcId)
  }

  override def receiveRecover: Receive = {
    case CbcIdIncrementEvent => incCbcIdCount()
  }

}

object CBCIdGenCommands {

  case object GenerateCBCId
  case class GenerateCBCIdResponse(value:Validated[Throwable,CBCId])
  case object CbcIdIncrementEvent

}
