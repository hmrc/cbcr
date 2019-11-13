/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.cbcr.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector}
import uk.gov.hmrc.cbcr.auth.CBCRAuth

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait MockAuth extends MockitoSugar {
  val mockAuthConnector = mock[AuthConnector]
  val cBCRAuth = new CBCRAuth(mockAuthConnector, cc)
  val agentAffinity: Future[Option[AffinityGroup]] =
    Future successful Some(AffinityGroup.Agent)

  private def agentAuthStub(returnValue: Future[Option[AffinityGroup]]) =
    when(mockAuthConnector.authorise(any(), any[Retrieval[Option[AffinityGroup]]]())(any(), any()))
      .thenReturn(returnValue)

  agentAuthStub(agentAffinity)
}
