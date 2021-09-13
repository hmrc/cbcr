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

///*
// * Copyright 2021 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package uk.gov.hmrc.cbcr.repositories
//
//import reactivemongo.api.bson.BSONDocument
//import reactivemongo.api.bson.collection.BSONSerializationPack
//import reactivemongo.api.indexes.{Index, IndexType}
//import reactivemongo.api.indexes.Index.Aux
//
//object IndexUtils {
//
//  def index(
//    key: Seq[(String, IndexType)],
//    name: Option[String],
//    expireAfterSeconds: Option[Int],
//    unique: Boolean = false,
//    background: Boolean = false,
//    dropDups: Boolean = false,
//    sparse: Boolean = false
//  ): Aux[BSONSerializationPack.type] =
//    Index(BSONSerializationPack)(
//      key = key,
//      name = name,
//      unique = unique,
//      background = background,
//      dropDups = dropDups,
//      sparse = sparse,
//      expireAfterSeconds = expireAfterSeconds,
//      storageEngine = None,
//      weights = None,
//      defaultLanguage = None,
//      languageOverride = None,
//      textIndexVersion = None,
//      sphereIndexVersion = None,
//      bits = None,
//      min = None,
//      max = None,
//      bucketSize = None,
//      collation = None,
//      wildcardProjection = None,
//      version = None,
//      partialFilter = None,
//      options = BSONDocument.empty
//    )
//
//}
