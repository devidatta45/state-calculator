package com.stackstate.models

import cats.implicits._

sealed trait StateEnum {
  def value: String

  def rank: Int
}

case object NoData extends StateEnum {
  override val value: String = "no_data"

  override def rank: Int = 1
}

case object Clear extends StateEnum {
  override val value: String = "clear"

  override def rank: Int = 2
}

case object Warning extends StateEnum {
  override val value: String = "warning"

  override def rank: Int = 3
}

case object Alert extends StateEnum {
  override val value: String = "alert"

  override def rank: Int = 4
}

object StateEnum {
  def getStateEnumById(id: String): Either[DomainError, StateEnum] = {
    id match {
      case "no_data" => NoData.asRight
      case "clear" => Clear.asRight
      case "warning" => Warning.asRight
      case "alert" => Alert.asRight
      case state => InvalidState(s"$state is invalid").asLeft
    }
  }

  def compare(stateEnum1: StateEnum, stateEnum2: StateEnum): StateEnum = {
    if (stateEnum1.rank > stateEnum2.rank) stateEnum1 else stateEnum2
  }
}

case class ComponentState(id: String,
                          own_state: StateEnum,
                          derived_state: StateEnum,
                          check_states: Map[String, String],
                          depends_on: Vector[String],
                          dependency_of: Vector[String]
                         )

object ComponentState {
  def validateAndConvert(components: Vector[ComponentStateRequest]): Either[DomainError, Vector[ComponentState]] = {
    components.traverse { component =>
      for {
        ownState <- StateEnum.getStateEnumById(component.own_state)
        derivedState <- StateEnum.getStateEnumById(component.derived_state)
      } yield ComponentState(component.id, ownState, derivedState, component.check_states, component.depends_on, component.dependency_of)
    }
  }

  def from(components: Vector[ComponentState]): Vector[ComponentStateRequest] = {
    components.map { component =>
      ComponentStateRequest(
        component.id,
        component.own_state.value,
        component.derived_state.value,
        component.check_states,
        component.depends_on,
        component.dependency_of
      )
    }
  }
}

case class ComponentStateRequest(id: String, own_state: String, derived_state: String,
                                 check_states: Map[String, String],
                                 depends_on: Vector[String] = Vector.empty,
                                 dependency_of: Vector[String] = Vector.empty)

case class ComponentStateWithTimestamp(state: ComponentState, latestTimestamp: Option[Int] = None)

case class ComponentEventRequest(timestamp: String, component: String, check_state: String, state: String)

case class ComponentEvent(timestamp: String, component: String, check_state: String, state: StateEnum)

case class Components(components: Vector[ComponentStateRequest])

case class ComponentStates(graph: Components)

case class ComponentEvents(events: Vector[ComponentEventRequest])

object ComponentEvent {
  def validateAndConvert(componentEventRequests: Vector[ComponentEventRequest]): Either[DomainError, Vector[ComponentEvent]] = {
    componentEventRequests.traverse { componentEventRequest =>
      for {
        state <- StateEnum.getStateEnumById(componentEventRequest.state)
      } yield ComponentEvent(componentEventRequest.timestamp, componentEventRequest.component, componentEventRequest.check_state, state)
    }
  }
}