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
import uk.gov.hmrc.cbcr.util.SpecBase
import uk.gov.hmrc.cbcr.models.NamespaceForNode

import scala.xml.NodeSeq

class TransformServiceSpec extends SpecBase with StreamlinedXmlEquality {

  "TransformationService" should {

    "add nameSpaces to a file" in {

      val service = app.injector.instanceOf[TransformService]

      val file =
        <CBCRSubmissionInboundRequest xmlns:dac6="urn:eu:taxud:cbcr:v1"
                                        xmlns:eis="http://www.hmrc.gov.uk/cbcr/eis"
                                        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                        xsi:schemaLocation="http://www.hmrc.gov.uk/cbcr/eis/CBCR_EIS_UK_schema.xsd">
          <file>
            <result>true</result>
          </file>
        </CBCRSubmissionInboundRequest>

      val expected =
        <eis:CBCRSubmissionInboundRequest xmlns:dac6="urn:eu:taxud:cbcr:v1"
                                            xmlns:eis="http://www.hmrc.gov.uk/cbcr/eis"
                                            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                            xsi:schemaLocation="http://www.hmrc.gov.uk/cbcr/eis/CBCR_EIS_UK_schema.xsd">
          <eis:file>
            <eis:result>true</eis:result>
          </eis:file>
        </eis:CBCRSubmissionInboundRequest>

      val result: NodeSeq = service.addNameSpaces(file, Seq(NamespaceForNode("CBCRSubmissionInboundRequest", "eis")))

      result shouldBe expected

    }
  }
}
