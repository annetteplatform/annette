package biz.lobachev.annette.camunda.test

import biz.lobachev.annette.camunda.api.common.{
  BooleanValue,
  DateValue,
  DoubleValue,
  IntegerValue,
  JsonValue,
  LongValue,
  ObjectValue,
  StringValue,
  ValueInfo,
  XmlValue
}

object BpmData {

  val simpleProcess =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:camunda="http://camunda.org/schema/1.0/bpmn" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" xmlns:modeler="http://camunda.org/schema/modeler/1.0" id="Definitions_1cphvlk" targetNamespace="http://bpmn.io/schema/bpmn" exporter="Camunda Modeler" exporterVersion="4.12.0" modeler:executionPlatform="Camunda Platform" modeler:executionPlatformVersion="7.16.0">
      |  <bpmn:process id="SimpleProcess" name="Simple Process" isExecutable="true">
      |    <bpmn:startEvent id="StartEvent_1">
      |      <bpmn:outgoing>Flow_1gzkp76</bpmn:outgoing>
      |    </bpmn:startEvent>
      |    <bpmn:sequenceFlow id="Flow_1gzkp76" sourceRef="StartEvent_1" targetRef="UserTaskActivity1" />
      |    <bpmn:userTask id="UserTaskActivity1" name="User Task 1">
      |      <bpmn:extensionElements>
      |        <camunda:inputOutput>
      |          <camunda:inputParameter name="localStr">${str}</camunda:inputParameter>
      |          <camunda:outputParameter name="str">${localStr}</camunda:outputParameter>
      |        </camunda:inputOutput>
      |      </bpmn:extensionElements>
      |      <bpmn:incoming>Flow_1gzkp76</bpmn:incoming>
      |      <bpmn:outgoing>Flow_09nwq2k</bpmn:outgoing>
      |    </bpmn:userTask>
      |    <bpmn:sequenceFlow id="Flow_09nwq2k" sourceRef="UserTaskActivity1" targetRef="ScriptActivity" />
      |    <bpmn:scriptTask id="ScriptActivity" name="Script Activity" scriptFormat="JavaScript">
      |      <bpmn:incoming>Flow_09nwq2k</bpmn:incoming>
      |      <bpmn:outgoing>Flow_115ctmq</bpmn:outgoing>
      |      <bpmn:script>console.log("simple process");
      |console.log(str)
      |execution.setVariable("str", str + " - Hello from Script")
      |execution.setVariable("newStrVar", "hello")
      |execution.setVariable("newNumVar", 42)
      |execution.setVariable("newBoolVar", true)</bpmn:script>
      |    </bpmn:scriptTask>
      |    <bpmn:endEvent id="Event_0bg4v75">
      |      <bpmn:incoming>Flow_1h4j2wq</bpmn:incoming>
      |    </bpmn:endEvent>
      |    <bpmn:sequenceFlow id="Flow_115ctmq" sourceRef="ScriptActivity" targetRef="UserTaskActivity2" />
      |    <bpmn:sequenceFlow id="Flow_1h4j2wq" sourceRef="UserTaskActivity2" targetRef="Event_0bg4v75" />
      |    <bpmn:userTask id="UserTaskActivity2" name="User Task 2">
      |      <bpmn:incoming>Flow_115ctmq</bpmn:incoming>
      |      <bpmn:outgoing>Flow_1h4j2wq</bpmn:outgoing>
      |    </bpmn:userTask>
      |  </bpmn:process>
      |  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
      |    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="SimpleProcess">
      |      <bpmndi:BPMNEdge id="Flow_1h4j2wq_di" bpmnElement="Flow_1h4j2wq">
      |        <di:waypoint x="680" y="117" />
      |        <di:waypoint x="762" y="117" />
      |      </bpmndi:BPMNEdge>
      |      <bpmndi:BPMNEdge id="Flow_115ctmq_di" bpmnElement="Flow_115ctmq">
      |        <di:waypoint x="530" y="117" />
      |        <di:waypoint x="580" y="117" />
      |      </bpmndi:BPMNEdge>
      |      <bpmndi:BPMNEdge id="Flow_09nwq2k_di" bpmnElement="Flow_09nwq2k">
      |        <di:waypoint x="370" y="117" />
      |        <di:waypoint x="430" y="117" />
      |      </bpmndi:BPMNEdge>
      |      <bpmndi:BPMNEdge id="Flow_1gzkp76_di" bpmnElement="Flow_1gzkp76">
      |        <di:waypoint x="215" y="117" />
      |        <di:waypoint x="270" y="117" />
      |      </bpmndi:BPMNEdge>
      |      <bpmndi:BPMNShape id="_BPMNShape_StartEvent_2" bpmnElement="StartEvent_1">
      |        <dc:Bounds x="179" y="99" width="36" height="36" />
      |      </bpmndi:BPMNShape>
      |      <bpmndi:BPMNShape id="Activity_1uejsmr_di" bpmnElement="UserTaskActivity1">
      |        <dc:Bounds x="270" y="77" width="100" height="80" />
      |      </bpmndi:BPMNShape>
      |      <bpmndi:BPMNShape id="Activity_18wfkpw_di" bpmnElement="ScriptActivity">
      |        <dc:Bounds x="430" y="77" width="100" height="80" />
      |      </bpmndi:BPMNShape>
      |      <bpmndi:BPMNShape id="Event_0bg4v75_di" bpmnElement="Event_0bg4v75">
      |        <dc:Bounds x="762" y="99" width="36" height="36" />
      |      </bpmndi:BPMNShape>
      |      <bpmndi:BPMNShape id="Activity_0rirely_di" bpmnElement="UserTaskActivity2">
      |        <dc:Bounds x="580" y="77" width="100" height="80" />
      |      </bpmndi:BPMNShape>
      |    </bpmndi:BPMNPlane>
      |  </bpmndi:BPMNDiagram>
      |</bpmn:definitions>""".stripMargin

  val extTaskProcess =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:camunda="http://camunda.org/schema/1.0/bpmn" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" xmlns:modeler="http://camunda.org/schema/modeler/1.0" id="Definitions_1cphvlk" targetNamespace="http://bpmn.io/schema/bpmn" exporter="Camunda Modeler" exporterVersion="4.12.0" modeler:executionPlatform="Camunda Platform" modeler:executionPlatformVersion="7.16.0">
      |  <bpmn:process id="SimpleProcessExtTask" name="Simple Process with ext. task" isExecutable="true">
      |    <bpmn:startEvent id="StartEvent_1">
      |      <bpmn:outgoing>Flow_1gzkp76</bpmn:outgoing>
      |    </bpmn:startEvent>
      |    <bpmn:sequenceFlow id="Flow_1gzkp76" sourceRef="StartEvent_1" targetRef="ExternalTask" />
      |    <bpmn:endEvent id="Event_0bg4v75">
      |      <bpmn:incoming>Flow_1h4j2wq</bpmn:incoming>
      |    </bpmn:endEvent>
      |    <bpmn:sequenceFlow id="Flow_115ctmq" sourceRef="ExternalTask" targetRef="UserTaskActivity2" />
      |    <bpmn:sequenceFlow id="Flow_1h4j2wq" sourceRef="UserTaskActivity2" targetRef="Event_0bg4v75" />
      |    <bpmn:userTask id="UserTaskActivity2" name="User Task 2">
      |      <bpmn:incoming>Flow_115ctmq</bpmn:incoming>
      |      <bpmn:outgoing>Flow_1h4j2wq</bpmn:outgoing>
      |    </bpmn:userTask>
      |    <bpmn:serviceTask id="ExternalTask" name="External Task" camunda:type="external" camunda:topic="TopicA">
      |      <bpmn:incoming>Flow_1gzkp76</bpmn:incoming>
      |      <bpmn:outgoing>Flow_115ctmq</bpmn:outgoing>
      |    </bpmn:serviceTask>
      |  </bpmn:process>
      |  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
      |    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="SimpleProcessExtTask">
      |      <bpmndi:BPMNEdge id="Flow_1h4j2wq_di" bpmnElement="Flow_1h4j2wq">
      |        <di:waypoint x="500" y="117" />
      |        <di:waypoint x="582" y="117" />
      |      </bpmndi:BPMNEdge>
      |      <bpmndi:BPMNEdge id="Flow_115ctmq_di" bpmnElement="Flow_115ctmq">
      |        <di:waypoint x="350" y="117" />
      |        <di:waypoint x="400" y="117" />
      |      </bpmndi:BPMNEdge>
      |      <bpmndi:BPMNEdge id="Flow_1gzkp76_di" bpmnElement="Flow_1gzkp76">
      |        <di:waypoint x="188" y="117" />
      |        <di:waypoint x="250" y="117" />
      |      </bpmndi:BPMNEdge>
      |      <bpmndi:BPMNShape id="Event_0bg4v75_di" bpmnElement="Event_0bg4v75">
      |        <dc:Bounds x="582" y="99" width="36" height="36" />
      |      </bpmndi:BPMNShape>
      |      <bpmndi:BPMNShape id="Activity_0rirely_di" bpmnElement="UserTaskActivity2">
      |        <dc:Bounds x="400" y="77" width="100" height="80" />
      |      </bpmndi:BPMNShape>
      |      <bpmndi:BPMNShape id="Activity_1991dg7_di" bpmnElement="ExternalTask">
      |        <dc:Bounds x="250" y="77" width="100" height="80" />
      |      </bpmndi:BPMNShape>
      |      <bpmndi:BPMNShape id="_BPMNShape_StartEvent_2" bpmnElement="StartEvent_1">
      |        <dc:Bounds x="152" y="99" width="36" height="36" />
      |      </bpmndi:BPMNShape>
      |    </bpmndi:BPMNPlane>
      |  </bpmndi:BPMNDiagram>
      |</bpmn:definitions>""".stripMargin
  val variables      = Some(
    Map(
      "str"          -> StringValue("aStringValue1"),
      "bool"         -> BooleanValue(true),
      "long"         -> LongValue(12345678901234L),
      "int"          -> IntegerValue(1234567),
      "double"       -> DoubleValue(1234567890.1234),
      "date"         -> DateValue("1977-03-30T00:00:00.000+0000"),
      "json"         -> JsonValue(
        "{\"id\": \"my-bpmn-model1\",\n  \"name\": \"My bpmn model 111\",\n  \"updatedBy\": \"person~P0002\"\n  }"
      ),
      "xml"          -> XmlValue("<name>Valery</name>"),
      "testScalaVar" -> ObjectValue(
        "{\n    \"id\": \"val\",\n    \"name\": \"Valery\",\n    \"active\": true,\n    \"intNumber\": 77\n  }",
        ValueInfo(
          objectTypeName = Some("camundatest.TestScalaVar"),
          serializationDataFormat = Some("application/json")
        )
      )
    )
  )

