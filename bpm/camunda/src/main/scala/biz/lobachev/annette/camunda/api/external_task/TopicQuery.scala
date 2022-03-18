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

package biz.lobachev.annette.camunda.api.external_task

import biz.lobachev.annette.camunda.api.common.VariableExpression
import play.api.libs.json.Json

/**
 * @param topicName Mandatory. The topic's name.
 * @param lockDuration Mandatory. The duration to lock the external tasks for in milliseconds.
 * @param variables A JSON array of String values that represent variable names. For each result task belonging
 *                  to this topic, the given variables are returned as well if they are accessible from
 *                  the external task's execution. If not provided - all variables will be fetched.
 * @param localVariables If true only local variables will be fetched.
 * @param businessKey A String value which enables the filtering of tasks based on process instance business key.
 * @param processDefinitionId Filter tasks based on process definition id.
 * @param processDefinitionIdIn Filter tasks based on process definition ids.
 * @param processDefinitionKey Filter tasks based on process definition key.
 * @param processDefinitionKeyIn Filter tasks based on process definition keys.
 * @param processDefinitionVersionTag Filter tasks based on process definition version tag.
 * @param withoutTenantId Filter tasks without tenant id.
 * @param tenantIdIn Filter tasks based on tenant ids.
 * @param processVariables A JSON object used for filtering tasks based on process instance variable values.
 *                         A property name of the object represents a process variable name, while the property value
 *                         represents the process variable value to filter tasks by.
 * @param deserializeValues Determines whether serializable variable values (typically variables that store
 *                           custom Java objects) should be deserialized on server side (default false).
 *                            If set to true, a serializable variable will be deserialized on server side
 *                            and transformed to JSON using Jackson's POJO/bean property introspection feature.
 *                            Note that this requires the Java classes of the variable value to be on
 *                            the REST API's classpath.
 *                            If set to false, a serializable variable will be returned in its serialized format.
 *                            For example, a variable that is serialized as XML will be returned as a JSON string
 *                            containing XML.
 * @param includeExtensionProperties Determines whether custom extension properties defined in the BPMN activity
 *                                   of the external task (e.g. via the Extensions tab in the Camunda modeler)
 *                                   should be included in the response. Default: false
 */
case class TopicQuery(
  topicName: String,
  lockDuration: Int,
  variables: Option[Seq[String]] = None,
  localVariables: Option[Boolean] = None,
  businessKey: Option[String] = None,
  processDefinitionId: Option[String] = None,
  processDefinitionIdIn: Option[Seq[String]] = None,
  processDefinitionKey: Option[String] = None,
  processDefinitionKeyIn: Option[Seq[String]] = None,
  processDefinitionVersionTag: Option[String] = None,
  withoutTenantId: Option[Boolean] = None,
  tenantIdIn: Option[String] = None,
  processVariables: Option[Seq[VariableExpression]] = None,
  deserializeValues: Option[Boolean] = None,
  includeExtensionProperties: Option[Boolean] = None
)

object TopicQuery {
  implicit val format = Json.format[TopicQuery]
}
