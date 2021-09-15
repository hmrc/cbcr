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

import uk.gov.hmrc.cbcr.models.{NamespaceForNode, ResponseDetails, SubmissionMetaData, SubscriberContact}

import javax.inject.Inject
import scala.xml._

class TransformService @Inject()() {

  //TODO - DAC6-1021 update schema when available

  def addNameSpaces(file: NodeSeq, namespaces: Seq[NamespaceForNode]): NodeSeq = {

    def changeNS(el: NodeSeq): NodeSeq = {
      def fixSeq(ns: Seq[Node], currentPrefix: Option[String]): Seq[Node] =
        for (node <- ns)
          yield
            node match {
              case elem: Elem =>
                namespaces
                  .find(
                    n => n.nodeName == elem.label
                  )
                  .map { n =>
                    elem.copy(
                      prefix = n.prefix,
                      child = fixSeq(elem.child, Some(n.prefix))
                    )
                  }
                  .getOrElse(
                    elem.copy(
                      prefix = currentPrefix.get,
                      child = fixSeq(elem.child, Some(currentPrefix.get))
                    )
                  )
              case other => other
            }

      fixSeq(el, None).head
    }

    changeNS(file)
  }

  def addNameSpaceDefinitions(submissionFile: NodeSeq): NodeSeq =
    for (node <- submissionFile)
      yield
        node match {
          case elem: Elem =>
            elem.copy(
              scope = NamespaceBinding(
                "xsi",
                "http://www.w3.org/2001/XMLSchema-instance",
                NamespaceBinding("cbc", "urn:oecd:ties:cbc:v2", TopScope)))
        }

  def transformContactInformation(
    contactInformation: SubscriberContact
  ): NodeSeq =
    <phoneNumber>{contactInformation.phoneNumber.number}</phoneNumber>
      <emailAddress>{contactInformation.email}</emailAddress>
      <individualDetails>
        <firstName>{contactInformation.firstName}</firstName>
        <lastName>{contactInformation.lastName}</lastName>
      </individualDetails>

  def transformSubscriptionDetails(
    subscriptionDetails: ResponseDetails,
    fileName: Option[String]
  ): NodeSeq =
    Seq(
      fileName.map(
        name => <fileName>{name}</fileName>
      ),
      Some(<cbcId>{subscriptionDetails.cbcId}</cbcId>),
      subscriptionDetails.tradingName.map(
        tradingName => <tradingName>{tradingName}</tradingName>
      ),
      Some(<isGBUser>{subscriptionDetails.isGBUser}</isGBUser>),
      Some(<primaryContact>
        {transformContactInformation(subscriptionDetails.primaryContact)}
      </primaryContact>),
      subscriptionDetails.secondaryContact.map(
        sc => <secondaryContact>
          {transformContactInformation(sc)}
        </secondaryContact>
      )
    ).filter(_.isDefined).map(_.get)

  def addSubscriptionDetailsToSubmission(
    submissionFile: NodeSeq,
    subscriptionDetails: ResponseDetails,
    metaData: SubmissionMetaData
  ): NodeSeq =
    <CBCSubmissionInboundRequest xmlns:cbc="urn:oecd:ties:cbc:v2"
                                    xmlns:eis="http://www.hmrc.gov.uk/cbc/eis"
                                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                    xsi:schemaLocation="http://www.hmrc.gov.uk/cbc/eis/CBC_EIS_UK_schema.xsd">
      <requestCommon>
        <receiptDate>{metaData.submissionTime}</receiptDate>
        <regime>CBC</regime>
        <conversationID>{metaData.conversationID.replace("govuk-tax-", "")}</conversationID>
        <schemaVersion>1.0.0</schemaVersion>
      </requestCommon>
      <requestDetail>
        {addNameSpaceDefinitions(submissionFile)}
      </requestDetail>
      <requestAdditionalDetail>
        {transformSubscriptionDetails(subscriptionDetails, metaData.fileName)}
      </requestAdditionalDetail>
    </CBCSubmissionInboundRequest>
}
