package biz.lobachev.annette.bpm_repository.test

object BpmRepositoryData {

  val bpmnXml =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:camunda="http://camunda.org/schema/1.0/bpmn" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" xmlns:modeler="http://camunda.org/schema/modeler/1.0" id="Definitions_04hbdmj" targetNamespace="http://bpmn.io/schema/bpmn" exporter="Camunda Modeler" exporterVersion="4.11.1" modeler:executionPlatform="Camunda Platform" modeler:executionPlatformVersion="7.15.0">
      |  <bpmn:process id="ApproveExpenses" name="Approve Expenses" isExecutable="true">
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
