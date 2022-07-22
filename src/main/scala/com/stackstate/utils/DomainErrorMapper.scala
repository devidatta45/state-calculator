package com.stackstate.utils

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives
import com.stackstate.models.{DomainError, InvalidComponent, InvalidState, UnknownError}

object DomainErrorMapper extends Directives with JsonSupport {
  val domainErrorMapper: ErrorMapper[DomainError] = {
    case InvalidComponent(message, code) =>
      HttpResponse(StatusCodes.BadRequest, entity = json4sToHttpEntityMarshaller(GenericErrorResponseBody(code, message)))

    case InvalidState(message, code) =>
      HttpResponse(StatusCodes.BadRequest, entity = json4sToHttpEntityMarshaller(GenericErrorResponseBody(code, message)))

    case UnknownError(message, code) =>
      HttpResponse(StatusCodes.InternalServerError, entity = json4sToHttpEntityMarshaller(GenericErrorResponseBody(code, message)))
  }

  case class GenericErrorResponseBody(code: String, message: String, errorDetails: Option[String] = None)

}
