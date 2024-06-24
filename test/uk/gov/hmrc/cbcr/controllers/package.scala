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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers.stubControllerComponents
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector}
import uk.gov.hmrc.cbcr.auth.CBCRAuth

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/** Created by colm on 27/09/17.
  */
package object controllers extends MockitoSugar {

  val cc = stubControllerComponents()
  val mockAuthConnector = mock[AuthConnector]
  val cBCRAuth = new CBCRAuth(mockAuthConnector, cc)
  val agentAffinity: Future[Option[AffinityGroup]] =
    Future successful Some(AffinityGroup.Agent)

  def passAuthMock() =
    when(mockAuthConnector.authorise(any(), any[Retrieval[Option[AffinityGroup]]]())(any(), any()))
      .thenReturn(agentAffinity)

}
