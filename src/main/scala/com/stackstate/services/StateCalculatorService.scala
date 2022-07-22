package com.stackstate.services

import com.stackstate.models._
import com.stackstate.storage.StateRepository
import cats.implicits._
import zio.{IO, ZIO}
import com.stackstate.utils.StateCalculatorHelper._

trait StateCalculatorService {

  def saveGraph(states: Vector[ComponentState]): ZIO[StateRepository, DomainError, Unit]

  def fetchLatestGraph: ZIO[StateRepository, DomainError, Vector[ComponentState]]

  def applyGraphEvents(events: Vector[ComponentEvent]): ZIO[StateRepository, DomainError, Vector[ComponentState]]
}

object StateCalculatorService {

  val service: StateCalculatorService = new StateCalculatorService {
    override def saveGraph(states: Vector[ComponentState]): ZIO[StateRepository, DomainError, Unit] = {
      val stateList = states.map(state => state.id -> ComponentStateWithTimestamp(state))
      ZIO.accessM[StateRepository](_.stateStorage.saveAll(stateList))
    }

    override def fetchLatestGraph: ZIO[StateRepository, DomainError, Vector[ComponentState]] = {
      ZIO.accessM[StateRepository](_.stateStorage.getAll)
    }

    override def applyGraphEvents(events: Vector[ComponentEvent]): ZIO[StateRepository, DomainError, Vector[ComponentState]] = {
      for {
        _ <- ZIO.foreach(events)(event => applyEvent(event))
        latestStates <- ZIO.accessM[StateRepository](_.stateStorage.getAll)
      } yield latestStates
    }

    private def applyEvent(event: ComponentEvent): ZIO[StateRepository, DomainError, Unit] = {
      for {
        stateWithTimestamp <- ZIO.accessM[StateRepository](_.stateStorage.getAllMapping)
        retrievedStateWithTimestamp <- ZIO.fromEither(Either.fromOption(stateWithTimestamp.get(event.component),
          InvalidComponent("the component is not valid")))
        finalState = retrievedStateWithTimestamp.latestTimestamp match {
          case Some(timestamp) if timestamp > event.timestamp.toInt => None
          case _ =>
            val changedMap = retrievedStateWithTimestamp.state.check_states + (event.check_state -> event.state.value)
            val changedState = resolveOwnState(retrievedStateWithTimestamp, event)
            val derivedState = resolveDerivedState(retrievedStateWithTimestamp, changedState)
            val finalState = retrievedStateWithTimestamp.state.copy(check_states = changedMap,
              own_state = changedState, derived_state = derivedState)
            Some(finalState)
        }
        _ <- finalState match {
          case Some(state) =>
            ZIO.fromEither(triggerDependentsRecursively(retrievedStateWithTimestamp.state.depends_on ++ retrievedStateWithTimestamp.state.dependency_of,
              stateWithTimestamp, state, event).map(mappings => mappings.toVector :+ (state.id,
              ComponentStateWithTimestamp(state, Some(event.timestamp.toInt))))
            ).flatMap(mappings => ZIO.accessM[StateRepository](_.stateStorage.saveAll(mappings)))
          case None => ZIO.unit
        }
      } yield ()
    }

    private def triggerDependentsRecursively(dependents: Vector[String],
                                             currentStateMap: Map[String, ComponentStateWithTimestamp],
                                             changedState: ComponentState,
                                             incomingEvent: ComponentEvent): Either[DomainError, Map[String, ComponentStateWithTimestamp]] = {
      if (dependents.isEmpty) {
        currentStateMap.asRight
      } else {
        for {
          changedMap <- changeDependentState(dependents.head, currentStateMap, changedState, incomingEvent)
          changedDependentState = changedMap(dependents.head)
          result <- triggerDependentsRecursively(dependents.tail, changedMap, changedDependentState.state, incomingEvent)
        } yield result
      }
    }

    private def changeDependentState(dependent: String,
                                     currentStateMap: Map[String, ComponentStateWithTimestamp],
                                     changedState: ComponentState,
                                     incomingEvent: ComponentEvent): Either[DomainError, Map[String, ComponentStateWithTimestamp]] = {
      for {
        dependState <- Either.fromOption(currentStateMap.get(dependent),
          InvalidComponent("the dependent component is not valid"))
        result <- dependState.latestTimestamp match {
          case Some(timestamp) if timestamp > incomingEvent.timestamp.toInt =>
            currentStateMap.asRight
          case _ =>
            val derivedState = resolveDependentDerivedState(dependState, changedState)
            val changedDependentState = ComponentStateWithTimestamp(dependState.state.copy(derived_state = derivedState),
              Some(incomingEvent.timestamp.toInt))
            val changedMap = currentStateMap + (dependent -> changedDependentState)

            val otherDependents = changedDependentState.state.dependency_of.filterNot(_ == changedState.id)
            triggerDependentsRecursively(changedDependentState.state.depends_on ++ otherDependents,
              changedMap, changedDependentState.state, incomingEvent)
        }
      } yield result
    }
  }
}
