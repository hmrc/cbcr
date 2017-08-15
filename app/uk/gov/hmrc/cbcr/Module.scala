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

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit.{MILLISECONDS, SECONDS}

import com.codahale.metrics.graphite.{Graphite, GraphiteReporter}
import com.codahale.metrics.{MetricFilter, SharedMetricRegistries}
import com.google.inject.AbstractModule
import org.slf4j.MDC
import play.api.{Configuration, Environment, Logger}

class Module(environment: Environment, configuration: Configuration) extends AbstractModule {

  val metricsPluginEnabled: Boolean = configuration.getBoolean("metrics.enabled").getOrElse(false)

  val graphitePublisherEnabled: Boolean = configuration.getBoolean("microservice.metrics.graphite.enabled").getOrElse(false)

  val graphiteEnabled: Boolean = metricsPluginEnabled && graphitePublisherEnabled

  val registryName: String = configuration.getString("metrics.name").getOrElse("default")

  private def startGraphite(): Unit = {
    Logger.info("Graphite metrics enabled, starting the reporter")

    val graphite = new Graphite(new InetSocketAddress(
      configuration.getString("graphite.host").getOrElse("graphite"),
      configuration.getInt("graphite.port").getOrElse(2003)))

    val prefix = configuration.getString("graphite.prefix").getOrElse(s"tax.${configuration.getString("appName")}")

    val reporter = GraphiteReporter.forRegistry(
      SharedMetricRegistries.getOrCreate(registryName))
      .prefixedWith(s"$prefix.${java.net.InetAddress.getLocalHost.getHostName}")
      .convertRatesTo(SECONDS)
      .convertDurationsTo(MILLISECONDS)
      .filter(MetricFilter.ALL)
      .build(graphite)

    reporter.start(configuration.getLong("graphite.interval").getOrElse(10L), SECONDS)
  }

  def configure(): Unit = {
    lazy val appName = configuration.getString("appName").get
    lazy val loggerDateFormat: Option[String] = configuration.getString("logger.json.dateformat")

    if (graphiteEnabled) startGraphite

    Logger.info(s"Starting microservice : $appName : in mode : ${environment.mode}")
    MDC.put("appName", appName)
    loggerDateFormat.foreach(str => MDC.put("logger.json.dateformat", str))
  }
}
