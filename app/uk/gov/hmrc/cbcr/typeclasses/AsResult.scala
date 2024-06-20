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

package uk.gov.hmrc.cbcr.typeclasses

import play.api.mvc.Result

/** Typeclass to present some type A as a [[play.api.mvc.Result]]
  */
trait AsResult[A] {
  def asResult: Result
}

object AsResult {

  def apply[A](implicit instance: AsResult[A]): AsResult[A] = instance

}
