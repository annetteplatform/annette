package biz.lobachev.annette.camunda4s.impl.repository

/**
 * Params for query process definitions that fulfill given parameters. Parameters may be the properties of process definitions, such as the name, key or version.
 *
 * @param processDefinitionId                      Filter by process definition id.
 * @param processDefinitionIdIn                    Filter by process definition ids.
 * @param name                                     Filter by process definition name.
 * @param nameLike                                 Filter by process definition names that the parameter is a substring of.
 * @param deploymentId                             Filter by the deployment the id belongs to.
 * @param deployedAfter                            Filter by the deploy time of the deployment the process definition belongs to. Only selects process definitions that have been deployed after (exclusive) a specific time.
 * @param deployedAt                               Filter by the deploy time of the deployment the process definition belongs to. Only selects process definitions that have been deployed at a specific time (exact match).
 * @param key                                      Filter by process definition key, i.e., the id in the BPMN 2.0 XML. Exact match.
 * @param keysIn                                   Filter by process definition keys.
 * @param keyLike                                  Filter by process definition keys that the parameter is a substring of.
 * @param category                                 Filter by process definition category. Exact match.
 * @param categoryLike                             Filter by process definition categories that the parameter is a substring of.
 * @param version                                  Filter by process definition version.
 * @param latestVersion                            Only include those process definitions that are latest versions. Value may only be true, as false is the default behavior.
 * @param resourceName                             Filter by the name of the process definition resource. Exact match.
 * @param resourceNameLike                         Filter by names of those process definition resources that the parameter is a substring of.
 * @param startableBy                              Filter by a user name who is allowed to start the process.
 * @param active                                   Only include active process definitions. Value may only be true, as false is the default behavior.
 * @param suspended                                Only include suspended process definitions. Value may only be true, as false is the default behavior.
 * @param incidentId                               Filter by the incident id.
 * @param incidentType                             Filter by the incident type. See the User Guide for a list of incident types.
 * @param incidentMessage                          Filter by the incident message. Exact match.
 * @param incidentMessageLike                      Filter by the incident message that the parameter is a substring of.
 * @param tenantIdIn                               Filter by a comma-separated list of tenant ids. A process definition must have one of the given tenant ids.
 * @param withoutTenantId                          Only include process definitions which belong to no tenant. Value may only be true, as false is the default behavior.
 * @param includeProcessDefinitionsWithoutTenantId Include process definitions which belong to no tenant. Can be used in combination with tenantIdIn. Value may only be true, as false is the default behavior.
 * @param versionTag                               Filter by the version tag.
 * @param versionTagLike                           Filter by the version tag that the parameter is a substring of.
 * @param withoutVersionTag                        Only include process definitions without a versionTag
 * @param startableInTasklist                      Filter by process definitions which are startable in Tasklist.
 * @param notStartableInTasklist                   Filter by process definitions which are not startable in Tasklist.
 * @param startablePermissionCheck                 Filter by process definitions which the user is allowed to start in Tasklist. If the user doesn't have these permissions the result will be empty list.
 *                                                 The permissions are:
 *                                                 * CREATE permission for all Process instances
 *                                                 * CREATE_INSTANCE and READ permission on Process definition level
 * @param sortBy                                   Sort the results lexicographically by a given criterion. Valid values are category, key, id, name, version, deploymentId, deployTime, tenantId and versionTag. Must be used in conjunction with the sortOrder parameter. Note: Sorting by versionTag is string based. The version will not be interpreted. As an example, the sorting could return v0.1.0, v0.10.0, v0.2.0.
 * @param sortOrder                                Sort the results in a given order. Values may be asc for ascending order or desc for descending order. Must be used in conjunction with the sortBy parameter.
 * @param firstResult                              Pagination of results. Specifies the index of the first result to return.
 * @param maxResults                               Pagination of results. Specifies the maximum number of results to return. Will return less results if there are no more results left.
 */
