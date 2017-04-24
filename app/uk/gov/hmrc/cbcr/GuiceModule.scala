/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.cbcr

import java.net.URL

import com.google.inject.name.Names.named
import com.google.inject.{AbstractModule, Provider}
import play.api.Mode.Mode
import play.api.{Configuration, Environment}
import uk.gov.hmrc.cbcr.connectors.DESConnector
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HttpGet, HttpPost}

class GuiceModule(environment: Environment, configuration: Configuration) extends AbstractModule with ServicesConfig {

  override protected lazy val mode: Mode = environment.mode
  override protected lazy val runModeConfiguration: Configuration = configuration

  override def configure(): Unit = {
    bind(classOf[HttpGet]).toInstance(WSHttp)
    bind(classOf[HttpPost]).toInstance(WSHttp)
    bind(classOf[HttpGet]).toInstance(WSHttp)
    bind(classOf[DESConnector])
    bind(classOf[AuditConnector]).toInstance(MicroserviceGlobal.auditConnector)
  }

 private def bindBaseUrl(serviceName: String) =
    bind(classOf[URL]).annotatedWith(named(s"$serviceName-baseUrl")).toProvider(new BaseUrlProvider(serviceName))

  private class BaseUrlProvider(serviceName: String) extends Provider[URL] {
    override lazy val get = new URL(baseUrl(serviceName))
  }

  private def bindConfigProperty(propertyName: String) =
    bind(classOf[String]).annotatedWith(named(s"$propertyName")).toProvider(new ConfigPropertyProvider(propertyName))

  private class ConfigPropertyProvider(propertyName: String) extends Provider[String] {
    override lazy val get = getConfString(propertyName, throw new RuntimeException(s"No configuration value found for '$propertyName'"))
  }

}
