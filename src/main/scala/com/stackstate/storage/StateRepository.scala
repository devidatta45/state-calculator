package com.stackstate.storage

import com.stackstate.models.{DomainError, ComponentState, ComponentStateWithTimestamp}
import zio.{IO, ZIO}

trait StateRepository {
  val stateStorage: StateRepository.Service
}

object StateRepository {
  trait Service {
    def saveAll(states: Vector[(String, ComponentStateWithTimestamp)]): IO[DomainError, Unit]

    def getAll: IO[DomainError, Vector[ComponentState]]

    def getAllMapping: IO[DomainError, Map[String, ComponentStateWithTimestamp]]

    def removeAll: IO[DomainError, Unit]
  }

  def saveAll(states: Vector[(String, ComponentStateWithTimestamp)]): ZIO[StateRepository, DomainError, Unit] = {
    ZIO.accessM[StateRepository](_.stateStorage.saveAll(states))
  }

  def getAll: ZIO[StateRepository, DomainError, Vector[ComponentState]] = {
    ZIO.accessM[StateRepository](_.stateStorage.getAll)
  }

  def getAllMapping: ZIO[StateRepository, DomainError, Map[String, ComponentStateWithTimestamp]] = {
    ZIO.accessM[StateRepository](_.stateStorage.getAllMapping)
  }

  def removeAll: ZIO[StateRepository, DomainError, Unit] = {
    ZIO.accessM[StateRepository](_.stateStorage.removeAll)
  }
}