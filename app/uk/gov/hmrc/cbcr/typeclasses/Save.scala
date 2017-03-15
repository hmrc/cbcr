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

package uk.gov.hmrc.cbcr.typeclasses

import play.api.libs.json._
import uk.gov.hmrc.cbcr.core.Opt
import uk.gov.hmrc.cbcr.models.{DbOperationResult, SaveAndRetrieve}
import uk.gov.hmrc.cbcr.repositories.SaveAndRetrieveRepository

import scala.concurrent.{ExecutionContext, Future}

trait Save[T] {
  def apply(e: T)(implicit ex: ExecutionContext): Future[Opt[DbOperationResult]]
}

object Save {

  implicit def saveEntity(implicit repo: SaveAndRetrieveRepository) = new Save[SaveAndRetrieve] {
    def apply(entity: SaveAndRetrieve)(implicit ex: ExecutionContext): Future[Opt[DbOperationResult]] = {
      repo.save(entity)
    }
  }

}
