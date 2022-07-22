package com.stackstate.storage

import com.stackstate.models.{DomainError, ComponentState, ComponentStateWithTimestamp}
import com.stackstate.storage.InMemoryStateStorage.StateStore
import zio.Runtime.default
import zio.{IO, Ref}

trait InMemoryStateStorage extends StateRepository {

  override val stateStorage: StateRepository.Service = new StateRepository.Service {
    val ref: Ref[StateStore] = default.unsafeRun(Ref.make(StateStore(Map())))

    override def saveAll(states: Vector[(String, ComponentStateWithTimestamp)]): IO[DomainError, Unit] = {
      ref.modify(_.saveAll(states))
    }

    override def getAll: IO[DomainError, Vector[ComponentState]] = {
      ref.modify(_.getAll)
    }

    override def getAllMapping: IO[DomainError, Map[String, ComponentStateWithTimestamp]] = {
      ref.modify(_.getAllMapping)
    }

    override def removeAll: IO[DomainError, Unit] = {
      ref.modify(_.removeAll)
    }
  }
}

object InMemoryStateStorage extends InMemoryStateStorage {
  final case class StateStore(storage: Map[String, ComponentStateWithTimestamp]) {
    def saveAll(states: Vector[(String, ComponentStateWithTimestamp)]): (Unit, StateStore) = {
      ((), copy(storage = storage ++ states))
    }

    def removeAll: (Unit, StateStore) = {
      ((), copy(storage = Map.empty[String, ComponentStateWithTimestamp]))
    }

    def getAll: (Vector[ComponentState], StateStore) = {
      (storage.values.map(_.state).toVector, this)
    }

    def getAllMapping: (Map[String, ComponentStateWithTimestamp], StateStore) = (storage, this)
  }
}