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

package biz.lobachev.annette.authorization.gateway

import biz.lobachev.annette.core.model.auth.Permission

object Permissions {
  final val VIEW_AUTHORIZATION_ROLE     = Permission("annette.authorization.role.view")
  final val MAINTAIN_AUTHORIZATION_ROLE = Permission("annette.authorization.role.maintain")
  final val MAINTAIN_ROLE_PRINCIPALS    = Permission("annette.authorization.role.maintainPrincipals")
  final val VIEW_ROLE_PRINCIPALS        = Permission("annette.authorization.role.viewPrincipals")
  final val VIEW_ASSIGNMENTS            = Permission("annette.authorization.assignments.view")
}
