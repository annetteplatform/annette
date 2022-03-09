package biz.lobachev.annette.camunda.api.runtime

import play.api.libs.json.Json

/**
 * @param processInstanceIds Filter by a comma-separated list of process instance ids.
 * @param businessKey Filter by process instance business key.
 * @param businessKeyLike Filter by process instance business key that the parameter is a substring of.
 * @param caseInstanceId Filter by case instance id.
 * @param processDefinitionId Filter by the process definition the instances run on.
 * @param processDefinitionKey Filter by the key of the process definition the instances run on.
 * @param processDefinitionKeyIn Filter by a comma-separated list of process definition keys. A process instance must have one of the given process definition keys.
 * @param processDefinitionKeyNotIn Exclude instances by a comma-separated list of process definition keys. A process instance must not have one of the given process definition keys.
 * @param deploymentId Filter by the deployment the id belongs to.
 * @param active Only include active process instances. Value may only be true, as false is the default behavior.
 * @param suspended Only include suspended process instances. Value may only be true, as false is the default behavior.
 * @param activityIdIn Filter by a comma-separated list of activity ids. A process instance must currently wait in a leaf activity with one of the given activity ids.
 * @param rootProcessInstances Restrict the query to all process instances that are top level process instances.
 * @param leafProcessInstances Restrict the query to all process instances that are leaf instances. (i.e. don't have any sub instances)
 * @param variables Only include process instances that have variables with certain values. Variable filtering expressions are comma-separated and are structured as follows:
 *                  A valid parameter value has the form key_operator_value. key is the variable name, operator is the comparison operator to be used and value the variable value.
 *                  Note: Values are always treated as String objects on server side.
 *                  Valid operator values are: eq - equal to; neq - not equal to; gt - greater than; gteq - greater than or equal to; lt - lower than; lteq - lower than or equal to; like.
 *                  key and value may not contain underscore or comma characters.
 * @param variableNamesIgnoreCase Match all variable names in this query case-insensitively. If set to true variableName and variablename are treated as equal.
 * @param variableValuesIgnoreCase Match all variable values in this query case-insensitively. If set to true variableValue and variablevalue are treated as equal.
 * @param sortBy Sort the results lexicographically by a given criterion. Valid values are instanceId, definitionKey, definitionId, tenantId and businessKey. Must be used in conjunction with the sortOrder parameter.
 * @param sortOrder Sort the results in a given order. Values may be asc for ascending order or desc for descending order. Must be used in conjunction with the sortBy parameter.
 * @param firstResult Pagination of results. Specifies the index of the first result to return.
 * @param maxResults Pagination of results. Specifies the maximum number of results to return. Will return less results if there are no more results left.
 */
case class ProcessInstanceFindQuery(
  processInstanceIds: Option[Seq[String]] = None,
  businessKey: Option[String] = None,
  businessKeyLike: Option[String] = None,
  caseInstanceId: Option[String] = None,
  processDefinitionId: Option[String] = None,
  processDefinitionKey: Option[String] = None,
  processDefinitionKeyIn: Option[Seq[String]] = None,
  processDefinitionKeyNotIn: Option[Seq[String]] = None,
  deploymentId: Option[String] = None,
  active: Option[Boolean] = None,
  suspended: Option[Boolean] = None,
  activityIdIn: Option[Seq[String]] = None,
  rootProcessInstances: Option[Boolean] = None,
  leafProcessInstances: Option[Boolean] = None,
  variables: Option[Seq[String]] = None,
  variableNamesIgnoreCase: Option[Boolean] = None,
  variableValuesIgnoreCase: Option[Boolean] = None,
  sortBy: Option[String] = None,
  sortOrder: Option[String] = None,
  firstResult: Option[Int] = None,
  maxResults: Option[Int] = None
)

object ProcessInstanceFindQuery {
  implicit val format = Json.format[ProcessInstanceFindQuery]
}
