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

import com.kenshoo.play.metrics.{MetricsController, MetricsImpl}
import play.api.ApplicationLoader.Context
import play.api._
import play.api.http._
import play.api.i18n.I18nComponents
import play.api.inject.{Injector, SimpleInjector}
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.{RequestHeader, Result}
import play.api.routing.Router
import play.core.SourceMapper
import play.modules.reactivemongo.ReactiveMongoComponentImpl
import reactivemongo.api.DefaultDB
import uk.gov.hmrc.cbcr.controllers.{FileUploadResponseController, SubscriptionDataController}
import uk.gov.hmrc.cbcr.models.SubscriptionData
import uk.gov.hmrc.cbcr.repositories.{FileUploadResponseRepository, SubscriptionRepository}
import uk.gov.hmrc.play.health.AdminController
import uk.gov.hmrc.play.microservice.bootstrap.JsonErrorHandling

import scala.concurrent.Future

class CBCRApplicationLoader extends ApplicationLoader {
  override def load(context: Context) = {
    LoggerConfigurator(context.environment.classLoader).foreach { _.configure(context.environment) }

    new MyComponents(context).application
  }
}

class MyComponents(context: Context) extends BuiltInComponentsFromContext(context)  with AhcWSComponents
with I18nComponents { self =>
  //lazy val router = Router.empty


  override lazy val httpErrorHandler: HttpErrorHandler = new CustomErrorHandling(environment, configuration, sourceMapper, Some(router))
  lazy val metrics = new MetricsImpl(applicationLifecycle, configuration)

  lazy val appRoutes = new app.Routes(httpErrorHandler, saveAndRetrieveController, subscriptionDataController, "cbcr")

  lazy val router = new prod.Routes(httpErrorHandler, appRoutes, healthRoutes, metricsController, "/")

 // lazy val applicationController = new controllers.SaveAndRetrieveController()

  override lazy val application: Application = new DefaultApplication(environment, applicationLifecycle, customInjector,
    configuration, httpRequestHandler, httpErrorHandler, actorSystem, materializer)


  lazy val configurationApp = new Application() {
    def actorSystem = self.actorSystem
    def classloader = self.environment.classLoader
    def configuration = self.configuration
    def errorHandler = self.httpErrorHandler
    implicit def materializer = self.materializer
    def mode = self.environment.mode
    def path = self.environment.rootPath
    def requestHandler = self.httpRequestHandler
    def stop() = self.applicationLifecycle.stop()
  }

  lazy val reactiveMongoComponent = new ReactiveMongoComponentImpl(configurationApp, applicationLifecycle)

  lazy val db: () => DefaultDB = reactiveMongoComponent.mongoConnector.db

  lazy implicit val saveAndRetrieveRepository = FileUploadResponseRepository.store

  lazy implicit val subscriptionDataRepository = SubscriptionRepository.store

  lazy val subscriptionDataController = new SubscriptionDataController()(subscriptionDataRepository)

  lazy val saveAndRetrieveController = new FileUploadResponseController()(saveAndRetrieveRepository)

  lazy val healthRoutes: health.Routes = health.Routes

  lazy val adminController = new AdminController(configuration)

  // Since core libraries are using deprecated play.api.libs.ws.WS we need to add wsApi into injector
  lazy val customInjector: Injector = new SimpleInjector(injector) + adminController + wsApi

  lazy val metricsController = new MetricsController(metrics)

}


class CustomErrorHandling(
                           environment: Environment,
                           configuration: Configuration,
                           sourceMapper: Option[SourceMapper] = None,
                           router: => Option[Router] = None
                         ) extends DefaultHttpErrorHandler(environment, configuration, sourceMapper, router) with JsonErrorHandling  {
  override def onBadRequest(request: RequestHeader, error: String): Future[Result] = {
    super.onBadRequest(request, error)
  }

  override def onNotFound(request: RequestHeader, message: String): Future[Result] = {
    super.onHandlerNotFound(request)
  }

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    super.onError(request, exception)
  }
}
