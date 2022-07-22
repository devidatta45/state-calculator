package com.stackstate.specs

import com.stackstate.models._
import org.scalatest.{OptionValues, Suite}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import com.stackstate.utils.Generators._
import com.stackstate.utils.StateCalculatorHelper

class StateCalculatorHelperSpec extends AnyFlatSpec with Suite with should.Matchers with OptionValues {

  behavior of "StateCalculatorHelper"

  it should "compare own state correctly" in {
    val componentState = componentStateGen.sample.value.copy(own_state = NoData)
    val componentStateWithTimestamp = ComponentStateWithTimestamp(componentState)
    val componentEvent = componentEventGen.sample.value.copy(state = Warning)

    StateCalculatorHelper.resolveOwnState(componentStateWithTimestamp, componentEvent) shouldBe Warning
  }

  it should "compare derived state correctly" in {
    val componentState = componentStateGen.sample.value.copy(derived_state = Alert)
    val componentStateWithTimestamp = ComponentStateWithTimestamp(componentState)

    StateCalculatorHelper.resolveDerivedState(componentStateWithTimestamp, Warning) shouldBe Alert
  }

  it should "compare dependent state correctly" in {
    val componentState = componentStateGen.sample.value.copy(own_state = Clear, derived_state = Warning)
    val componentStateWithTimestamp = ComponentStateWithTimestamp(componentState)
    StateCalculatorHelper.resolveDependentDerivedState(componentStateWithTimestamp, componentState) shouldBe Warning
  }
}
