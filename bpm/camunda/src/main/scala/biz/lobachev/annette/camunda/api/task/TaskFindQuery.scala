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

package biz.lobachev.annette.camunda.api.task

import ai.x.play.json.Jsonx
import ai.x.play.json.Encoders._
import biz.lobachev.annette.camunda.api.common.VariableExpression

/**
 * @param taskId Restrict to task with the given id.
 * @param taskIdIn Restrict to tasks with any of the given ids.
 * @param processInstanceId Restrict to tasks that belong to process instances with the given id.
 * @param processInstanceIdIn Restrict to tasks that belong to process instances with the given ids.
 * @param processInstanceBusinessKey Restrict to tasks that belong to process instances with the given business key.
 * @param processInstanceBusinessKeyExpression Restrict to tasks that belong to process instances with the given business key which is described by an expression. See the user guide for more information on available functions.
 * @param processInstanceBusinessKeyIn Restrict to tasks that belong to process instances with one of the give business keys. The keys need to be in an array.
 * @param processInstanceBusinessKeyLike Restrict to tasks that have a process instance business key that has the parameter value as a substring.
 * @param processInstanceBusinessKeyLikeExpression Restrict to tasks that have a process instance business key that has the parameter value as a substring and is described by an expression. See the user guide for more information on available functions.
 * @param processDefinitionId Restrict to tasks that belong to a process definition with the given id.
 * @param processDefinitionKey Restrict to tasks that belong to a process definition with the given key.
 * @param processDefinitionKeyIn Restrict to tasks that belong to a process definition with one of the given keys. The keys need to be in a comma-separated list.
 * @param processDefinitionName Restrict to tasks that belong to a process definition with the given name.
 * @param processDefinitionNameLike Restrict to tasks that have a process definition name that has the parameter value as a substring.
 * @param executionId Restrict to tasks that belong to an execution with the given id.
 * @param caseInstanceId Restrict to tasks that belong to case instances with the given id.
 * @param caseInstanceBusinessKey Restrict to tasks that belong to case instances with the given business key.
 * @param caseInstanceBusinessKeyLike Restrict to tasks that have a case instance business key that has the parameter value as a substring.
 * @param caseDefinitionId Restrict to tasks that belong to a case definition with the given id.
 * @param caseDefinitionKey Restrict to tasks that belong to a case definition with the given key.
 * @param caseDefinitionName Restrict to tasks that belong to a case definition with the given name.
 * @param caseDefinitionNameLike Restrict to tasks that have a case definition name that has the parameter value as a substring.
 * @param caseExecutionId Restrict to tasks that belong to a case execution with the given id.
 * @param activityInstanceIdIn Only include tasks which belong to one of the passed activity instance ids.
 * @param tenantIdIn Restrict to tasks that belong to one of the given tenant ids. The ids need to be in a comma-separated list.
 * @param withoutTenantId Only include tasks which belong to no tenant. Value may only be true, as false is the default behavior.
 * @param assignee Restrict to tasks that the given user is assigned to.
 * @param assigneeExpression Restrict to tasks that the user described by the given expression is assigned to. See the user guide for more information on available functions.
 * @param assigneeLike Restrict to tasks that have an assignee that has the parameter value as a substring.
 * @param assigneeLikeExpression Restrict to tasks that have an assignee that has the parameter value described by the given expression as a substring. See the user guide for more information on available functions.
 * @param assigneeIn Only include tasks which are assigned to one of the user ids passed in the array
 * @param assigneeNotIn Only include tasks which are not assigned to one of the user ids passed in the array.
 * @param owner Restrict to tasks that the given user owns.
 * @param ownerExpression Restrict to tasks that the user described by the given expression owns. See the user guide for more information on available functions.
 * @param candidateGroup Only include tasks that are offered to the given group.
 * @param candidateGroupExpression Only include tasks that are offered to the group described by the given expression. See the user guide for more information on available functions.
 * @param withCandidateGroups Only include tasks which have a candidate group. Value may only be true, as false is the default behavior.
 * @param withoutCandidateGroups Only include tasks which have no candidate group. Value may only be true, as false is the default behavior.
 * @param withCandidateUsers Only include tasks which have a candidate user. Value may only be true, as false is the default behavior.
 * @param withoutCandidateUsers Only include tasks which have no candidate user. Value may only be true, as false is the default behavior.
 * @param candidateUser Only include tasks that are offered to the given user or to one of his groups.
 * @param candidateUserExpression Only include tasks that are offered to the user described by the given expression. See the user guide for more information on available functions.
 * @param includeAssignedTasks Also include tasks that are assigned to users in candidate queries. Default is to only include tasks that are not assigned to any user if you query by candidate user or group(s).
 * @param involvedUser Only include tasks that the given user is involved in. A user is involved in a task if an identity link exists between task and user (e.g., the user is the assignee).
 * @param involvedUserExpression Only include tasks that the user described by the given expression is involved in. A user is involved in a task if an identity link exists between task and user (e.g., the user is the assignee). See the user guide for more information on available functions.
 * @param assigned If set to true, restricts the query to all tasks that are assigned.
 * @param unassigned If set to true, restricts the query to all tasks that are unassigned.
 * @param taskDefinitionKey Restrict to tasks that have the given key.
 * @param taskDefinitionKeyIn Restrict to tasks that have one of the given keys. The keys need to be in a comma-separated list.
 * @param taskDefinitionKeyLike Restrict to tasks that have a key that has the parameter value as a substring.
 * @param name Restrict to tasks that have the given name.
 * @param nameNotEqual Restrict to tasks that do not have the given name.
 * @param nameLike Restrict to tasks that have a name with the given parameter value as substring.
 * @param nameNotLike Restrict to tasks that do not have a name with the given parameter value as substring.
 * @param description Restrict to tasks that have the given description.
 * @param descriptionLike Restrict to tasks that have a description that has the parameter value as a substring.
 * @param priority Restrict to tasks that have the given priority.
 * @param maxPriority Restrict to tasks that have a lower or equal priority.
 * @param minPriority Restrict to tasks that have a higher or equal priority.
 * @param dueDate Restrict to tasks that are due on the given date. By default*, the date must have the format yyyy-MM-dd'T'HH:mm:ss.SSSZ, e.g., 2013-01-23T14:42:45.234+0200.
 * @param dueDateExpression Restrict to tasks that are due on the date described by the given expression. See the user guide for more information on available functions. The expression must evaluate to a java.util.Date or org.joda.time.DateTime object.
 * @param dueAfter Restrict to tasks that are due after the given date. By default*, the date must have the format yyyy-MM-dd'T'HH:mm:ss.SSSZ, e.g., 2013-01-23T14:42:45.453+0200.
 * @param dueAfterExpression Restrict to tasks that are due after the date described by the given expression. See the user guide for more information on available functions. The expression must evaluate to a java.util.Date or org.joda.time.DateTime object.
 * @param dueBefore Restrict to tasks that are due before the given date. By default*, the date must have the format yyyy-MM-dd'T'HH:mm:ss.SSSZ, e.g., 2013-01-23T14:42:45.667+0200.
 * @param dueBeforeExpression Restrict to tasks that are due before the date described by the given expression. See the user guide for more information on available functions. The expression must evaluate to a java.util.Date or org.joda.time.DateTime object.
 * @param withoutDueDate Only include tasks which have no due date. Value may only be true, as false is the default behavior.
 * @param followUpDate Restrict to tasks that have a followUp date on the given date. By defalut*, the date must have the format yyyy-MM-dd'T'HH:mm:ss.SSSZ, e.g., 2013-01-23T14:42:45.384+0200.
 * @param followUpDateExpression Restrict to tasks that have a followUp date on the date described by the given expression. See the user guide for more information on available functions. The expression must evaluate to a java.util.Date or org.joda.time.DateTime object.
 * @param followUpAfter Restrict to tasks that have a followUp date after the given date. By default*, the date must have the format yyyy-MM-dd'T'HH:mm:ss.SSSZ, e.g., 2013-01-23T14:42:45.343+0200.
 * @param followUpAfterExpression Restrict to tasks that have a followUp date after the date described by the given expression. See the user guide for more information on available functions. The expression must evaluate to a java.util.Date or org.joda.time.DateTime object.
 * @param followUpBefore Restrict to tasks that have a followUp date before the given date. By default*, the date must have the format yyyy-MM-dd'T'HH:mm:ss.SSSZ, e.g., 2013-01-23T14:42:45.736+0200.
 * @param followUpBeforeExpression Restrict to tasks that have a followUp date before the date described by the given expression. See the user guide for more information on available functions. The expression must evaluate to a java.util.Date or org.joda.time.DateTime object.
 * @param followUpBeforeOrNotExistent Restrict to tasks that have no followUp date or a followUp date before the given date. See the user guide for more information on available functions. The expression must evaluate to a java.util.Date or org.joda.time.DateTime object.
 * @param followUpBeforeOrNotExistentExpression Restrict to tasks that have no followUp date or a followUp date before the date described by the given expression. See the user guide for more information on available functions. The expression must evaluate to a java.util.Date or org.joda.time.DateTime object.
 * @param createdOn Restrict to tasks that were created on the given date. By default*, the date must have the format yyyy-MM-dd'T'HH:mm:ss.SSSZ, e.g., 2013-01-23T14:42:45.038+0200.
 * @param createdOnExpression Restrict to tasks that were created on the date described by the given expression. See the user guide for more information on available functions. The expression must evaluate to a java.util.Date or org.joda.time.DateTime object.
 * @param createdAfter Restrict to tasks that were created after the given date. By default*, the date must have the format yyyy-MM-dd'T'HH:mm:ss.SSSZ, e.g., 2013-01-23T14:42:45.654+0200.
 * @param createdAfterExpression Restrict to tasks that were created after the date described by the given expression. See the user guide for more information on available functions. The expression must evaluate to a java.util.Date or org.joda.time.DateTime object.
 * @param createdBefore Restrict to tasks that were created before the given date. The date must have the format yyyy-MM-dd'T'HH:mm:ss.SSSZ, e.g., 2013-01-23T14:42:45.324+0200.
 * @param createdBeforeExpression Restrict to tasks that were created before the date described by the given expression. See the user guide for more information on available functions. The expression must evaluate to a java.util.Date or org.joda.time.DateTime object.
 * @param delegationState Restrict to tasks that are in the given delegation state. Valid values are PENDING and RESOLVED.
 * @param candidateGroups Restrict to tasks that are offered to any of the given candidate groups. Takes a JSON array of group names, so for example ["developers", "support", "sales"].
 * @param candidateGroupsExpression Restrict to tasks that are offered to any of the candidate groups described by the given expression. See the user guide for more information on available functions. The expression must evaluate to java.util.List of Strings.
 * @param active Only include active tasks. Value may only be true, as false is the default behavior.
 * @param suspended Only include suspended tasks. Value may only be true, as false is the default behavior.
 * @param taskVariables A JSON array to only include tasks that have variables with certain values.
 *                      The array consists of JSON objects with three properties name, operator and value. name is the variable name, operator is the comparison operator to be used and value the variable value.
 *                      value may be of type String, Number or Boolean.
 *                      Valid operator values are: eq - equal to; neq - not equal to; gt - greater than; gteq - greater than or equal to; lt - lower than; lteq - lower than or equal to; like.
 * @param processVariables A JSON array to only include tasks that belong to a process instance with variables with certain values.
 *                         The array consists of JSON objects with three properties name, operator and value. name is the variable name, operator is the comparison operator to be used and value the variable value.
 *                         value may be of type String, Number or Boolean.
 *                         Valid operator values are: eq - equal to; neq - not equal to; gt - greater than; gteq - greater than or equal to; lt - lower than; lteq - lower than or equal to; like;notLike.
 * @param caseInstanceVariables A JSON array to only include tasks that belong to a case instance with variables with certain values.
 *                              The array consists of JSON objects with three properties name, operator and value. name is the variable name, operator is the comparison operator to be used and value the variable value.
 *                              value may be of type String, Number or Boolean.
 *                              Valid operator values are: eq - equal to; neq - not equal to; gt - greater than; gteq - greater than or equal to; lt - lower than; lteq - lower than or equal to; like.
 * @param variableNamesIgnoreCase Match all variable names in this query case-insensitively. If set to true variableName and variablename are treated as equal.
 * @param variableValuesIgnoreCase Match all variable values in this query case-insensitively. If set to true variableValue and variablevalue are treated as equal.
 * @param parentTaskId Restrict query to all tasks that are sub tasks of the given task. Takes a task id.
 * @param sorting A JSON array of criteria to sort the result by. Each element of the array is a JSON object that specifies one ordering. The position in the array identifies the rank of an ordering, i.e., whether it is primary, secondary, etc. The ordering objects have the following properties:
 */
