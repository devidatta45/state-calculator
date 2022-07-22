package com.stackstate.specs

import com.stackstate.models.ComponentStateWithTimestamp
import com.stackstate.storage.{InMemoryStateStorage, StateRepository}
import com.stackstate.utils.Generators._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, OptionValues, Suite}
import zio.Runtime
import zio.internal.Platform

class StateRepositorySpec extends AnyFlatSpec
  with Suite with should.Matchers
  with BeforeAndAfterEach with OptionValues with BeforeAndAfterAll {

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

  behavior of "StateRepository"

  it should "insert all state mappings correctly" in {
    val componentState = componentStateGen.sample.value
    val stateMapping = Vector((componentState.id, ComponentStateWithTimestamp(componentState)))
    myRuntime.unsafeRun(StateRepository.saveAll(stateMapping)) shouldBe()
  }

  it should "get all states correctly" in {
    val componentState = componentStateGen.sample.value
    val stateMapping = Vector((componentState.id, ComponentStateWithTimestamp(componentState)))
    myRuntime.unsafeRun(StateRepository.saveAll(stateMapping)) shouldBe()
    myRuntime.unsafeRun(StateRepository.getAll) should contain(componentState)
  }

  it should "get state mappings correctly" in {
    val componentState = componentStateGen.sample.value
    val stateMapping = Vector((componentState.id, ComponentStateWithTimestamp(componentState)))
    myRuntime.unsafeRun(StateRepository.saveAll(stateMapping)) shouldBe()
    myRuntime.unsafeRun(StateRepository.getAllMapping) should contain(componentState.id -> ComponentStateWithTimestamp(componentState))
  }
}
