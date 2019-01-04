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

package uk.gov.hmrc

import cats.data.{EitherT, OptionT}
import uk.gov.hmrc.cbcr.models.InvalidState
import _root_.play.api.libs.json._
import _root_.play.api.libs.json.Json._
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.microservice.config.LoadAuditingConfig

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector => Auditing}

object AuditConnector extends Auditing with AppName {
  override lazy val auditingConfig = LoadAuditingConfig(s"auditing")
}

package object cbcr {


  type ServiceResponse[A] = EitherT[Future, InvalidState, A]
  type ServiceResponseOpt[A] = OptionT[Future, A]
  
  type UnexpectedOr[A] = Either[InvalidState, A]

  def fromFutureOptA[A](fa: Future[UnexpectedOr[A]]): ServiceResponse[A] = {
    EitherT[Future, InvalidState, A](fa)
  }

  def fromFutureA[A](fa: Future[A])(implicit ec: ExecutionContext): ServiceResponse[A] = {
    EitherT[Future, InvalidState, A](fa.map(Right(_)))
  }

  def fromOptA[A](oa: UnexpectedOr[A])(implicit ec: ExecutionContext): ServiceResponse[A] = {
    EitherT[Future, InvalidState, A](Future.successful(oa))
  }

  def fromFutureOptionA[A](fo: Future[Option[A]])(invalid: => InvalidState)(implicit ec: ExecutionContext): ServiceResponse[A] = {
    val futureA = fo.map {
      case Some(a) => Right(a)
      case None => Left(invalid)
    }
    EitherT[Future, InvalidState, A](futureA)
  }

  implicit def listTupleJsWrapper[T:Writes](l:List[(String,T)]) : List[(String,JsValueWrapper)] = l.map(t => t._1 -> toJsFieldJsValueWrapper(t._2))
  implicit def listJsWrapper[T:Writes](l:List[T]) : List[JsValueWrapper] = l.map(toJsFieldJsValueWrapper[T])

}
