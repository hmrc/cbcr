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

package uk.gov.hmrc.cbcr.config

import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}

@Singleton
class AppConfig @Inject()(
  config: Configuration,
  servicesConfig: ServicesConfig
) {
  lazy val appName: String = config.get[String]("appName")

  val authBaseUrl: String = servicesConfig.baseUrl("auth")

  lazy val bearerToken: String = config.get[String]("microservice.services.submission.bearer-token")
  lazy val eisEnvironment: String = config.get[String]("microservice.services.submission.environment")

  lazy val submissionUrl: String =
    s"${config.get[Service]("microservice.services.submission").baseUrl}${config.get[String]("microservice.services.submission.startUrl")}"

}