  val bpmnXml =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:camunda="http://camunda.org/schema/1.0/bpmn" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" xmlns:modeler="http://camunda.org/schema/modeler/1.0" id="Definitions_04hbdmj" targetNamespace="http://bpmn.io/schema/bpmn" exporter="Camunda Modeler" exporterVersion="4.11.1" modeler:executionPlatform="Camunda Platform" modeler:executionPlatformVersion="7.15.0">
      |  <bpmn:process id="ApproveExpensesA" name="Approve Expenses A" isExecutable="true">
      |    <bpmn:startEvent id="StartEvent_1" camunda:formRef="ApplyExpenses" camunda:formRefBinding="latest">
      |      <bpmn:outgoing>Flow_0n27kix</bpmn:outgoing>
      |    </bpmn:startEvent>
      |    <bpmn:sequenceFlow id="Flow_0n27kix" sourceRef="StartEvent_1" targetRef="Activity_0exgb35" />
      |    <bpmn:userTask id="Activity_0exgb35" name="Approve Expenses" camunda:formRef="ApproveExpenses" camunda:formRefBinding="latest" camunda:candidateGroups="accounting">
      |      <bpmn:incoming>Flow_0n27kix</bpmn:incoming>
      |      <bpmn:outgoing>Flow_0b66jaa</bpmn:outgoing>
      |    </bpmn:userTask>
      |    <bpmn:endEvent id="Event_02mcixp">
      |      <bpmn:incoming>Flow_1prfu5d</bpmn:incoming>
      |    </bpmn:endEvent>
      |    <bpmn:sequenceFlow id="Flow_0b66jaa" sourceRef="Activity_0exgb35" targetRef="Activity_0qqou20" />
      |    <bpmn:userTask id="Activity_0qqou20" name="Check approvement" camunda:formRef="ApproveExpenses" camunda:formRefBinding="latest" camunda:assignee="demo">
      |      <bpmn:incoming>Flow_0b66jaa</bpmn:incoming>
      |      <bpmn:outgoing>Flow_1prfu5d</bpmn:outgoing>
      |    </bpmn:userTask>
      |    <bpmn:sequenceFlow id="Flow_1prfu5d" sourceRef="Activity_0qqou20" targetRef="Event_02mcixp" />
      |  </bpmn:process>
      |  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
      |    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="ApproveExpenses">
      |      <bpmndi:BPMNEdge id="Flow_0n27kix_di" bpmnElement="Flow_0n27kix">
      |        <di:waypoint x="215" y="117" />
      |        <di:waypoint x="270" y="117" />
      |      </bpmndi:BPMNEdge>
      |      <bpmndi:BPMNEdge id="Flow_0b66jaa_di" bpmnElement="Flow_0b66jaa">
      |        <di:waypoint x="370" y="117" />
      |        <di:waypoint x="510" y="117" />
      |      </bpmndi:BPMNEdge>
      |      <bpmndi:BPMNEdge id="Flow_1prfu5d_di" bpmnElement="Flow_1prfu5d">
      |        <di:waypoint x="610" y="117" />
      |        <di:waypoint x="762" y="117" />
      |      </bpmndi:BPMNEdge>
      |      <bpmndi:BPMNShape id="_BPMNShape_StartEvent_2" bpmnElement="StartEvent_1">
      |        <dc:Bounds x="179" y="99" width="36" height="36" />
      |      </bpmndi:BPMNShape>
      |      <bpmndi:BPMNShape id="Activity_0lsju36_di" bpmnElement="Activity_0exgb35">
      |        <dc:Bounds x="270" y="77" width="100" height="80" />
      |      </bpmndi:BPMNShape>
      |      <bpmndi:BPMNShape id="Event_02mcixp_di" bpmnElement="Event_02mcixp">
      |        <dc:Bounds x="762" y="99" width="36" height="36" />
      |      </bpmndi:BPMNShape>
      |      <bpmndi:BPMNShape id="Activity_13opc82_di" bpmnElement="Activity_0qqou20">
      |        <dc:Bounds x="510" y="77" width="100" height="80" />
      |      </bpmndi:BPMNShape>
      |    </bpmndi:BPMNPlane>
      |  </bpmndi:BPMNDiagram>
      |</bpmn:definitions>""".stripMargin

  val processA =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" xmlns:modeler="http://camunda.org/schema/modeler/1.0" id="Definitions_0jbg6oz" targetNamespace="http://bpmn.io/schema/bpmn" exporter="Camunda Modeler" exporterVersion="5.0.0-alpha.0" modeler:executionPlatform="Camunda Platform" modeler:executionPlatformVersion="7.16.0">
      |  <bpmn:collaboration id="Process_A">
      |    <bpmn:extensionElements />
      |    <bpmn:participant id="Participant_1" name="User1" processRef="Process_1" />
      |    <bpmn:participant id="Participant_2" name="User2" processRef="Process_2" />
      |    <bpmn:messageFlow id="Flow_1kxm1dn" sourceRef="Activity_0mrka36" targetRef="Activity_0tb8xp1" />
      |    <bpmn:messageFlow id="Flow_19oxkjd" sourceRef="Activity_1hdvdec" targetRef="Activity_1rudn8x" />
      |  </bpmn:collaboration>
      |  <bpmn:process id="Process_1" name="My Process 1" isExecutable="true">
      |    <bpmn:laneSet id="LaneSet_0yyhgg0" />
      |    <bpmn:startEvent id="StartEvent_1">
      |      <bpmn:outgoing>Flow_193iwpy</bpmn:outgoing>
      |    </bpmn:startEvent>
      |    <bpmn:task id="Activity_0mrka36">
      |      <bpmn:incoming>Flow_193iwpy</bpmn:incoming>
      |    </bpmn:task>
      |    <bpmn:sequenceFlow id="Flow_193iwpy" sourceRef="StartEvent_1" targetRef="Activity_0mrka36" />
      |    <bpmn:task id="Activity_1rudn8x">
      |      <bpmn:outgoing>Flow_1uvy78n</bpmn:outgoing>
      |    </bpmn:task>
      |    <bpmn:endEvent id="Event_16up9d2">
      |      <bpmn:incoming>Flow_1uvy78n</bpmn:incoming>
      |    </bpmn:endEvent>
      |    <bpmn:sequenceFlow id="Flow_1uvy78n" sourceRef="Activity_1rudn8x" targetRef="Event_16up9d2" />
      |  </bpmn:process>
      |  <bpmn:process id="Process_2" name="My Process 2" isExecutable="false">
      |    <bpmn:task id="Activity_0tb8xp1">
      |      <bpmn:incoming>Flow_0h3lr45</bpmn:incoming>
      |      <bpmn:outgoing>Flow_12au8x7</bpmn:outgoing>
      |    </bpmn:task>
      |    <bpmn:sequenceFlow id="Flow_12au8x7" sourceRef="Activity_0tb8xp1" targetRef="Activity_1hdvdec" />
      |    <bpmn:task id="Activity_1hdvdec">
      |      <bpmn:incoming>Flow_12au8x7</bpmn:incoming>
      |    </bpmn:task>
      |    <bpmn:boundaryEvent id="Event_0ktccin" attachedToRef="Activity_1hdvdec">
      |      <bpmn:outgoing>Flow_1dzw38o</bpmn:outgoing>
      |      <bpmn:timerEventDefinition id="TimerEventDefinition_1doluon" />
      |    </bpmn:boundaryEvent>
      |    <bpmn:task id="Activity_0czz6bk">
      |      <bpmn:incoming>Flow_1dzw38o</bpmn:incoming>
      |      <bpmn:outgoing>Flow_1egokfn</bpmn:outgoing>
      |    </bpmn:task>
      |    <bpmn:sequenceFlow id="Flow_1dzw38o" sourceRef="Event_0ktccin" targetRef="Activity_0czz6bk" />
      |    <bpmn:endEvent id="Event_0voyvc9">
      |      <bpmn:incoming>Flow_1egokfn</bpmn:incoming>
      |    </bpmn:endEvent>
      |    <bpmn:sequenceFlow id="Flow_1egokfn" sourceRef="Activity_0czz6bk" targetRef="Event_0voyvc9" />
      |    <bpmn:startEvent id="Event_01p3d92">
      |      <bpmn:outgoing>Flow_0h3lr45</bpmn:outgoing>
      |      <bpmn:messageEventDefinition id="MessageEventDefinition_1b3k4ph" />
      |    </bpmn:startEvent>
      |    <bpmn:sequenceFlow id="Flow_0h3lr45" sourceRef="Event_01p3d92" targetRef="Activity_0tb8xp1" />
      |  </bpmn:process>
      |  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
      |    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_A">
      |      <bpmndi:BPMNShape id="Participant_194ocv9_di" bpmnElement="Participant_1" isHorizontal="true">
      |        <dc:Bounds x="129" y="80" width="759" height="250" />
      |      </bpmndi:BPMNShape>
      |      <bpmndi:BPMNEdge id="Flow_193iwpy_di" bpmnElement="Flow_193iwpy">
      |        <di:waypoint x="228" y="200" />
      |        <di:waypoint x="280" y="200" />
      |      </bpmndi:BPMNEdge>
      |      <bpmndi:BPMNEdge id="Flow_1uvy78n_di" bpmnElement="Flow_1uvy78n">
      |        <di:waypoint x="570" y="200" />
      |        <di:waypoint x="622" y="200" />
      |      </bpmndi:BPMNEdge>
      |      <bpmndi:BPMNShape id="_BPMNShape_StartEvent_2" bpmnElement="StartEvent_1">
      |        <dc:Bounds x="192" y="182" width="36" height="36" />
      |      </bpmndi:BPMNShape>
      |      <bpmndi:BPMNShape id="Activity_0mrka36_di" bpmnElement="Activity_0mrka36">
      |        <dc:Bounds x="280" y="160" width="100" height="80" />
      |      </bpmndi:BPMNShape>
      |      <bpmndi:BPMNShape id="Activity_1rudn8x_di" bpmnElement="Activity_1rudn8x">
      |        <dc:Bounds x="470" y="160" width="100" height="80" />
      |      </bpmndi:BPMNShape>
      |      <bpmndi:BPMNShape id="Event_16up9d2_di" bpmnElement="Event_16up9d2">
      |        <dc:Bounds x="622" y="182" width="36" height="36" />
      |      </bpmndi:BPMNShape>
      |      <bpmndi:BPMNShape id="Participant_0k2staf_di" bpmnElement="Participant_2" isHorizontal="true">
      |        <dc:Bounds x="129" y="330" width="759" height="350" />
      |      </bpmndi:BPMNShape>
      |      <bpmndi:BPMNEdge id="Flow_12au8x7_di" bpmnElement="Flow_12au8x7">
      |        <di:waypoint x="380" y="460" />
      |        <di:waypoint x="470" y="460" />
      |      </bpmndi:BPMNEdge>
      |      <bpmndi:BPMNEdge id="Flow_1dzw38o_di" bpmnElement="Flow_1dzw38o">
      |        <di:waypoint x="530" y="518" />
      |        <di:waypoint x="530" y="580" />
      |        <di:waypoint x="600" y="580" />
      |      </bpmndi:BPMNEdge>
      |      <bpmndi:BPMNEdge id="Flow_1egokfn_di" bpmnElement="Flow_1egokfn">
      |        <di:waypoint x="700" y="580" />
      |        <di:waypoint x="752" y="580" />
      |      </bpmndi:BPMNEdge>
      |      <bpmndi:BPMNEdge id="Flow_0h3lr45_di" bpmnElement="Flow_0h3lr45">
      |        <di:waypoint x="228" y="460" />
      |        <di:waypoint x="280" y="460" />
      |      </bpmndi:BPMNEdge>
      |      <bpmndi:BPMNShape id="Activity_0tb8xp1_di" bpmnElement="Activity_0tb8xp1">
      |        <dc:Bounds x="280" y="420" width="100" height="80" />
      |      </bpmndi:BPMNShape>
      |      <bpmndi:BPMNShape id="Activity_1hdvdec_di" bpmnElement="Activity_1hdvdec">
      |        <dc:Bounds x="470" y="420" width="100" height="80" />
      |      </bpmndi:BPMNShape>
      |      <bpmndi:BPMNShape id="Activity_0czz6bk_di" bpmnElement="Activity_0czz6bk">
      |        <dc:Bounds x="600" y="540" width="100" height="80" />
      |      </bpmndi:BPMNShape>
      |      <bpmndi:BPMNShape id="Event_0voyvc9_di" bpmnElement="Event_0voyvc9">
      |        <dc:Bounds x="752" y="562" width="36" height="36" />
      |      </bpmndi:BPMNShape>
      |      <bpmndi:BPMNShape id="Event_1malbck_di" bpmnElement="Event_01p3d92">
      |        <dc:Bounds x="192" y="442" width="36" height="36" />
      |      </bpmndi:BPMNShape>
      |      <bpmndi:BPMNShape id="Event_12k44cl_di" bpmnElement="Event_0ktccin">
      |        <dc:Bounds x="512" y="482" width="36" height="36" />
      |      </bpmndi:BPMNShape>
      |      <bpmndi:BPMNEdge id="Flow_1kxm1dn_di" bpmnElement="Flow_1kxm1dn">
      |        <di:waypoint x="330" y="240" />
      |        <di:waypoint x="330" y="420" />
      |      </bpmndi:BPMNEdge>
      |      <bpmndi:BPMNEdge id="Flow_19oxkjd_di" bpmnElement="Flow_19oxkjd">
      |        <di:waypoint x="520" y="420" />
      |        <di:waypoint x="520" y="240" />
      |      </bpmndi:BPMNEdge>
      |    </bpmndi:BPMNPlane>
      |  </bpmndi:BPMNDiagram>
      |</bpmn:definitions>""".stripMargin

