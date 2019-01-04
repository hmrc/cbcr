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

package uk.gov.hmrc.cbcr.services

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.Configuration
import uk.gov.hmrc.play.test.UnitSpec

class RunModeSpec extends UnitSpec with MockitoSugar with OneAppPerSuite {
  val config = app.injector.instanceOf[Configuration]

  "attempt to retrieve the runMode env" when {
    "env explicitly set in app config" in {
      val runMode = new RunMode(config ++ Configuration("run.mode" -> "Prod"))
      runMode.env shouldBe("Prod")
    }

    "env not set in app config" in {
      val runMode = new RunMode(config)
      runMode.env shouldBe("Dev")
    }

  }
}