case class TaskFindQuery(
  taskId: Option[String] = None,
  taskIdIn: Option[Seq[String]] = None,
  processInstanceId: Option[String] = None,
  processInstanceIdIn: Option[Seq[String]] = None,
  processInstanceBusinessKey: Option[String] = None,
  processInstanceBusinessKeyExpression: Option[String] = None,
  processInstanceBusinessKeyIn: Option[Seq[String]] = None,
  processInstanceBusinessKeyLike: Option[String] = None,
  processInstanceBusinessKeyLikeExpression: Option[String] = None,
  processDefinitionId: Option[String] = None,
  processDefinitionKey: Option[String] = None,
  processDefinitionKeyIn: Option[Seq[String]] = None,
  processDefinitionName: Option[String] = None,
  processDefinitionNameLike: Option[String] = None,
  executionId: Option[String] = None,
  caseInstanceId: Option[String] = None,
  caseInstanceBusinessKey: Option[String] = None,
  caseInstanceBusinessKeyLike: Option[String] = None,
  caseDefinitionId: Option[String] = None,
  caseDefinitionKey: Option[String] = None,
  caseDefinitionName: Option[String] = None,
  caseDefinitionNameLike: Option[String] = None,
  caseExecutionId: Option[String] = None,
  activityInstanceIdIn: Option[Seq[String]] = None,
  tenantIdIn: Option[Seq[String]] = None,
  withoutTenantId: Option[Seq[String]] = None,
  assignee: Option[String] = None,
  assigneeExpression: Option[String] = None,
  assigneeLike: Option[String] = None,
  assigneeLikeExpression: Option[String] = None,
  assigneeIn: Option[Seq[String]] = None,
  assigneeNotIn: Option[Seq[String]] = None,
  owner: Option[String] = None,
  ownerExpression: Option[String] = None,
  candidateGroup: Option[String] = None,
  candidateGroupExpression: Option[String] = None,
  withCandidateGroups: Option[Boolean] = None,
  withoutCandidateGroups: Option[Boolean] = None,
  withCandidateUsers: Option[Boolean] = None,
  withoutCandidateUsers: Option[Boolean] = None,
  candidateUser: Option[String] = None,
  candidateUserExpression: Option[String] = None,
  includeAssignedTasks: Option[Boolean] = None,
  involvedUser: Option[String] = None,
  involvedUserExpression: Option[String] = None,
  assigned: Option[Boolean] = None,
  unassigned: Option[Boolean] = None,
  taskDefinitionKey: Option[String] = None,
  taskDefinitionKeyIn: Option[Seq[String]] = None,
  taskDefinitionKeyLike: Option[String] = None,
  name: Option[String] = None,
  nameNotEqual: Option[String] = None,
  nameLike: Option[String] = None,
  nameNotLike: Option[String] = None,
  description: Option[String] = None,
  descriptionLike: Option[String] = None,
  priority: Option[Int] = None,
  maxPriority: Option[Int] = None,
  minPriority: Option[Int] = None,
  dueDate: Option[String] = None,
  dueDateExpression: Option[String] = None,
  dueAfter: Option[String] = None,
  dueAfterExpression: Option[String] = None,
  dueBefore: Option[String] = None,
  dueBeforeExpression: Option[String] = None,
  withoutDueDate: Option[String] = None,
  followUpDate: Option[String] = None,
  followUpDateExpression: Option[String] = None,
  followUpAfter: Option[String] = None,
  followUpAfterExpression: Option[String] = None,
  followUpBefore: Option[String] = None,
  followUpBeforeExpression: Option[String] = None,
  followUpBeforeOrNotExistent: Option[String] = None,
  followUpBeforeOrNotExistentExpression: Option[String] = None,
  createdOn: Option[String] = None,
  createdOnExpression: Option[String] = None,
  createdAfter: Option[String] = None,
  createdAfterExpression: Option[String] = None,
  createdBefore: Option[String] = None,
  createdBeforeExpression: Option[String] = None,
  delegationState: Option[String] = None,
  candidateGroups: Option[Seq[String]] = None,
  candidateGroupsExpression: Option[String] = None,
  active: Option[Boolean] = None,
  suspended: Option[Boolean] = None,
  taskVariables: Option[Seq[VariableExpression]] = None,
  processVariables: Option[Seq[VariableExpression]] = None,
  caseInstanceVariables: Option[Seq[VariableExpression]] = None,
  variableNamesIgnoreCase: Option[Boolean] = None,
  variableValuesIgnoreCase: Option[Boolean] = None,
  parentTaskId: Option[String] = None,
  sorting: Option[Seq[TaskSorting]] = None
)

object TaskFindQuery {
  implicit val format = Jsonx.formatCaseClass[TaskFindQuery]
}
