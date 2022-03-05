package biz.lobachev.annette.camunda4s.api.repository

import play.api.libs.json.Json

/**
 * @param id Filter by deployment id.
 * @param name Filter by the deployment name. Exact match.
 * @param nameLike Filter by the deployment name that the parameter is a substring of. The parameter can include the wildcard % to express like-strategy such as: starts with (%name), ends with (name%) or contains (%name%).
 * @param source Filter by the deployment source.
 * @param withoutSource Filter by the deployment source whereby source is equal to null.
 * @param tenantIdIn Filter by a comma-separated list of tenant ids. A deployment must have one of the given tenant ids.
 * @param withoutTenantId Only include deployments which belong to no tenant. Value may only be true, as false is the default behavior.
 * @param includeDeploymentsWithoutTenantId Include deployments which belong to no tenant. Can be used in combination with tenantIdIn. Value may only be true, as false is the default behavior.
 * @param after Restricts to all deployments after the given date. By default*, the date must have the format yyyy-MM-dd'T'HH:mm:ss.SSSZ, e.g., 2013-01-23T14:42:45.000+0200.
 * @param before Restricts to all deployments before the given date. By default*, the date must have the format yyyy-MM-dd'T'HH:mm:ss.SSSZ, e.g., 2013-01-23T14:42:45.000+0200.
 * @param sortBy Sort the results lexicographically by a given criterion. Valid values are id, name, deploymentTime and tenantId. Must be used in conjunction with the sortOrder parameter.
 * @param sortOrder Sort the results in a given order. Values may be asc for ascending order or desc for descending order. Must be used in conjunction with the sortBy parameter.
 * @param firstResult Pagination of results. Specifies the index of the first result to return.
 * @param maxResults Pagination of results. Specifies the maximum number of results to return. Will return less results if there are no more results left.
 */
case class DeploymentFindQuery(
  id: Option[String] = None,
  name: Option[String] = None,
  nameLike: Option[String] = None,
  source: Option[String] = None,
  withoutSource: Option[String] = None,
  tenantIdIn: Option[Seq[String]] = None,
  withoutTenantId: Option[Boolean] = None,
  includeDeploymentsWithoutTenantId: Option[Boolean] = None,
  after: Option[String] = None,
  before: Option[String] = None,
  sortBy: Option[String] = None,
  sortOrder: Option[String] = None,
  firstResult: Option[Int] = None,
  maxResults: Option[Int] = None
)

object DeploymentFindQuery {
  implicit val format = Json.format[DeploymentFindQuery]
}
