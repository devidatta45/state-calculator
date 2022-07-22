package com.stackstate.utils

import com.stackstate.models._
import org.scalacheck.Gen

object Generators {

  def componentStateGen: Gen[ComponentState] = for {
    id <- Gen.alphaUpperStr
    ownState <- Gen.oneOf(Seq(NoData, Clear, Warning, Alert))
    derivedState <- Gen.oneOf(Seq(NoData, Clear, Warning, Alert))
  } yield ComponentState(id, ownState, derivedState, Map.empty, Vector.empty, Vector.empty)

  def componentEventGen: Gen[ComponentEvent] = for {
    component <- Gen.alphaUpperStr
    checkState <- Gen.identifier
    timeStamp <- Gen.chooseNum(1, 10)
    state <- Gen.oneOf(Seq(NoData, Clear, Warning, Alert))
  } yield ComponentEvent(timeStamp.toString, component, checkState, state)

  def generateInitialComponentState(id: String, dependent: Option[String], dependencyOf: Option[String], checkStateComponents: Vector[String]) = {
    val checkStates = checkStateComponents.map(state => state -> NoData.value).toMap
    val dependents = dependent.map(d => Vector(d)).getOrElse(Vector.empty)
    val dependencyOfs = dependencyOf.map(d => Vector(d)).getOrElse(Vector.empty)
    ComponentState(id, NoData, NoData, checkStates, dependents, dependencyOfs)
  }

  def generateComponentEvent(timestamp: Int, component: String, checkState: String, state: StateEnum) = {
    ComponentEvent(timestamp.toString, component, checkState, state)
  }
}
