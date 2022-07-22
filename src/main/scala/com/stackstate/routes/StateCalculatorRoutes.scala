package com.stackstate.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives
import com.stackstate.models.{ComponentEvent, ComponentEvents, ComponentState, ComponentStates, DomainError}
import com.stackstate.services.StateCalculatorService
import com.stackstate.storage.StateRepository
import com.stackstate.utils.{DomainErrorMapper, ErrorMapper, JsonSupport, ZioToRoutes}
import zio.ZIO
import zio.internal.Platform

import scala.concurrent.ExecutionContext

class StateCalculatorRoutes(env: StateRepository)(
  implicit executionContext: ExecutionContext,
  system: ActorSystem,
) extends ZioToRoutes[StateRepository] with Directives with JsonSupport {
  override def environment: StateRepository = env

  override def platform: Platform = Platform.default

  private lazy val service = StateCalculatorService.service

  implicit val errorMapper: ErrorMapper[DomainError] = DomainErrorMapper.domainErrorMapper

  val routes = pathPrefix("api" / "topology") {
    post {
      entity(as[ComponentStates]) { componentGraph =>
        for {
          validatedComponents <- ZIO.fromEither(ComponentState.validateAndConvert(componentGraph.graph.components))
          _ <- service.saveGraph(validatedComponents)
        } yield complete(
          StatusCodes.Created,
          "saved"
        )
      }
    } ~ get {
      for {
        changedState <- service.fetchLatestGraph
      } yield complete(
        StatusCodes.OK,
        ComponentState.from(changedState)
      )
    }
  } ~ pathPrefix("api" / "events") {
    post {
      entity(as[ComponentEvents]) { eventWrapper =>
        for {
          validatedEvents <- ZIO.fromEither(ComponentEvent.validateAndConvert(eventWrapper.events))
          changedState <- service.applyGraphEvents(validatedEvents)
        } yield complete(
          StatusCodes.OK,
          ComponentState.from(changedState)
        )
      }
    }
  }
}
