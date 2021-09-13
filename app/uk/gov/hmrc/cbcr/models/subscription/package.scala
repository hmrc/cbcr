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

package uk.gov.hmrc.cbcr.models

import play.api.libs.json._
import uk.gov.hmrc.cbcr.models.subscription.request.CreateRequestDetail

package object subscription {

  sealed trait ContactInformation

  case class OrganisationDetails(organisationName: String)

  object OrganisationDetails {
    implicit val format: OFormat[OrganisationDetails] = Json.format[OrganisationDetails]
  }

  case class ContactInformationForOrganisation(
    organisation: OrganisationDetails,
    email: String,
    phone: Option[String],
    mobile: Option[String])
      extends ContactInformation

  object ContactInformationForOrganisation {
    implicit val format: OFormat[ContactInformationForOrganisation] = Json.format[ContactInformationForOrganisation]
  }

  case class PrimaryContact(contactInformation: ContactInformation)

  private def convertContactInformation(contactInformation: ContactInformation): ContactInformation =
    contactInformation match {
      case c: ContactInformationForOrganisation =>
        c.copy(organisation = OrganisationDetails(c.organisation.organisationName))
      case _ => throw new IllegalArgumentException("An organisation is required")
    }

  object PrimaryContact {

    def apply(requestDetail: CreateRequestDetail): PrimaryContact =
      PrimaryContact(convertContactInformation(requestDetail.primaryContact.contactInformation))

    implicit lazy val reads: Reads[PrimaryContact] = {
      import play.api.libs.functional.syntax._
      (
        (__ \\ "organisation").readNullable[OrganisationDetails] and
          (__ \\ "email").read[String] and
          (__ \\ "phone").readNullable[String] and
          (__ \\ "mobile").readNullable[String]
      )(
        (organisation, email, phone, mobile) =>
          organisation match {
            case Some(o) => PrimaryContact(ContactInformationForOrganisation(o, email, phone, mobile))
            case _       => throw new Exception("Primary Contact must have an organisation element")
        }
      )
    }

    //API accepts one item for contact information
    implicit lazy val writes: OWrites[PrimaryContact] = OWrites[PrimaryContact] {
      case PrimaryContact(contactInformationForOrg @ ContactInformationForOrganisation(_, _, _, _)) =>
        Json.toJsObject(contactInformationForOrg)
    }
  }

  case class SecondaryContact(contactInformation: ContactInformation)

  object SecondaryContact {

    def apply(secondaryContact: SecondaryContact): SecondaryContact =
      SecondaryContact(convertContactInformation(secondaryContact.contactInformation))

    implicit lazy val reads: Reads[SecondaryContact] = {
      import play.api.libs.functional.syntax._
      (
        (__ \\ "organisation").readNullable[OrganisationDetails] and
          (__ \\ "email").read[String] and
          (__ \\ "phone").readNullable[String] and
          (__ \\ "mobile").readNullable[String]
      )(
        (organisation, email, phone, mobile) =>
          organisation match {
            case Some(o) =>
              SecondaryContact(ContactInformationForOrganisation(o, email, phone, mobile))
            case _ => throw new Exception("Secondary Contact must have an organisation element")
        }
      )
    }

    //API accepts one item for contact information
    implicit lazy val writes: OWrites[SecondaryContact] = {
      case SecondaryContact(contactInformationForOrg @ ContactInformationForOrganisation(_, _, _, _)) =>
        Json.toJsObject(contactInformationForOrg)
    }
  }

}
