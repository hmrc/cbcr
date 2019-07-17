/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.cbcr.actors

import akka.actor.{ActorIdentity, ActorSystem, Identify, PoisonPill}
import akka.persistence.inmemory.extension.{InMemoryJournalStorage, StorageExtension}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.Eventually
import org.scalatest.{Matchers, WordSpecLike}
import uk.gov.hmrc.cbcr.models.CBCId
import uk.gov.hmrc.cbcr.services.CBCIdGenCommands.{GenerateCBCId, GenerateCBCIdResponse}
import uk.gov.hmrc.cbcr.services.CBCIdGenerator

class CBCIdGeneratorSpec extends TestKit(
  ActorSystem("CBCIdGeneratorSpec",ConfigFactory.parseString(
  """
    |akka {
    | persistence {
    |   journal.plugin = "inmemory-journal"
    |   snapshot-store.plugin = "inmemory-snapshot-store"
    | }
    |}
  """.stripMargin))) with WordSpecLike with ImplicitSender with Matchers with Eventually {


  val generator = system.actorOf(CBCIdGenerator.props)

  "The CBCIdGenerator actor" should {

    "return a new CBCId when requested" in {
      generator ! GenerateCBCId
      expectMsgType[GenerateCBCIdResponse].value.isValid shouldBe true
    }

    "return a new CBCId on each request" in {
      generator ! GenerateCBCId
      val id1 = expectMsgType[GenerateCBCIdResponse]
      generator ! GenerateCBCId
      val id2 = expectMsgType[GenerateCBCIdResponse]
      id1.value.exists(id => id2.value.exists(_ != id))
    }

    "recover correctly when restarted" in {
      //First clear the journal
      val tp = TestProbe()
      tp.send(StorageExtension(system).journalStorage, InMemoryJournalStorage.ClearJournal)
      tp.expectMsg(akka.actor.Status.Success(""))

      //kill the actor
      watch(generator) ! PoisonPill
      expectTerminated(generator)

      //make sure its dead
      awaitAssert {
        generator ! Identify("dead?")
        expectMsg(ActorIdentity("dead?", None))
      }

      //create new generator
      val newGenerator = system.actorOf(CBCIdGenerator.props)

      //it should start generating ids from 1
      eventually {
        newGenerator ! GenerateCBCId
        val response = expectMsgType[GenerateCBCIdResponse]
        response.value.toOption.map(_.value) shouldEqual CBCId("XTCBC0100000001").map(_.value)
      }

      //lets request 4 more
      newGenerator ! GenerateCBCId
      expectMsgType[GenerateCBCIdResponse].value.isValid shouldBe true
      newGenerator ! GenerateCBCId
      expectMsgType[GenerateCBCIdResponse].value.isValid shouldBe true
      newGenerator ! GenerateCBCId
      expectMsgType[GenerateCBCIdResponse].value.isValid shouldBe true
      newGenerator ! GenerateCBCId
      expectMsgType[GenerateCBCIdResponse].value.isValid shouldBe true

      //now kill it and restart once dead
      watch(generator) ! PoisonPill
      expectTerminated(generator)

      awaitAssert {
        generator ! Identify("dead?")
        expectMsg(ActorIdentity("dead?", None))
      }

      val newNewGenerator = system.actorOf(CBCIdGenerator.props)

      // ensure it's the 6th id
      newNewGenerator ! GenerateCBCId
      expectMsgType[GenerateCBCIdResponse].value.exists(_.value == "XLCBC0000000006")
    }
  }

}
