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

package uk.gov.hmrc.cbcr.repositories

import javax.inject.{Inject, Singleton}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.cbcr.models.UploadFileResponse
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FileUploadRepository @Inject()(val rmc: ReactiveMongoComponent)
    extends ReactiveRepository[UploadFileResponse, BSONObjectID](
      "FileUpload",
      rmc.mongoConnector.db,
      UploadFileResponse.ufrFormat,
      ReactiveMongoFormats.objectIdFormats) {

  def save2(f: UploadFileResponse)(implicit ec: ExecutionContext): Future[WriteResult] = insert(f)

  def get(envelopeId: String)(implicit ec: ExecutionContext): Future[Option[UploadFileResponse]] =
    find("envelopeId" -> envelopeId).map(_.headOption)

}
