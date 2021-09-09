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

package uk.gov.hmrc.cbcr.controllers.upscan

import org.slf4j.LoggerFactory
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, BaseController, MessagesControllerComponents}
import uk.gov.hmrc.cbcr.models.upscan.CallbackBody
import uk.gov.hmrc.cbcr.services.UpscanCallbackDispatcher
import uk.gov.hmrc.play.bootstrap.controller.WithJsonBody

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class UploadCallbackController @Inject()(
  val controllerComponents: MessagesControllerComponents,
  val upscanCallbackDispatcher: UpscanCallbackDispatcher,
  implicit override val messagesApi: MessagesApi)(implicit val ec: ExecutionContext)
    extends BaseController with WithJsonBody with I18nSupport {

  private val logger = LoggerFactory.getLogger(getClass)

  val callback: Action[JsValue] = Action.async(parse.json) { implicit request =>
    logger.info(s"Received callback notification [${Json.stringify(request.body)}]")
    withJsonBody[CallbackBody] { feedback: CallbackBody =>
      upscanCallbackDispatcher
        .handleCallback(feedback)
        .map(
          _ => Ok
        )
    }
  }
}