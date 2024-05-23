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

package biz.lobachev.annette.cms.impl.blogs.blog.dao

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import biz.lobachev.annette.cms.api.blogs.blog._
import biz.lobachev.annette.cms.impl.blogs.blog.BlogEntity
import biz.lobachev.annette.core.model.auth.AnnettePrincipal
import biz.lobachev.annette.microservice_core.db.{CassandraQuillDao, CassandraTableBuilder}
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import biz.lobachev.annette.core.utils.ChimneyCommons._
import io.scalaland.chimney.dsl._

import scala.collection.immutable.{Seq, _}
import scala.concurrent.{ExecutionContext, Future}

private[impl] class BlogDbDao(
  override val session: CassandraSession
)(implicit
  val ec: ExecutionContext,
  val materializer: Materializer
) extends CassandraQuillDao {

  import ctx._

  private val blogSchema       = quote(querySchema[BlogRecord]("blogs"))
  private val blogTargetSchema = quote(querySchema[BlogTargetRecord]("blog_targets"))
  private val blogAuthorSchema = quote(querySchema[BlogAuthorRecord]("blog_authors"))

  private implicit val insertBlogMeta       = insertMeta[BlogRecord]()
  private implicit val updateBlogMeta       = updateMeta[BlogRecord](_.id)
  private implicit val insertBlogTargetMeta = insertMeta[BlogTargetRecord]()
  private implicit val insertBlogAuthorMeta = insertMeta[BlogAuthorRecord]()
  touch(insertBlogMeta)
  touch(updateBlogMeta)
  touch(insertBlogTargetMeta)
  touch(insertBlogAuthorMeta)

  def createTables(): Future[Done] = {
    import CassandraTableBuilder.types._
    for {
      _ <- session.executeCreateTable(
             CassandraTableBuilder("blogs")
               .column("id", Text, true)
               .column("name", Text)
               .column("description", Text)
               .column("category_id", Text)
               .column("active", Boolean)
               .column("updated_at", Timestamp)
               .column("updated_by", Text)
               .build
           )
      _ <- session.executeCreateTable(
             CassandraTableBuilder("blog_targets")
               .column("blog_id", Text)
               .column("principal", Text)
               .withPrimaryKey("blog_id", "principal")
               .build
           )
      _ <- session.executeCreateTable(
             CassandraTableBuilder("blog_authors")
               .column("blog_id", Text)
               .column("principal", Text)
               .withPrimaryKey("blog_id", "principal")
               .build
           )
    } yield Done
  }

  def createBlog(event: BlogEntity.BlogCreated) = {
    val blogRecord = event
      .into[BlogRecord]
      .withFieldConst(_.active, true)
      .withFieldComputed(_.updatedAt, _.createdAt)
      .withFieldComputed(_.updatedBy, _.createdBy)
      .transform
    for {
      _ <- ctx.run(blogSchema.insert(lift(blogRecord)))
      _ <- Source(event.authors)
             .mapAsync(1) { author =>
               val blogAuthorRecord = BlogAuthorRecord(
                 blogId = event.id,
                 principal = author
               )
               ctx.run(blogAuthorSchema.insert(lift(blogAuthorRecord)))
             }
             .runWith(Sink.ignore)
      _ <- Source(event.targets)
             .mapAsync(1) { target =>
               val blogTargetRecord = BlogTargetRecord(
                 blogId = event.id,
                 principal = target
               )
               ctx.run(blogTargetSchema.insert(lift(blogTargetRecord)))
             }
             .runWith(Sink.ignore)
    } yield Done
  }

  def updateBlogName(event: BlogEntity.BlogNameUpdated) =
    ctx.run(
      blogSchema
        .filter(_.id == lift(event.id))
        .update(
          _.name      -> lift(event.name),
          _.updatedAt -> lift(event.updatedAt),
          _.updatedBy -> lift(event.updatedBy)
        )
    )

  def updateBlogDescription(event: BlogEntity.BlogDescriptionUpdated) =
    ctx.run(
      blogSchema
        .filter(_.id == lift(event.id))
        .update(
          _.description -> lift(event.description),
          _.updatedAt   -> lift(event.updatedAt),
          _.updatedBy   -> lift(event.updatedBy)
        )
    )

  def updateBlogCategory(event: BlogEntity.BlogCategoryUpdated) =
    ctx.run(
      blogSchema
        .filter(_.id == lift(event.id))
        .update(
          _.categoryId -> lift(event.categoryId),
          _.updatedAt  -> lift(event.updatedAt),
          _.updatedBy  -> lift(event.updatedBy)
        )
    )

  def assignBlogAuthorPrincipal(event: BlogEntity.BlogAuthorPrincipalAssigned) =
    for {
      _ <- ctx.run(
             blogAuthorSchema.insert(
               lift(
                 BlogAuthorRecord(
                   event.id,
                   event.principal
                 )
               )
             )
           )
      _ <- ctx.run(
             blogSchema
               .filter(_.id == lift(event.id))
               .update(
                 _.updatedAt -> lift(event.updatedAt),
                 _.updatedBy -> lift(event.updatedBy)
               )
           )
    } yield Done

  def unassignBlogAuthorPrincipal(event: BlogEntity.BlogAuthorPrincipalUnassigned) =
    for {
      _ <- ctx.run(
             blogAuthorSchema
               .filter(r =>
                 r.blogId == lift(event.id) &&
                   r.principal == lift(event.principal)
               )
               .delete
           )
      _ <- ctx.run(
             blogSchema
               .filter(_.id == lift(event.id))
               .update(
                 _.updatedAt -> lift(event.updatedAt),
                 _.updatedBy -> lift(event.updatedBy)
               )
           )
    } yield Done

  def assignBlogTargetPrincipal(event: BlogEntity.BlogTargetPrincipalAssigned) =
    for {
      _ <- ctx.run(
             blogTargetSchema.insert(
               lift(
                 BlogTargetRecord(
                   event.id,
                   event.principal
                 )
               )
             )
           )
      _ <- ctx.run(
             blogSchema
               .filter(_.id == lift(event.id))
               .update(
                 _.updatedAt -> lift(event.updatedAt),
                 _.updatedBy -> lift(event.updatedBy)
               )
           )
    } yield Done

  def unassignBlogTargetPrincipal(event: BlogEntity.BlogTargetPrincipalUnassigned) =
    for {
      _ <- ctx.run(
             blogTargetSchema
               .filter(r =>
                 r.blogId == lift(event.id) &&
                   r.principal == lift(event.principal)
               )
               .delete
           )
      _ <- ctx.run(
             blogSchema
               .filter(_.id == lift(event.id))
               .update(
                 _.updatedAt -> lift(event.updatedAt),
                 _.updatedBy -> lift(event.updatedBy)
               )
           )
    } yield Done

  def activateBlog(event: BlogEntity.BlogActivated) =
    ctx.run(
      blogSchema
        .filter(_.id == lift(event.id))
        .update(
          _.active    -> lift(true),
          _.updatedAt -> lift(event.updatedAt),
          _.updatedBy -> lift(event.updatedBy)
        )
    )

  def deactivateBlog(event: BlogEntity.BlogDeactivated) =
    ctx.run(
      blogSchema
        .filter(_.id == lift(event.id))
        .update(
          _.active    -> lift(false),
          _.updatedAt -> lift(event.updatedAt),
          _.updatedBy -> lift(event.updatedBy)
        )
    )

  def deleteBlog(event: BlogEntity.BlogDeleted) =
    for {
      _ <- ctx.run(blogSchema.filter(_.id == lift(event.id)).delete)
      _ <- ctx.run(blogAuthorSchema.filter(_.blogId == lift(event.id)).delete)
      _ <- ctx.run(blogTargetSchema.filter(_.blogId == lift(event.id)).delete)
    } yield Done

  def getBlog(id: BlogId): Future[Option[Blog]] =
    for {
      maybeBlogRecord <- ctx
                           .run(blogSchema.filter(_.id == lift(id)))
                           .map(_.headOption)
      authors         <- ctx
                           .run(blogAuthorSchema.filter(_.blogId == lift(id)).map(_.principal))
      targets         <- ctx
                           .run(blogTargetSchema.filter(_.blogId == lift(id)).map(_.principal))
    } yield maybeBlogRecord.map(_.toBlog.copy(authors = authors.toSet, targets = targets.toSet))

  def getBlogs(ids: Set[BlogId]): Future[Seq[Blog]] =
    for {
      blogs   <- ctx
                   .run(blogSchema.filter(b => liftQuery(ids).contains(b.id)))
                   .map(_.map(_.toBlog))
      authors <- ctx
                   .run(blogAuthorSchema.filter(b => liftQuery(ids).contains(b.blogId)))
                   .map(_.groupMap(_.blogId)(_.principal))
      targets <- ctx
                   .run(blogTargetSchema.filter(b => liftQuery(ids).contains(b.blogId)))
                   .map(_.groupMap(_.blogId)(_.principal))
    } yield blogs
      .map(blog =>
        blog.copy(
          authors = authors.get(blog.id).map(_.toSet).getOrElse(Set.empty),
          targets = targets.get(blog.id).map(_.toSet).getOrElse(Set.empty)
        )
      )

  def getBlogViews(ids: Set[BlogId], principals: Set[AnnettePrincipal]): Future[Seq[BlogView]] =
    for {
      blogIds   <- ctx
                     .run(
                       blogTargetSchema
                         .filter(b =>
                           liftQuery(ids).contains(b.blogId) &&
                             liftQuery(principals).contains(b.principal)
                         )
                         .map(_.blogId)
                     )
                     .map(_.toSet)
      blogViews <- ctx
                     .run(blogSchema.filter(b => liftQuery(blogIds).contains(b.id)))
                     .map(_.map(_.toBlogView))
      authors   <- ctx
                     .run(blogAuthorSchema.filter(b => liftQuery(blogIds).contains(b.blogId)))
                     .map(_.groupMap(_.blogId)(_.principal))
    } yield blogViews
      .map(blog =>
        blog.copy(
          authors = authors.get(blog.id).map(_.toSet).getOrElse(Set.empty)
        )
      )

  def canEditBlogPosts(id: BlogId, principals: Set[AnnettePrincipal]): Future[Boolean] =
    for {
      maybeCount <- ctx
                      .run(
                        blogAuthorSchema
                          .filter(b =>
                            b.blogId == lift(id) &&
                              liftQuery(principals).contains(b.principal)
                          )
                          .size
                      )
    } yield maybeCount.map(_ > 0).getOrElse(false)

  def canAccessToBlog(id: BlogId, principals: Set[AnnettePrincipal]): Future[Boolean] =
    for {
      maybeCount <- ctx
                      .run(
                        blogTargetSchema
                          .filter(b =>
                            b.blogId == lift(id) &&
                              liftQuery(principals).contains(b.principal)
                          )
                          .size
                      )
    } yield maybeCount.map(_ > 0).getOrElse(false)

}
