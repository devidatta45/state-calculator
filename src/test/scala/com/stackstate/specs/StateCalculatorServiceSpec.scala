package com.stackstate.specs

import com.stackstate.models._
import com.stackstate.services.StateCalculatorService
import com.stackstate.storage.{InMemoryStateStorage, StateRepository}
import com.stackstate.utils.Generators._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, EitherValues, OptionValues, Suite}
import zio.Runtime
import zio.internal.Platform

class StateCalculatorServiceSpec extends AnyFlatSpec
  with Suite with should.Matchers
  with BeforeAndAfterEach
  with OptionValues with EitherValues with BeforeAndAfterAll {

  object TestEnvironment extends StateRepository {
    override val stateStorage: StateRepository.Service = InMemoryStateStorage.stateStorage
  }

  val myRuntime: Runtime[StateRepository] = Runtime(TestEnvironment, Platform.default)

  override def beforeEach(): Unit = {
    super.beforeEach()
    myRuntime.unsafeRun(StateRepository.removeAll)
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    myRuntime.unsafeRun(StateRepository.removeAll)
  }

  behavior of "StateCalculatorService"

  it should "save graph correctly" in {
    val componentState = componentStateGen.sample.value
    val states = Vector(componentState)
    myRuntime.unsafeRun(StateCalculatorService.service.saveGraph(states)) shouldBe()
  }

  it should "fetch graph correctly" in {
    val componentState = componentStateGen.sample.value
    myRuntime.unsafeRun(StateCalculatorService.service.saveGraph(Vector(componentState))) shouldBe()
    myRuntime.unsafeRun(StateCalculatorService.service.fetchLatestGraph) should contain(componentState)
  }

  it should "apply events correctly to the graph" in {
    val componentState1 = generateInitialComponentState("db", Some("hardware"), Some("app"), Vector("CPU load", "Ram"))
    val componentState2 = generateInitialComponentState("app", Some("db"), None, Vector("CPU load", "Ram"))
    val componentState3 = generateInitialComponentState("hardware", None, Some("db"), Vector("CPU load", "Ram"))
    val states = Vector(componentState1, componentState2, componentState3)
    myRuntime.unsafeRun(StateCalculatorService.service.saveGraph(states)) shouldBe()

    val event1 = generateComponentEvent(1, "db", "CPU load", Warning)
    val event2 = generateComponentEvent(2, "app", "CPU load", Clear)
    val events = Vector(event1, event2)

    val changedComponentState1 = componentState1.copy(own_state = Warning, derived_state = Warning,
      check_states = Map("CPU load" -> Warning.value, "Ram" -> NoData.value))

    val changedComponentState2 = componentState2.copy(own_state = Clear, derived_state = Warning,
      check_states = Map("CPU load" -> Clear.value, "Ram" -> NoData.value))

    val changedComponentState3 = componentState3.copy(own_state = NoData, derived_state = Warning,
      check_states = Map("CPU load" -> NoData.value, "Ram" -> NoData.value))

    val changedEvents = myRuntime.unsafeRun(StateCalculatorService.service.applyGraphEvents(events))
    changedEvents should contain(changedComponentState1)
    changedEvents should contain(changedComponentState2)
    changedEvents should contain(changedComponentState3)
  }

  it should "apply or not apply events based on the latest timestamp applied" in {
    val componentState1 = generateInitialComponentState("db", None, Some("app"), Vector("CPU load", "Ram"))
    val componentState2 = generateInitialComponentState("app", Some("db"), None, Vector("CPU load", "Ram"))
    val states = Vector(componentState1, componentState2)
    myRuntime.unsafeRun(StateCalculatorService.service.saveGraph(states)) shouldBe()

    val event = generateComponentEvent(20, "app", "CPU load", Clear)
    val events = Vector(event)

    val changedComponentState1 = componentState1.copy(own_state = NoData, derived_state = Clear,
      check_states = Map("CPU load" -> NoData.value, "Ram" -> NoData.value))
    val changedComponentState2 = componentState2.copy(own_state = Clear, derived_state = Clear,
      check_states = Map("CPU load" -> Clear.value, "Ram" -> NoData.value))
    val changedEvents = myRuntime.unsafeRun(StateCalculatorService.service.applyGraphEvents(events))
    changedEvents should contain(changedComponentState1)
    changedEvents should contain(changedComponentState2)

    val oldEvent = generateComponentEvent(10, "app", "CPU load", Warning)
    val unChangedEvents = myRuntime.unsafeRun(StateCalculatorService.service.applyGraphEvents(Vector(oldEvent)))
    unChangedEvents should contain(changedComponentState1)
    unChangedEvents should contain(changedComponentState2)

    val newEvent = generateComponentEvent(30, "app", "CPU load", Warning)
    val appliedComponentState1 = componentState1.copy(own_state = NoData, derived_state = Warning,
      check_states = Map("CPU load" -> NoData.value, "Ram" -> NoData.value))
    val appliedComponentState2 = componentState2.copy(own_state = Warning, derived_state = Warning,
      check_states = Map("CPU load" -> Warning.value, "Ram" -> NoData.value))
    val appliedEvents = myRuntime.unsafeRun(StateCalculatorService.service.applyGraphEvents(Vector(newEvent)))
    appliedEvents should contain(appliedComponentState1)
    appliedEvents should contain(appliedComponentState2)
  }

  it should "apply events correctly to both dependents and dependency ofs in the graph" in {
    val componentState1 = generateInitialComponentState("db", Some("hardware"), Some("app"), Vector("CPU load", "Ram"))
    val componentState2 = generateInitialComponentState("app", Some("db"), None, Vector("CPU load", "Ram"))
    val componentState3 = generateInitialComponentState("hardware", None, Some("db"), Vector("CPU load", "Ram"))
    val states = Vector(componentState1, componentState2, componentState3)
    myRuntime.unsafeRun(StateCalculatorService.service.saveGraph(states)) shouldBe()

    val event1 = generateComponentEvent(1, "db", "CPU load", Warning)
    val event2 = generateComponentEvent(2, "app", "CPU load", Clear)
    val events = Vector(event1, event2)

    val changedComponentState1 = componentState1.copy(own_state = Warning, derived_state = Warning,
      check_states = Map("CPU load" -> Warning.value, "Ram" -> NoData.value))

    val changedComponentState2 = componentState2.copy(own_state = Clear, derived_state = Warning,
      check_states = Map("CPU load" -> Clear.value, "Ram" -> NoData.value))

    val changedComponentState3 = componentState3.copy(own_state = NoData, derived_state = Warning,
      check_states = Map("CPU load" -> NoData.value, "Ram" -> NoData.value))

    val changedEvents = myRuntime.unsafeRun(StateCalculatorService.service.applyGraphEvents(events))
    changedEvents should contain(changedComponentState1)
    changedEvents should contain(changedComponentState2)
    changedEvents should contain(changedComponentState3)

    val event3 = generateComponentEvent(3, "hardware", "CPU load", Alert)

    val changedEvents1 = myRuntime.unsafeRun(StateCalculatorService.service.applyGraphEvents(Vector(event3)))

    val changedComponentState4 = componentState1.copy(own_state = Warning, derived_state = Alert,
      check_states = Map("CPU load" -> Warning.value, "Ram" -> NoData.value))

    val changedComponentState5 = componentState2.copy(own_state = Clear, derived_state = Alert,
      check_states = Map("CPU load" -> Clear.value, "Ram" -> NoData.value))

    val changedComponentState6 = componentState3.copy(own_state = Alert, derived_state = Alert,
      check_states = Map("CPU load" -> Alert.value, "Ram" -> NoData.value))

    changedEvents1 should contain(changedComponentState4)
    changedEvents1 should contain(changedComponentState6)
    changedEvents1 should contain(changedComponentState5)
  }

  it should "fail in case of invalid component in the event" in {
    val componentState1 = generateInitialComponentState("db", None, Some("app"), Vector("CPU load", "Ram"))
    val componentState2 = generateInitialComponentState("app", Some("db"), None, Vector("CPU load", "Ram"))
    val states = Vector(componentState1, componentState2)
    myRuntime.unsafeRun(StateCalculatorService.service.saveGraph(states)) shouldBe()

    val event = generateComponentEvent(1, "new-component", "CPU load", Warning)
    val events = Vector(event)

    myRuntime.unsafeRun(StateCalculatorService.service.
      applyGraphEvents(events).either).left.value shouldBe InvalidComponent("the component is not valid")
  }
}
