/*
 * Copyright 2013 Valery Lobachev
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

package biz.lobachev.annette.api_gateway_core.authentication

import biz.lobachev.annette.core.model.auth.PersonPrincipal
import biz.lobachev.annette.org_structure.api.OrgStructureService

import scala.concurrent.{ExecutionContext, Future}

// TODO: move to appropriate subproject
class OrgStructureSubjectTransformer(
  orgStructureService: OrgStructureService,
  implicit val executionContext: ExecutionContext
) extends SubjectTransformer {
  override def transform(subject: Subject): Future[Subject] =
    subject.principals.headOption.flatMap {
      case PersonPrincipal(personId) => Some(personId)
      case _                         => None
    }.map { personId =>
      for {
        principals <- orgStructureService.getPersonPrincipals(personId)
      } yield subject.copy(
        principals = subject.principals ++ principals.toSeq
      )
    }.getOrElse(Future.successful(subject))
}
