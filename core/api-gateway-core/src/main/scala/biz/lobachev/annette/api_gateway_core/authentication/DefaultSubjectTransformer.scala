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

import biz.lobachev.annette.core.model.auth.{PersonPrincipal, PrincipalGroupPrincipal}
import biz.lobachev.annette.org_structure.api.OrgStructureService
import biz.lobachev.annette.principal_group.api.PrincipalGroupService
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future}

// TODO: move to appropriate subproject
class DefaultSubjectTransformer(
  orgStructureService: OrgStructureService,
  principalGroupService: PrincipalGroupService,
  config: Config,
  implicit val executionContext: ExecutionContext
) extends SubjectTransformer {
  val enableOrgStructure    = config.getBoolean("annette.authorization.enable-org-structure")
  val enablePrincipalGroups = config.getBoolean("annette.authorization.enable-principal-groups")

  override def transform(subject: Subject): Future[Subject] =
    subject.principals.headOption.flatMap {
      case PersonPrincipal(personId) => Some(personId)
      case _                         => None
    }.map { personId =>
      for {
        orgStructurePrincipals <- if (enableOrgStructure)
                                    orgStructureService.getPersonPrincipals(personId)
                                  else
                                    Future.successful(Set.empty)
        groupPrincipals        <- if (enablePrincipalGroups)
                                    principalGroupService
                                      .getPrincipalAssignments(subject.principals.toSet ++ orgStructurePrincipals)
                                      .map(_.map(groupId => PrincipalGroupPrincipal(groupId)))
                                  else
                                    Future.successful(Set.empty)

      } yield subject.copy(
        principals = subject.principals ++ orgStructurePrincipals.toSeq ++ groupPrincipals.toSeq
      )

    }.getOrElse(Future.successful(subject))
}
