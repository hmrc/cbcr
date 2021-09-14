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

import org.scalatest.StreamlinedXmlEquality
import uk.gov.hmrc.cbcr.models.{NamespaceForNode, PhoneNumber, SubscriberContact}
import uk.gov.hmrc.cbcr.util.SpecBase
import uk.gov.hmrc.emailaddress.EmailAddress

import scala.xml.NodeSeq

class TransformServiceSpec extends SpecBase with StreamlinedXmlEquality {

  "TransformationService" should {

    "add nameSpaces to a file" in {

      val service = app.injector.instanceOf[TransformService]

      val file =
        <CBCSubmissionInboundRequest xmlns:cbc="urn:eu:taxud:cbc:v1"
                                        xmlns:eis="http://www.hmrc.gov.uk/cbc/eis"
                                        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                        xsi:schemaLocation="http://www.hmrc.gov.uk/cbc/eis/CBC_EIS_UK_schema.xsd">
          <file>
            <result>true</result>
          </file>
        </CBCSubmissionInboundRequest>

      val expected =
        <eis:CBCSubmissionInboundRequest xmlns:cbc="urn:eu:taxud:cbc:v1"
                                            xmlns:eis="http://www.hmrc.gov.uk/cbc/eis"
                                            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                            xsi:schemaLocation="http://www.hmrc.gov.uk/cbc/eis/CBC_EIS_UK_schema.xsd">
          <eis:file>
            <eis:result>true</eis:result>
          </eis:file>
        </eis:CBCSubmissionInboundRequest>

      val result: NodeSeq = service.addNameSpaces(file, Seq(NamespaceForNode("CBCSubmissionInboundRequest", "eis")))

      result shouldBe expected

    }

    "handle two Namespaces in a file" in {
      val service = app.injector.instanceOf[TransformService]
      val file =
        <CBCSubmissionInboundRequest xmlns:cbc="urn:eu:taxud:cbc:v1"
                                        xmlns:eis="http://www.hmrc.gov.uk/cbc/eis"
                                        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                        xsi:schemaLocation="http://www.hmrc.gov.uk/cbc/eis/CBC_EIS_UK_schema.xsd">
          <file>
            <result>true</result>
            <requestDetail>
              <CBC_Report xmlns:cbc="urn:cbc:v0.1"
                                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                version="2020-04-16T09:30:47Z">
                <submission>Submitted Data</submission>
              </CBC_Report>
            </requestDetail>
          </file>
        </CBCSubmissionInboundRequest>

      val expected =
        <eis:CBCSubmissionInboundRequest xmlns:cbc="urn:eu:taxud:cbc:v1"
                                            xmlns:eis="http://www.hmrc.gov.uk/cbc/eis"
                                            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                            xsi:schemaLocation="http://www.hmrc.gov.uk/cbc/eis/CBC_EIS_UK_schema.xsd">
          <eis:file>
            <eis:result>true</eis:result>
            <eis:requestDetail>
              <cbc:CBC_Report xmlns:cbc="urn:cbc:v0.1"
                                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                     version="2020-04-16T09:30:47Z">
                <cbc:submission>Submitted Data</cbc:submission>
              </cbc:CBC_Report>
            </eis:requestDetail>
          </eis:file>
        </eis:CBCSubmissionInboundRequest>

      val result: NodeSeq = service.addNameSpaces(
        file,
        Seq(
          NamespaceForNode("CBCSubmissionInboundRequest", "eis"),
          NamespaceForNode("CBC_Report", "cbc")
        ))

      result shouldBe expected
    }

    "add namespace definitions for a CBCR Submission" in {
      val service = app.injector.instanceOf[TransformService]
      val file = <CBC_Report version="2020-04-16T09:30:47Z">
        <submission>Submitted Data</submission>
      </CBC_Report>

      val expected = <CBC_Report version="2020-04-16T09:30:47Z"
                                       xmlns:cbc="urn:cbc:v0.1"
                                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <submission>Submitted Data</submission>
      </CBC_Report>

      val result = service.addNameSpaceDefinitions(file)

      result.toString shouldBe expected.toString
    }

    "must transform ContactInformation from SubscriberContact" in {
      val service = app.injector.instanceOf[TransformService]

      val contactInformation = SubscriberContact(
        name = None,
        firstName = "firstName",
        lastName = "lastName",
        phoneNumber = PhoneNumber("123456789").get,
        email = EmailAddress("email@email.com")
      )

      val expected =
        <contactDetails><phoneNumber>{contactInformation.phoneNumber.number}</phoneNumber><emailAddress>{contactInformation.email}</emailAddress><individualDetails><firstName>{contactInformation.firstName}</firstName><lastName>{contactInformation.lastName}</lastName></individualDetails></contactDetails>

      val result =
        service.transformContactInformation(contactInformation)

      result shouldBe expected
    }
  }
}
