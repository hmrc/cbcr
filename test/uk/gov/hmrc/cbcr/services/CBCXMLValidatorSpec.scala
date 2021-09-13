/*
 * Copyright 2021 HM Revenue & Customs
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
import com.typesafe.config.ConfigFactory
import org.codehaus.stax2.validation.{XMLValidationSchema, XMLValidationSchemaFactory}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{Configuration, Environment}

import java.io.File

class CBCXMLValidatorSpec extends WordSpec with Matchers with GuiceOneAppPerSuite with MockitoSugar {

  private def loadFile(filename: String) = new File(s"test/resources/$filename")

  val validXmlFile = loadFile("cbcr-valid.xml")
  val invalidXmlFile = loadFile("cbcr-invalid.xml")
  val invalidMultipleXmlFile = loadFile("cbcr-invalid-multiple-errors.xml")
  val invalidMultipleXmlFile2 = loadFile("cbcr-invalid-multiple-errors2.xml")
  val fatal = loadFile("fatal.xml")
  val configuration = new Configuration(ConfigFactory.load("application.conf"))

  implicit val env = app.injector.instanceOf[Environment]

  implicit val as = app.injector.instanceOf[ActorSystem]

  val schemaVer: String = configuration
    .getOptional[String]("oecd-schema-version")
    .getOrElse("2.0")
  val xmlValidationSchemaFactory: XMLValidationSchemaFactory =
    XMLValidationSchemaFactory.newInstance(XMLValidationSchema.SCHEMA_ID_W3C_SCHEMA)
  val schemaFile: File = new File(s"conf/schema/$schemaVer/CbcXML_v$schemaVer.xsd")
  val validator = new CBCXMLValidator(xmlValidationSchemaFactory.createSchema(schemaFile))

  "An Xml Validator" should {
    "not return any error for a valid file" in {

      val validate: XmlErrorHandler = validator.validateSchema(validXmlFile)
      validate.hasErrors shouldBe false
      validate.hasFatalErrors shouldBe false
      validate.hasWarnings shouldBe false
    }

    "return an error if the file is invalid and a single error" in {

      val validate = validator.validateSchema(invalidXmlFile)
      validate.hasErrors shouldBe true
      validate.errorsCollection.size shouldBe 5
    }

    "return multiple errors if the file is invalid and has multiple errors" in {
      val validate = validator.validateSchema(invalidMultipleXmlFile)
      validate.hasErrors shouldBe true
      validate.errorsCollection.size shouldBe 20
    }

    "not throw errors if the validator encounters a fatal error" in {
      val validate = validator.validateSchema(fatal)
      validate.hasFatalErrors shouldBe true
    }

    "stop collecting errors after the configurable limit is reached" in {
      val validate = validator.validateSchema(invalidMultipleXmlFile2)
      validate.hasFatalErrors shouldBe true
      validate.hasErrors shouldBe true
      validate.errorsCollection.size shouldBe 100

    }

  }
}