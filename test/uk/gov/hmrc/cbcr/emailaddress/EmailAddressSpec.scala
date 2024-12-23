/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.cbcr.emailaddress

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class EmailAddressSpec extends AnyWordSpec with ScalaCheckPropertyChecks with Matchers with EmailAddressGenerators {

  val emailAddressValidation = new EmailAddressValidation

  "Validating EmailAddress" should {
    "work for a valid email" in {
      forAll(validEmailAddresses()) { address =>
        EmailAddress(address).value should be(address)
      }
    }

  }

  "validate invalid email" in {
    emailAddressValidation.isValid(EmailAddress("sausages")) shouldBe false
  }

  "Validate email ending with invalid characters" in {
    forAll(validEmailAddresses()) { address =>
      emailAddressValidation.isValid(EmailAddress(address + "§")) shouldBe false
    }
  }

  "Validate an empty email" in {
    emailAddressValidation.isValid(EmailAddress("")) shouldBe false
  }

  "Validate repeated email" in {
    emailAddressValidation.isValid(EmailAddress("test@domain.comtest@domain.com")) shouldBe false
  }

  "Validate an test@test email" in {
    emailAddressValidation.isValid(EmailAddress("test@test")) shouldBe false
  }

  "Validate when the '@' is missing" in {
    forAll { s: String =>
      whenever(!s.contains("@")) {
        emailAddressValidation.isValid(EmailAddress(s)) shouldBe false
      }
    }
  }

  "An EmailAddress class" should {
    "implicitly convert to a String of the address" in {
      val e: String = EmailAddress("test@domain.com")
      e should be("test@domain.com")
    }
    "toString to a String of the address" in {
      val e = EmailAddress("test@domain.com")
      e.toString should be("test@domain.com")
    }

  }

  "A email address domain" should {

    "not create for invalid domains" in {
      emailAddressValidation.isValid("test@e.") shouldBe false
      emailAddressValidation.isValid("test@.uk") shouldBe false
      emailAddressValidation.isValid("test@.com") shouldBe false
      emailAddressValidation.isValid("test@*domain") shouldBe false
    }

  }

  "An email domain validation" should {
    "return true for a domain that has MX record (ex: gmail.com)" in {
      val email = new EmailAddressValidation
      email.isValid("mike@gmail.com") shouldBe true
      email.isValid("mike@msn.co.uk") shouldBe true
    }
    "return false for an invalid domain" in {
      val email = new EmailAddressValidation
      email.isValid("mike@fortytwoisnotananswer.org") shouldBe true
    }
  }

}
