package com.stackstate

import akka.actor.ActorSystem
import akka.http.scaladsl.server.{Directives, Route}
import akka.util.Timeout
import com.stackstate.routes.StateCalculatorRoutes
import com.stackstate.storage.{InMemoryStateStorage, StateRepository}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

trait AppContext extends Directives {

  implicit def executionContext: ExecutionContext = system.dispatcher
  implicit def system: ActorSystem
  implicit def timeout: Timeout = Duration.fromNanos(100000)
  lazy val config = system.settings.config

  // Live environment for the application with all required dependency
  object LiveEnvironment extends StateRepository {
    override val stateStorage: StateRepository.Service = InMemoryStateStorage.stateStorage
  }

  lazy val routes: Route = new StateCalculatorRoutes(LiveEnvironment).routes
}
