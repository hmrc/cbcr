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

import javax.inject.{Inject, Singleton}

import play.api.{Configuration, Logger}
import configs.syntax._

@Singleton
class RunMode @Inject() (configuration:Configuration) {
  private val APP_RUNNING_LOCALY: String = "Dev"

  val env: String = configuration.underlying.get[String]("run.mode").valueOr(_ => APP_RUNNING_LOCALY)
  Logger.info(s"Current environment run.mode: $env")
}
