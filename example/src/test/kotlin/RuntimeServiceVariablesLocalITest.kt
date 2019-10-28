package org.camunda.bpm.extension.feign.itest

import com.tngtech.jgiven.annotation.As
import org.assertj.core.api.Assertions.assertThat
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.variable.Variables.*
import org.camunda.bpm.engine.variable.value.StringValue
import org.junit.Test
import java.util.*

@RuntimeServiceCategory
@As("Variables")
class RuntimeServiceVariablesLocalITest : CamundaBpmFeignITestBase<RuntimeService, RuntimeServiceActionStage, RuntimeServiceAssertStage>() {

  @Test
  fun `should add new, update and delete existing local variables`() {
    val processDefinitionKey = processDefinitionKey()
    val signalName = "var_process_blocker_2"
    val userTaskId = "user-task"
    given()
      .process_with_intermediate_signal_catch_event_is_deployed(processDefinitionKey, userTaskId, signalName)
      .and()
      .process_is_started_by_key(processDefinitionKey, "my-business-key2", "caseInstanceId2",
        createVariables()
          .putValue("VAR1", "VAL1")
          .putValue("VAR2", "VAL2")
          .putValueTyped("VAR3", stringValue("VAL3"))
          .putValueTyped("VAR4", objectValue("My object value").create())
      )

    whenever()
      .remoteService
      .removeVariableLocal(given().processInstance.id, "VAR2")

    whenever()
      .remoteService
      .setVariableLocal(given().processInstance.id, "VAR1", "NEW VALUE")

    whenever()
      .remoteService
      .setVariableLocal(given().processInstance.id, "TO_REMOVE", "TO_REMOVE")

    whenever()
      .remoteService
      .removeVariablesLocal(given().processInstance.id, listOf("TO_REMOVE"))


    whenever()
      .remoteService
      .setVariableLocal(given().processInstance.id, "VAR5", "untyped")

    whenever()
      .remoteService
      .setVariablesLocal(given().processInstance.id, createVariables().putValueTyped("VAR6", stringValue("typed")))


    then()
      .process_instance_exists(processDefinitionKey) { instance, stage ->
        assertThat(instance.businessKey).isEqualTo("my-business-key2")
        assertThat(instance.caseInstanceId).isEqualTo("caseInstanceId2")

        assertThat(stage.remoteService.getVariablesLocal(instance.id)).containsKeys("VAR1", "VAR3", "VAR4", "VAR5", "VAR6")
        assertThat(stage.remoteService.getVariablesLocal(instance.id, listOf("VAR1", "VAR2", "VAR6"))).containsKeys("VAR1", "VAR6")
        assertThat(stage.remoteService.getVariableLocal(instance.id, "VAR1")).isEqualTo("NEW VALUE")

        assertThat(stage.remoteService.getVariablesLocalTyped(instance.id)).containsKeys("VAR1", "VAR3", "VAR4", "VAR5", "VAR6")
        assertThat(stage.remoteService.getVariablesLocalTyped(instance.id, listOf("VAR1", "VAR2", "VAR6"), true)).containsKeys("VAR1", "VAR6")

        assertThat(stage.remoteService.getVariableLocalTyped<StringValue>(instance.id, "VAR6")).isEqualTo(stringValue("typed"))

        assertThat(stage.localService.getVariablesLocal(instance.id)).containsKeys("VAR1", "VAR3", "VAR4", "VAR5", "VAR6")
        assertThat(stage.localService.getVariableLocal(instance.id, "VAR1")).isEqualTo("NEW VALUE")
        assertThat(stage.localService.getVariableLocal(instance.id, "VAR3")).isEqualTo("VAL3")
        assertThat(stage.localService.getVariableLocal(instance.id, "VAR4")).isEqualTo("My object value")
        assertThat(stage.localService.getVariableLocal(instance.id, "VAR5")).isEqualTo("untyped")
        assertThat(stage.localService.getVariableLocalTyped<StringValue>(instance.id, "VAR6")).isEqualTo(stringValue("typed"))
      }
  }
}