  val processB =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:camunda="http://camunda.org/schema/1.0/bpmn" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" xmlns:modeler="http://camunda.org/schema/modeler/1.0" id="Definitions_1mh09rf" targetNamespace="http://bpmn.io/schema/bpmn" exporter="Camunda Modeler" exporterVersion="5.0.0-alpha.0" modeler:executionPlatform="Camunda Platform" modeler:executionPlatformVersion="7.16.0">
      |  <bpmn:process id="ProcessB" name="Process B" isExecutable="true">
      |    <bpmn:startEvent id="StartEvent_1">
      |      <bpmn:outgoing>Flow_0edjmgz</bpmn:outgoing>
      |    </bpmn:startEvent>
      |    <bpmn:sequenceFlow id="Flow_0edjmgz" sourceRef="StartEvent_1" targetRef="Activity_1gnloyz" />
      |    <bpmn:sequenceFlow id="Flow_03w0f1m" sourceRef="Activity_1gnloyz" targetRef="Activity_1q1xwpr" />
      |    <bpmn:sequenceFlow id="Flow_1irkiyk" sourceRef="Activity_1q1xwpr" targetRef="Activity_160x6g5" />
      |    <bpmn:endEvent id="Event_1kzyavq">
      |      <bpmn:incoming>Flow_1ab5i61</bpmn:incoming>
      |    </bpmn:endEvent>
      |    <bpmn:sequenceFlow id="Flow_1ab5i61" sourceRef="Activity_160x6g5" targetRef="Event_1kzyavq" />
      |    <bpmn:userTask id="Activity_1gnloyz" name="Step 1">
      |      <bpmn:incoming>Flow_0edjmgz</bpmn:incoming>
      |      <bpmn:outgoing>Flow_03w0f1m</bpmn:outgoing>
      |    </bpmn:userTask>
      |    <bpmn:serviceTask id="Activity_1q1xwpr" name="Step 2" camunda:class="Test">
      |      <bpmn:incoming>Flow_03w0f1m</bpmn:incoming>
      |      <bpmn:outgoing>Flow_1irkiyk</bpmn:outgoing>
      |    </bpmn:serviceTask>
      |    <bpmn:userTask id="Activity_160x6g5" name="Step3">
      |      <bpmn:incoming>Flow_1irkiyk</bpmn:incoming>
      |      <bpmn:outgoing>Flow_1ab5i61</bpmn:outgoing>
      |    </bpmn:userTask>
      |  </bpmn:process>
      |  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
      |    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="ProcessB">
      |      <bpmndi:BPMNEdge id="Flow_0edjmgz_di" bpmnElement="Flow_0edjmgz">
      |        <di:waypoint x="215" y="117" />
      |        <di:waypoint x="270" y="117" />
      |      </bpmndi:BPMNEdge>
      |      <bpmndi:BPMNEdge id="Flow_03w0f1m_di" bpmnElement="Flow_03w0f1m">
      |        <di:waypoint x="370" y="117" />
      |        <di:waypoint x="430" y="117" />
      |      </bpmndi:BPMNEdge>
      |      <bpmndi:BPMNEdge id="Flow_1irkiyk_di" bpmnElement="Flow_1irkiyk">
      |        <di:waypoint x="530" y="117" />
      |        <di:waypoint x="590" y="117" />
      |      </bpmndi:BPMNEdge>
      |      <bpmndi:BPMNEdge id="Flow_1ab5i61_di" bpmnElement="Flow_1ab5i61">
      |        <di:waypoint x="690" y="117" />
      |        <di:waypoint x="752" y="117" />
      |      </bpmndi:BPMNEdge>
      |      <bpmndi:BPMNShape id="_BPMNShape_StartEvent_2" bpmnElement="StartEvent_1">
      |        <dc:Bounds x="179" y="99" width="36" height="36" />
      |      </bpmndi:BPMNShape>
      |      <bpmndi:BPMNShape id="Event_1kzyavq_di" bpmnElement="Event_1kzyavq">
      |        <dc:Bounds x="752" y="99" width="36" height="36" />
      |      </bpmndi:BPMNShape>
      |      <bpmndi:BPMNShape id="Activity_1dyei91_di" bpmnElement="Activity_1gnloyz">
      |        <dc:Bounds x="270" y="77" width="100" height="80" />
      |      </bpmndi:BPMNShape>
      |      <bpmndi:BPMNShape id="Activity_0p7sk9p_di" bpmnElement="Activity_1q1xwpr">
      |        <dc:Bounds x="430" y="77" width="100" height="80" />
      |      </bpmndi:BPMNShape>
      |      <bpmndi:BPMNShape id="Activity_07jzitj_di" bpmnElement="Activity_160x6g5">
      |        <dc:Bounds x="590" y="77" width="100" height="80" />
      |      </bpmndi:BPMNShape>
      |    </bpmndi:BPMNPlane>
      |  </bpmndi:BPMNDiagram>
      |</bpmn:definitions>
      |""".stripMargin

  val processBe =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" xmlns:modeler="http://camunda.org/schema/modeler/1.0" id="Definitions_1mh09rf" targetNamespace="http://bpmn.io/schema/bpmn" exporter="Camunda Modeler" exporterVersion="5.0.0-alpha.0" modeler:executionPlatform="Camunda Platform" modeler:executionPlatformVersion="7.16.0">
      |  <bpmn:process id="ProcessB" name="Process B" isExecutable="true">
      |    <bpmn:startEvent id="StartEvent_1">
      |      <bpmn:outgoing>Flow_0edjmgz</bpmn:outgoing>
      |    </bpmn:startEvent>
      |    <bpmn:sequenceFlow id="Flow_0edjmgz" sourceRef="StartEvent_1" targetRef="Activity_1gnloyz" />
      |    <bpmn:sequenceFlow id="Flow_03w0f1m" sourceRef="Activity_1gnloyz" targetRef="Activity_1q1xwpr" />
      |    <bpmn:sequenceFlow id="Flow_1irkiyk" sourceRef="Activity_1q1xwpr" targetRef="Activity_160x6g5" />
      |    <bpmn:endEvent id="Event_1kzyavq">
      |      <bpmn:incoming>Flow_1ab5i61</bpmn:incoming>
      |    </bpmn:endEvent>
      |    <bpmn:sequenceFlow id="Flow_1ab5i61" sourceRef="Activity_160x6g5" targetRef="Event_1kzyavq" />
      |    <bpmn:userTask id="Activity_1gnloyz" name="Step 1">
      |      <bpmn:incoming>Flow_0edjmgz</bpmn:incoming>
      |      <bpmn:outgoing>Flow_03w0f1m</bpmn:outgoing>
      |    </bpmn:userTask>
      |    <bpmn:serviceTask id="Activity_1q1xwpr" name="Step 2">
      |      <bpmn:incoming>Flow_03w0f1m</bpmn:incoming>
      |      <bpmn:outgoing>Flow_1irkiyk</bpmn:outgoing>
      |    </bpmn:serviceTask>
      |    <bpmn:scriptTask id="Activity_160x6g5" name="Step3">
      |      <bpmn:incoming>Flow_1irkiyk</bpmn:incoming>
      |      <bpmn:outgoing>Flow_1ab5i61</bpmn:outgoing>
      |    </bpmn:scriptTask>
      |  </bpmn:process>
      |  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
      |    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="ProcessB">
      |      <bpmndi:BPMNEdge id="Flow_0edjmgz_di" bpmnElement="Flow_0edjmgz">
      |        <di:waypoint x="215" y="117" />
      |        <di:waypoint x="270" y="117" />
      |      </bpmndi:BPMNEdge>
      |      <bpmndi:BPMNEdge id="Flow_03w0f1m_di" bpmnElement="Flow_03w0f1m">
      |        <di:waypoint x="370" y="117" />
      |        <di:waypoint x="430" y="117" />
      |      </bpmndi:BPMNEdge>
      |      <bpmndi:BPMNEdge id="Flow_1irkiyk_di" bpmnElement="Flow_1irkiyk">
      |        <di:waypoint x="530" y="117" />
      |        <di:waypoint x="590" y="117" />
      |      </bpmndi:BPMNEdge>
      |      <bpmndi:BPMNEdge id="Flow_1ab5i61_di" bpmnElement="Flow_1ab5i61">
      |        <di:waypoint x="690" y="117" />
      |        <di:waypoint x="752" y="117" />
      |      </bpmndi:BPMNEdge>
      |      <bpmndi:BPMNShape id="_BPMNShape_StartEvent_2" bpmnElement="StartEvent_1">
      |        <dc:Bounds x="179" y="99" width="36" height="36" />
      |      </bpmndi:BPMNShape>
      |      <bpmndi:BPMNShape id="Event_1kzyavq_di" bpmnElement="Event_1kzyavq">
      |        <dc:Bounds x="752" y="99" width="36" height="36" />
      |      </bpmndi:BPMNShape>
      |      <bpmndi:BPMNShape id="Activity_1dyei91_di" bpmnElement="Activity_1gnloyz">
      |        <dc:Bounds x="270" y="77" width="100" height="80" />
      |      </bpmndi:BPMNShape>
      |      <bpmndi:BPMNShape id="Activity_0p7sk9p_di" bpmnElement="Activity_1q1xwpr">
      |        <dc:Bounds x="430" y="77" width="100" height="80" />
      |      </bpmndi:BPMNShape>
      |      <bpmndi:BPMNShape id="Activity_1o97qy6_di" bpmnElement="Activity_160x6g5">
      |        <dc:Bounds x="590" y="77" width="100" height="80" />
      |      </bpmndi:BPMNShape>
      |    </bpmndi:BPMNPlane>
      |  </bpmndi:BPMNDiagram>
      |</bpmn:definitions>
      |""".stripMargin