case class GetProcessDefinitionsRequest(
  processDefinitionId: Option[String] = None,        //	Filter by process definition id.
  processDefinitionIdIn: Option[Seq[String]] = None, //	Filter by process definition ids.
  name: Option[String] = None,                       //	Filter by process definition name.
  nameLike: Option[String] = None,                   //	Filter by process definition names that the parameter is a substring of.
  deploymentId: Option[String] = None,               // Filter by the deployment the id belongs to.
  // Filter by the deploy time of the deployment the process definition belongs to. Only selects process definitions that have been deployed after (exclusive) a specific time.
  deployedAfter: Option[String] = None,
  // Filter by the deploy time of the deployment the process definition belongs to. Only selects process definitions that have been deployed at a specific time (exact match).
  deployedAt: Option[String] = None,
  key: Option[String] = None,                        //	Filter by process definition key, i.e., the id in the BPMN 2.0 XML. Exact match.
  keysIn: Option[Seq[String]] = None,                //	Filter by process definition keys.
  keyLike: Option[String] = None,                    //	Filter by process definition keys that the parameter is a substring of.
  category: Option[String] = None,                   //	Filter by process definition category. Exact match.
  categoryLike: Option[String] = None,               //	Filter by process definition categories that the parameter is a substring of.
  version: Option[String] = None,                    //	Filter by process definition version.
  //	Only include those process definitions that are latest versions. Value may only be true, as false is the default behavior.
  latestVersion: Option[String] = None,
  resourceName: Option[String] = None,               //	Filter by the name of the process definition resource. Exact match.
  //	Filter by names of those process definition resources that the parameter is a substring of.
  resourceNameLike: Option[String] = None,
  startableBy: Option[String] = None,                //	Filter by a user name who is allowed to start the process.
  //	Only include active process definitions. Value may only be true, as false is the default behavior.
  active: Option[Boolean] = None,
  //	Only include suspended process definitions. Value may only be true, as false is the default behavior.
  suspended: Option[Boolean] = None,
  incidentId: Option[String] = None,                 //	Filter by the incident id.
  incidentType: Option[String] = None,               //	Filter by the incident type. See the User Guide for a list of incident types.
  incidentMessage: Option[String] = None,            // Filter by the incident message. Exact match.
  incidentMessageLike: Option[String] = None,        //	Filter by the incident message that the parameter is a substring of.
  //	Filter by a comma-separated list of tenant ids. A process definition must have one of the given tenant ids.
  tenantIdIn: Option[String] = None,
  //	Only include process definitions which belong to no tenant. Value may only be true, as false is the default behavior.
  withoutTenantId: Option[String] = None,
  //	Include process definitions which belong to no tenant. Can be used in combination with tenantIdIn. Value may only be true, as false is the default behavior.
  includeProcessDefinitionsWithoutTenantId: Option[String] = None,
  versionTag: Option[String] = None,                 //	Filter by the version tag.
  versionTagLike: Option[String] = None,             //	Filter by the version tag that the parameter is a substring of.
  withoutVersionTag: Option[String] = None,          //	Only include process definitions without a versionTag
  startableInTasklist: Option[String] = None,        //	Filter by process definitions which are startable in Tasklist.
  notStartableInTasklist: Option[String] = None,     //	Filter by process definitions which are not startable in Tasklist.
  //	Filter by process definitions which the user is allowed to start in Tasklist. If the user doesn't have these permissions the result will be empty list.
  // The permissions are:
  //  * CREATE permission for all Process instances
  //  * CREATE_INSTANCE and READ permission on Process definition level
  startablePermissionCheck: Option[String] = None,
  //	Sort the results lexicographically by a given criterion. Valid values are category, key, id, name, version, deploymentId, deployTime, tenantId and versionTag. Must be used in conjunction with the sortOrder parameter. Note: Sorting by versionTag is string based. The version will not be interpreted. As an example, the sorting could return v0.1.0, v0.10.0, v0.2.0.
  sortBy: Option[String] = None,
  //	Sort the results in a given order. Values may be asc for ascending order or desc for descending order. Must be used in conjunction with the sortBy parameter.
  sortOrder: Option[String] = None,
  //	Pagination of results. Specifies the index of the first result to return.
  firstResult: Option[Int] = None,
  //	Pagination of results. Specifies the maximum number of results to return. Will return less results if there are no more results left.
  maxResults: Option[Int] = None
)
