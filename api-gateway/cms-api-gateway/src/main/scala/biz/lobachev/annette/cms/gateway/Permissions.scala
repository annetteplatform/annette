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

package biz.lobachev.annette.cms.gateway

import biz.lobachev.annette.core.model.auth.Permission

object Permissions {
  final val VIEW_BLOGS = Permission("annette.cms.blog.view")

  final val VIEW_ALL_BLOG_CATEGORIES     = Permission("annette.cms.blogCategory.view.all")
  final val MAINTAIN_ALL_BLOG_CATEGORIES = Permission("annette.cms.blogCategory.maintain.all")

  final val MAINTAIN_ALL_BLOGS = Permission("annette.cms.blog.maintain.all")

  final val MAINTAIN_ALL_POSTS = Permission("annette.cms.post.maintain.all")

  final val VIEW_SPACES = Permission("annette.cms.space.view")

  final val VIEW_ALL_SPACE_CATEGORIES     = Permission("annette.cms.spaceCategory.view.all")
  final val MAINTAIN_ALL_SPACE_CATEGORIES = Permission("annette.cms.spaceCategory.maintain.all")

  final val MAINTAIN_ALL_SPACES = Permission("annette.cms.space.maintain.all")

  final val MAINTAIN_ALL_PAGES = Permission("annette.cms.page.maintain.all")

}
