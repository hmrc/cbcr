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

import akka.actor.{ActorLogging, Props}
import akka.persistence.PersistentActor
import cats.data.Validated
import uk.gov.hmrc.cbcr.models.CBCId
import uk.gov.hmrc.cbcr.services.CBCIdGenCommands.{CBCIdIncrementEvent, GenerateCBCId, GenerateCBCIdResponse}

/**
  * Actor to encapsulate the mutable state of CBCIdCount.
  * On each request it generates a new CBCId and on success, increments CBCIdCount
  */
class CBCIdGenerator extends PersistentActor with ActorLogging{

  private var CBCIdCount: Int = 0

  def incCBCIdCount() : Unit = CBCIdCount = CBCIdCount + 1

  override def persistenceId: String = "CBCIdGenerator"

  override def receiveCommand: Receive = {
    case GenerateCBCId =>
      val newCBCId = CBCId.create(CBCIdCount+1)
      newCBCId.fold(
        error => log.error(error,s"Failed to generate CBCId: ${error.getMessage}"),
        _     => persist(CBCIdIncrementEvent)(_ => incCBCIdCount())
      )
      sender() ! GenerateCBCIdResponse(newCBCId)
  }

  override def receiveRecover: Receive = {
    case CBCIdIncrementEvent => incCBCIdCount()
  }

}

object CBCIdGenerator {
  def props:Props = Props[CBCIdGenerator]
}

object CBCIdGenCommands {
  case object CBCIdIncrementEvent
  case object GenerateCBCId
  case class GenerateCBCIdResponse(value:Validated[Throwable,CBCId])
}
