package biz.lobachev.annette.camunda.api.repository

import play.api.libs.json.Json

/**
 * Params for query process definitions that fulfill given parameters. Parameters may be the properties of process definitions, such as the name, key or version.
 *
 * @param processDefinitionId                      Filter by process definition id.
 * @param processDefinitionIdIn                    Filter by process definition ids.
 * @param name                                     Filter by process definition name.
 * @param nameLike                                 Filter by process definition names that the parameter is a substring of.
 * @param deploymentId                             Filter by the deployment the id belongs to.
 * @param key                                      Filter by process definition key, i.e., the id in the BPMN 2.0 XML. Exact match.
 * @param keysIn                                   Filter by process definition keys.
 * @param keyLike                                  Filter by process definition keys that the parameter is a substring of.
 * @param version                                  Filter by process definition version.
 * @param latestVersion                            Only include those process definitions that are latest versions. Value may only be true, as false is the default behavior.
 * @param resourceName                             Filter by the name of the process definition resource. Exact match.
 * @param resourceNameLike                         Filter by names of those process definition resources that the parameter is a substring of.
 * @param active                                   Only include active process definitions. Value may only be true, as false is the default behavior.
 * @param suspended                                Only include suspended process definitions. Value may only be true, as false is the default behavior.
 * @param versionTag                               Filter by the version tag.
 * @param versionTagLike                           Filter by the version tag that the parameter is a substring of.
 * @param withoutVersionTag                        Only include process definitions without a versionTag
 * @param sortBy                                   Sort the results lexicographically by a given criterion. Valid values are category, key, id, name, version, deploymentId, deployTime, tenantId and versionTag. Must be used in conjunction with the sortOrder parameter. Note: Sorting by versionTag is string based. The version will not be interpreted. As an example, the sorting could return v0.1.0, v0.10.0, v0.2.0.
 * @param sortOrder                                Sort the results in a given order. Values may be asc for ascending order or desc for descending order. Must be used in conjunction with the sortBy parameter.
 * @param firstResult                              Pagination of results. Specifies the index of the first result to return.
 * @param maxResults                               Pagination of results. Specifies the maximum number of results to return. Will return less results if there are no more results left.
 */
case class ProcessDefinitionFindQuery(
  processDefinitionId: Option[String] = None,        //	Filter by process definition id.
  processDefinitionIdIn: Option[Seq[String]] = None, //	Filter by process definition ids.
  name: Option[String] = None,                       //	Filter by process definition name.
  nameLike: Option[String] = None,                   //	Filter by process definition names that the parameter is a substring of.
  deploymentId: Option[String] = None,               // Filter by the deployment the id belongs to.
  key: Option[String] = None,                        //	Filter by process definition key, i.e., the id in the BPMN 2.0 XML. Exact match.
  keysIn: Option[Seq[String]] = None,                //	Filter by process definition keys.
  keyLike: Option[String] = None,                    //	Filter by process definition keys that the parameter is a substring of.
  version: Option[String] = None,                    //	Filter by process definition version.
  //	Only include those process definitions that are latest versions. Value may only be true, as false is the default behavior.
  latestVersion: Option[Boolean] = None,
  resourceName: Option[String] = None,               //	Filter by the name of the process definition resource. Exact match.
  //	Filter by names of those process definition resources that the parameter is a substring of.
  resourceNameLike: Option[String] = None,
  //	Only include active process definitions. Value may only be true, as false is the default behavior.
  active: Option[Boolean] = None,
  //	Only include suspended process definitions. Value may only be true, as false is the default behavior.
  suspended: Option[Boolean] = None,
  versionTag: Option[String] = None,                 //	Filter by the version tag.
  versionTagLike: Option[String] = None,             //	Filter by the version tag that the parameter is a substring of.
  withoutVersionTag: Option[String] = None,          //	Only include process definitions without a versionTag

  //	Sort the results lexicographically by a given criterion. Valid values are category, key, id, name, version, deploymentId, deployTime, tenantId and versionTag. Must be used in conjunction with the sortOrder parameter. Note: Sorting by versionTag is string based. The version will not be interpreted. As an example, the sorting could return v0.1.0, v0.10.0, v0.2.0.
  sortBy: Option[String] = None,
  //	Sort the results in a given order. Values may be asc for ascending order or desc for descending order. Must be used in conjunction with the sortBy parameter.
  sortOrder: Option[String] = None,
  //	Pagination of results. Specifies the index of the first result to return.
  firstResult: Option[Int] = None,
  //	Pagination of results. Specifies the maximum number of results to return. Will return less results if there are no more results left.
  maxResults: Option[Int] = None
)

object ProcessDefinitionFindQuery {
  implicit val format = Json.format[ProcessDefinitionFindQuery]
}
