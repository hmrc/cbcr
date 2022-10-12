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

import play.api.Logger
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.DefaultWriteResult
import reactivemongo.api.indexes.{CollectionIndexesManager, Index}
import reactivemongo.play.json.collection.JSONCollection

import scala.util.{Failure, Success}
import reactivemongo.api.indexes.IndexType.Ascending

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

abstract class IndexBuilder(implicit ec: ExecutionContext) {

  lazy val logger: Logger = Logger(this.getClass)

  protected val mongo: ReactiveMongoApi
  protected val collectionName: String

  protected val cbcIndexes: List[CbcIndex]

  private val indexManager: Future[CollectionIndexesManager] =
    mongo.database.map(_.collection[JSONCollection](collectionName).indexesManager)

  indexManager
    .map(manager =>
      manager.list().foreach { mongoIndexes =>
        Future
          .sequence(cbcIndexes.map { cbcIndex =>
            if (!mongoIndexes.exists(_.name.contains(cbcIndex.name))) {
              createUniqueIndex(manager, cbcIndex.id, cbcIndex.name)
            } else {
              Future.successful(())
            }
          })
          .onComplete {
            case Success(result) =>
              logger.warn(s"Indexes exist or created. Result: $result")
            case Failure(t) =>
              logger.error("Failed to create Indexes", t)
              throw t
          }
    })
    .recover {
      case NonFatal(e) => logger.error(s"Unable to create Index: ${e.getMessage}", e)
    }

  private def createUniqueIndex(manager: CollectionIndexesManager, fieldName: String, indexName: String): Future[Unit] =
    manager.create(Index(Seq(fieldName -> Ascending), Some(indexName), unique = true)).map {
      case d: DefaultWriteResult if !d.ok => throw new RuntimeException(s"${d.errmsg}")
      case _                              => ()
    }
}

case class CbcIndex(name: String, id: String)