  val dmnXml =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<definitions xmlns="https://www.omg.org/spec/DMN/20191111/MODEL/" xmlns:dmndi="https://www.omg.org/spec/DMN/20191111/DMNDI/" xmlns:dc="http://www.omg.org/spec/DMN/20180521/DC/" id="Definitions_0eear6m" name="DRD" namespace="http://camunda.org/schema/1.0/dmn">
      |  <decision id="Decision_0ek7xn0" name="Decision 1">
      |    <decisionTable id="DecisionTable_1r9g981">
      |      <input id="Input_1">
      |        <inputExpression id="InputExpression_1" typeRef="string">
      |          <text></text>
      |        </inputExpression>
      |      </input>
      |      <output id="Output_1" typeRef="string" />
      |    </decisionTable>
      |  </decision>
      |  <dmndi:DMNDI>
      |    <dmndi:DMNDiagram>
      |      <dmndi:DMNShape dmnElementRef="Decision_0ek7xn0">
      |        <dc:Bounds height="80" width="180" x="160" y="100" />
      |      </dmndi:DMNShape>
      |    </dmndi:DMNDiagram>
      |  </dmndi:DMNDI>
      |</definitions>
      |""".stripMargin

  val cmmnXml = """<?xml version="1.0" encoding="ISO-8859-1" standalone="yes"?>
                  |
                  |<definitions id="_81291489-586f-4b00-b7c4-1ef8f7523c29"
                  |                  targetNamespace="Examples"
                  |                  xmlns="http://www.omg.org/spec/CMMN/20131201/MODEL"
                  |                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                  |
                  |  <case id="loanApplication" name="Loan Application Case">
                  |
                  |    <casePlanModel name="Loan Application" id="CasePlanModel_1">
                  |
                  |      <planItem definitionRef="Stage_1" id="PI_Stage_1"/>
                  |      <planItem definitionRef="HumanTask_6" id="PI_HumanTask_6"/>
                  |
                  |      <humanTask name="Capture Applicant Data" id="HumanTask_1"/>
                  |      <humanTask name="Obtain Schufa-Information" id="HumanTask_2"/>
                  |      <humanTask name="Obtain Credit-worthiness" id="HumanTask_3"/>
                  |      <humanTask name="Review Documents" id="HumanTask_4"/>
                  |      <humanTask name="Request Missing Documents" id="HumanTask_5"/>
                  |      <humanTask name="Decide About Loan Application" id="HumanTask_6"/>
                  |
                  |      <stage name="Obtain Customer Data" id="Stage_1">
                  |        <planItem definitionRef="HumanTask_1" id="PI_HumanTask_1"/>
                  |        <planItem definitionRef="HumanTask_2" id="PI_HumanTask_2"/>
                  |        <planItem definitionRef="HumanTask_3" id="PI_HumanTask_3"/>
                  |        <planItem definitionRef="HumanTask_4" id="PI_HumanTask_4"/>
                  |        <planItem definitionRef="HumanTask_5" id="PI_HumanTask_5"/>
                  |      </stage>
                  |
                  |    </casePlanModel>
                  |
                  |  </case>
                  |</definitions>""".stripMargin

  val invalidXml = """<?xml version="1.0" encoding="ISO-8859-1" standalone="yes"?>
                     |<def id="_81291489-586f-4b00-b7c4-1ef8f7523c29"
                     |                  targetNamespace="Examples"
                     |                  xmlns="http://www.omg.org/spec/CMMN/20131201/MODEL"
                     |                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                     |  <case id="loanApplication" name="Loan Application Case">
                     |  </case>
                     |</def>""".stripMargin

}
