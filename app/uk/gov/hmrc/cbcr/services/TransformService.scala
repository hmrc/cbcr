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

package uk.gov.hmrc.cbcr.services

import uk.gov.hmrc.cbcr.models.NamespaceForNode

import javax.inject.Inject
import scala.xml.{Elem, Node, NodeSeq}

class TransformService @Inject()() {

  def addNameSpaces(file: NodeSeq, namespaces: Seq[NamespaceForNode]): NodeSeq = {

    def changeNS(el: NodeSeq): NodeSeq = {
      def fixSeq(ns: Seq[Node], currentPrefix: Option[String]): Seq[Node] =
        for (node <- ns)
          yield
            node match {
              case elem: Elem =>
                namespaces
                  .find(
                    n => n.nodeName == elem.label
                  )
                  .map { n =>
                    elem.copy(
                      prefix = n.prefix,
                      child = fixSeq(elem.child, Some(n.prefix))
                    )
                  }
                  .getOrElse(
                    elem.copy(
                      prefix = currentPrefix.get,
                      child = fixSeq(elem.child, Some(currentPrefix.get))
                    )
                  )
              case other => other
            }

      fixSeq(el, None).head
    }

    changeNS(file)
  }

}
