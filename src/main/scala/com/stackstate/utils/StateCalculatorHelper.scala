package com.stackstate.utils

import com.stackstate.models._

object StateCalculatorHelper {
  def resolveOwnState(stateWithTimestamp: ComponentStateWithTimestamp, incomingEvent: ComponentEvent): StateEnum = {
    StateEnum.compare(stateWithTimestamp.state.own_state, incomingEvent.state)
  }

  def resolveDerivedState(stateWithTimestamp: ComponentStateWithTimestamp, currentOwnState: StateEnum): StateEnum = {
    StateEnum.compare(stateWithTimestamp.state.derived_state, currentOwnState)
  }

  def resolveDependentDerivedState(stateWithTimestamp: ComponentStateWithTimestamp, currentState: ComponentState): StateEnum = {
    StateEnum.compare(stateWithTimestamp.state.own_state, currentState.derived_state)
  }
}
