/*
 * Copyright 2018 HM Revenue & Customs
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
import reactivemongo.api.indexes.{CollectionIndexesManager, Index}
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.Future
import scala.util.{Failure, Success}
import reactivemongo.api.indexes.IndexType.Ascending

import scala.concurrent.{ExecutionContext, Future}

abstract class IndexBuilder(implicit ec:ExecutionContext) {

  protected val mongo: ReactiveMongoApi
  protected val collectionName: String

  protected val cbcIndexes: List[CbcIndex]

  private val indexManager: Future[CollectionIndexesManager] = mongo.database.map(_.collection[JSONCollection](collectionName).indexesManager)

  indexManager.flatMap(manager => manager.list().flatMap { mongoIndexes =>
    cbcIndexes.map { cbcIndex =>
      if (!mongoIndexes.exists(_.name.contains(cbcIndex.name))) {
        createIndex(manager, cbcIndex.id, cbcIndex.name)
      } else {
        Future.successful(true)
      }
    }.foldRight(Future.successful(true))((result, previous) => result.flatMap(r => previous.map(p => r && p)))
  }
  ).onComplete {
    case Success(result) =>
      Logger.warn(s"Indexes exist or created. Result: $result")
    case Failure(t) =>
      Logger.error("Failed to create Indexes",t)
      throw t
  }

  private def createIndex(manager:CollectionIndexesManager, fieldName:String, indexName:String): Future[Boolean] = {
    manager.create(Index(Seq(fieldName -> Ascending), Some(indexName), unique = true)).map(_.ok)
  }

}

case class CbcIndex (name:String,id:String)