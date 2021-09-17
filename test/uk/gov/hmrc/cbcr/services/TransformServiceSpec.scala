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
import uk.gov.hmrc.cbcr.models.NamespaceForNode
import uk.gov.hmrc.cbcr.models.subscription.{ContactInformationForOrganisation, OrganisationDetails, SubscriptionDetails}
import uk.gov.hmrc.cbcr.util.SpecBase
import uk.gov.hmrc.emailaddress.EmailAddress

import scala.xml.NodeSeq
import scala.xml.Utility.trim

class TransformServiceSpec extends SpecBase with StreamlinedXmlEquality {

  "TransformationService" should {

    "add nameSpaces to a file" in {

      val service = app.injector.instanceOf[TransformService]

      //TODO - DAC6-1021 update schema when available

      val file =
        <CBCSubmissionInboundRequest xmlns:cbc="urn:oecd:ties:cbc:v2"
                                        xmlns:eis="http://www.hmrc.gov.uk/cbc/eis"
                                        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                        xsi:schemaLocation="http://www.hmrc.gov.uk/cbc/eis/CBC_EIS_UK_schema.xsd">
          <file>
            <result>true</result>
          </file>
        </CBCSubmissionInboundRequest>

      val expected =
        <eis:CBCSubmissionInboundRequest xmlns:cbc="urn:oecd:ties:cbc:v2"
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
        <CBCSubmissionInboundRequest xmlns:cbc="urn:oecd:ties:cbc:v2"
                                        xmlns:eis="http://www.hmrc.gov.uk/cbc/eis"
                                        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                        xsi:schemaLocation="http://www.hmrc.gov.uk/cbc/eis/CBC_EIS_UK_schema.xsd">
          <file>
            <result>true</result>
            <requestDetail>
              <CBC_Report xmlns:cbc="urn:oecd:ties:cbc:v2"
                                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                version="2020-04-16T09:30:47Z">
                <submission>Submitted Data</submission>
              </CBC_Report>
            </requestDetail>
          </file>
        </CBCSubmissionInboundRequest>

      val expected =
        <eis:CBCSubmissionInboundRequest xmlns:cbc="urn:oecd:ties:cbc:v2"
                                            xmlns:eis="http://www.hmrc.gov.uk/cbc/eis"
                                            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                            xsi:schemaLocation="http://www.hmrc.gov.uk/cbc/eis/CBC_EIS_UK_schema.xsd">
          <eis:file>
            <eis:result>true</eis:result>
            <eis:requestDetail>
              <cbc:CBC_Report xmlns:cbc="urn:oecd:ties:cbc:v2"
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
                                       xmlns:cbc="urn:oecd:ties:cbc:v2"
                                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <submission>Submitted Data</submission>
      </CBC_Report>

      val result = service.addNameSpaceDefinitions(file)

      result.toString shouldBe expected.toString
    }

    "must transform ContactInformation from SubscriberContact" in {
      val service = app.injector.instanceOf[TransformService]

      val contactInformation: ContactInformationForOrganisation = ContactInformationForOrganisation(
        organisation = OrganisationDetails("name"),
        email = EmailAddress("email@email.com"),
        Some("091111"),
        None
      )

      val expected = <contactDetails>
        <phoneNumber>091111</phoneNumber>
        <emailAddress>email@email.com</emailAddress>
        <organisationDetails>
          <organisationName>name</organisationName>
        </organisationDetails>
      </contactDetails>

      val result =
        <contactDetails>
        {service.transformContactInformation(contactInformation)}
        </contactDetails>

      trim(result) shouldBe trim(expected)
    }
  }

  "must transform Subscription Details" in {
    val service = app.injector.instanceOf[TransformService]

    val subscriptionDetails = SubscriptionDetails(
      "111111111",
      Some("tradingName"),
      true,
      ContactInformationForOrganisation(OrganisationDetails("org1"), "test@email.com", None, None),
      Some(ContactInformationForOrganisation(OrganisationDetails("org2"), "test1@email.com", None, None))
    )

    val expected: NodeSeq =
      <subscriptionDetails>
        <cbcId>111111111</cbcId> <tradingName>tradingName</tradingName>
        <isGBUser>true</isGBUser>
        <primaryContact>
          <emailAddress>test@email.com</emailAddress>
          <organisationDetails>
            <organisationName>org1</organisationName>
          </organisationDetails>
        </primaryContact> <secondaryContact>
        <emailAddress>test1@email.com</emailAddress> <organisationDetails>
          <organisationName>org2</organisationName>
        </organisationDetails>
      </secondaryContact>
      </subscriptionDetails>

    val result: NodeSeq =
      <subscriptionDetails>
      {service.transformSubscriptionDetails(subscriptionDetails, None)}
      </subscriptionDetails>

    result.map(trim).toString() shouldEqual expected.map(trim).toString()
  }
}
