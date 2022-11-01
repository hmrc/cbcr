/*
 * Copyright 2022 HM Revenue & Customs
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
import org.codehaus.stax2.validation.{XMLValidationSchema, XMLValidationSchemaFactory}
import org.slf4j.MDC
import play.api.{Configuration, Environment, Logger}
import services.{MongoBackedUploadProgressTracker, UploadProgressTracker}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import java.io.File

class Module(environment: Environment, configuration: Configuration) extends AbstractModule {

  lazy val logger: Logger = Logger(this.getClass)

  val graphiteConfig: Configuration = configuration
    .getOptional[Configuration]("microservice.metrics.graphite")
    .getOrElse(throw new Exception("No configuration for microservice.metrics.graphite found"))

  val metricsPluginEnabled: Boolean = configuration.getOptional[Boolean]("metrics.enabled").getOrElse(false)

  val graphitePublisherEnabled: Boolean = graphiteConfig.getOptional[Boolean]("enabled").getOrElse(false)

  val graphiteEnabled: Boolean = metricsPluginEnabled && graphitePublisherEnabled

  val registryName: String = configuration.getOptional[String]("metrics.name").getOrElse("default")

  // TODO review property when actual DCT06_EIS_UK_SCHEMA.xsd is available
  val schemaVer: String = configuration.getOptional[String]("oecd-schema-version").getOrElse("2.0")

  private def startGraphite(): Unit = {
    logger.info("Graphite metrics enabled, starting the reporter")

    val graphite = new Graphite(
      new InetSocketAddress(
        graphiteConfig.getOptional[String]("host").getOrElse("graphite"),
        graphiteConfig.getOptional[Int]("port").getOrElse(2003)))

    val prefix = graphiteConfig.getOptional[String]("prefix").getOrElse("play.cbcr")

    val reporter = GraphiteReporter
      .forRegistry(SharedMetricRegistries.getOrCreate(registryName))
      .prefixedWith(s"$prefix.${java.net.InetAddress.getLocalHost.getHostName}")
      .convertRatesTo(SECONDS)
      .convertDurationsTo(MILLISECONDS)
      .filter(MetricFilter.ALL)
      .build(graphite)

    reporter.start(graphiteConfig.getOptional[Long]("interval").getOrElse(10L), SECONDS)
  }

  override def configure(): Unit = {
    logger.info(s"CONFIGURE RUNNING - graphiteEnabled: $graphiteEnabled")
    lazy val appName = configuration.getOptional[String]("appName").get
    lazy val loggerDateFormat: Option[String] = configuration.getOptional[String]("logger.json.dateformat")

    if (graphiteEnabled) startGraphite

    bind(classOf[XMLValidationSchema]).toInstance {
      val xmlValidationSchemaFactory: XMLValidationSchemaFactory =
        XMLValidationSchemaFactory.newInstance(XMLValidationSchema.SCHEMA_ID_W3C_SCHEMA)
      val schemaFile: File = new File(s"conf/schema/$schemaVer/CbcXML_v$schemaVer.xsd")
      xmlValidationSchemaFactory.createSchema(schemaFile)
    }
    bind(classOf[HttpClient]).to(classOf[DefaultHttpClient])
    bind(classOf[UploadProgressTracker]).to(classOf[MongoBackedUploadProgressTracker])
    bind(classOf[AuthConnector]).to(classOf[DefaultAuthConnector])
    MDC.put("appName", appName)
    loggerDateFormat.foreach(str => MDC.put("logger.json.dateformat", str))
  }
}
