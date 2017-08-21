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

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.{TestKit, TestProbe}
import cats.data.Validated.Invalid
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.play.OneAppPerSuite
import play.api.Configuration
import play.api.http.Status
import play.api.test.FakeRequest
import uk.gov.hmrc.cbcr.actors.CBCIdGenCommands.{GenerateCBCId, GenerateCBCIdResponse}
import uk.gov.hmrc.cbcr.models.CBCId
import uk.gov.hmrc.play.test.UnitSpec
import cats.syntax.option._
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext

class LocalCBCIdGeneratorSpec  extends TestKit(ActorSystem("CBCIdControllerSpec",ConfigFactory.parseString("""
  |akka {
  | persistence {
  |   journal.plugin = "inmemory-journal"
  |   snapshot-store.plugin = "inmemory-snapshot-store"
  | }
  |}
  |CBCId.controller {
  |  timeout = 2 seconds
  |  supervisor {
  |    minBackoff = 3 seconds
  |    maxBackoff = 10 minutes
  |  }
  |}
""".stripMargin))) with UnitSpec with Matchers with ScalaFutures with OneAppPerSuite{

  implicit val mat = ActorMaterializer()

  val testCBCIdGenerator = TestProbe("testCBCID")
  val config = system.settings.config
  implicit val as = app.injector.instanceOf[ActorSystem]
  implicit val ec = app.injector.instanceOf[ExecutionContext]

  val localGen = new LocalCBCIdGenerator(Configuration(config)){
    override private[services] lazy val cbcIdGenerator = testCBCIdGenerator.ref
  }


  "The LocalCBCIdGenerator" should {
    "respond with a 200 and a new CBCId when queried with getCBCId" in {
      val response = localGen.generateCBCId()
      testCBCIdGenerator.expectMsg(GenerateCBCId)
      testCBCIdGenerator.reply(GenerateCBCIdResponse(CBCId("XGCBC0000000001").toValid(new Exception("Test Error generating CBCId"))))
      status(response) shouldBe Status.OK
      jsonBodyOf(response).futureValue shouldEqual Json.obj("cbc-id" -> "XGCBC0000000001")
    }
    "respond with a 500 if the CBCIdGenerator service fails" in {
      val response = localGen.generateCBCId()
      testCBCIdGenerator.expectMsg(GenerateCBCId)
      testCBCIdGenerator.reply(GenerateCBCIdResponse(Invalid(new Exception("Failed to generate CBCId"))))
      status(response) shouldBe Status.INTERNAL_SERVER_ERROR
    }
    "respond with a 500 if the CBCIdGenerator service fails to respond" in {
      val response = localGen.generateCBCId()
      testCBCIdGenerator.expectMsg(GenerateCBCId)
      status(response) shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

}
