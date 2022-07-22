package com.stackstate.utils

import akka.http.scaladsl.marshalling.{Marshaller, Marshalling, PredefinedToResponseMarshallers}
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.server.{RequestContext, Route, RouteResult}
import zio.ZIO

import scala.concurrent.{Future, Promise}
import scala.language.implicitConversions

// Normal utility class for route support with ZIO
trait ErrorMapper[E] {
  def toHttpResponse(e: E): HttpResponse
}

trait ZioToRoutes[T] extends zio.Runtime[T] {

  implicit def errorMarshaller[E: ErrorMapper]: Marshaller[E, HttpResponse] =
    Marshaller { implicit ec =>
      a =>
        PredefinedToResponseMarshallers.fromResponse(implicitly[ErrorMapper[E]].toHttpResponse(a))
    }

  implicit def zioMarshaller[A, E](implicit m1: Marshaller[A, HttpResponse], m2: Marshaller[E, HttpResponse]): Marshaller[ZIO[T, E, A], HttpResponse] =
    Marshaller { implicit ec =>
      a => {
        val r = a.foldM(
          e => ZIO.fromFuture(_ => m2(e)),
          a => ZIO.fromFuture(_ => m1(a))
        )

        val p = Promise[List[Marshalling[HttpResponse]]]()

        unsafeRunAsync(r) { exit =>
          exit.fold(e => p.failure(e.squash), s => p.success(s))
        }

        p.future
      }
    }

  implicit def zioRoute[E: ErrorMapper](z: ZIO[T, E, Route]): Route = ctx => {
    val p = Promise[RouteResult]()

    val f = z.fold(
      e => (ctx: RequestContext) => Future.successful(Complete(implicitly[ErrorMapper[E]].toHttpResponse(e))),
      a => a
    )

    unsafeRunAsync(f) { exit =>
      exit.fold(e => p.failure(e.squash), s => p.completeWith(s.apply(ctx)))
    }

    p.future
  }
}
