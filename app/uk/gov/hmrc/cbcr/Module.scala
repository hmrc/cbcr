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

package uk.gov.hmrc.cbcr

import com.codahale.metrics.graphite.{Graphite, GraphiteReporter}
import com.codahale.metrics.{MetricFilter, SharedMetricRegistries}
import com.google.inject.AbstractModule
import org.slf4j.MDC
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.cbcr.config.ConfigurationOpts.ConfigurationOps
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit.{MILLISECONDS, SECONDS}

class Module(environment: Environment, configuration: Configuration) extends AbstractModule {

  lazy val logger: Logger = Logger(this.getClass)

  private val graphiteConfig: Configuration = configuration.load[Configuration]("microservice.metrics.graphite")

  private val metricsPluginEnabled: Boolean = configuration.load[Boolean]("metrics.enabled")

  private val graphitePublisherEnabled: Boolean = graphiteConfig.load[Boolean]("enabled")

  private val graphiteEnabled: Boolean = metricsPluginEnabled && graphitePublisherEnabled

  private val registryName: String = configuration.load[String]("metrics.name")

  private def startGraphite(): Unit = {
    logger.info("Graphite metrics enabled, starting the reporter")

    val graphite = new Graphite(
      new InetSocketAddress(graphiteConfig.load[String]("host"), graphiteConfig.load[Int]("port"))
    )

    val prefix = graphiteConfig.load[String]("prefix")

    val reporter = GraphiteReporter
      .forRegistry(SharedMetricRegistries.getOrCreate(registryName))
      .prefixedWith(s"$prefix.${java.net.InetAddress.getLocalHost.getHostName}")
      .convertRatesTo(SECONDS)
      .convertDurationsTo(MILLISECONDS)
      .filter(MetricFilter.ALL)
      .build(graphite)

    reporter.start(graphiteConfig.load[Long]("interval"), SECONDS)
  }

  override def configure(): Unit = {
    logger.info(s"CONFIGURE RUNNING - graphiteEnabled: $graphiteEnabled")
    lazy val appName = configuration.getOptional[String]("appName").get

    if (graphiteEnabled) startGraphite()

    bind(classOf[HttpClient]).to(classOf[DefaultHttpClient])
    bind(classOf[AuthConnector]).to(classOf[DefaultAuthConnector])
    MDC.put("appName", appName)
  }
}
