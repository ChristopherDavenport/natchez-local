package io.chrisdavenport.natchezlocal

import io.chrisdavenport.fiberlocal.FiberLocal
import natchez._
import cats.effect.kernel._
import cats.syntax.all._
import cats.~>

object TraceLocal {

  def fromFiberLocal[F[_]: MonadCancelThrow](local: FiberLocal[F, Span[F]]): natchez.Trace[F] = 
    new natchez.Trace[F] {
      def put(fields: (String, TraceValue)*): F[Unit] =
        local.get.flatMap(_.put(fields: _*))

      def log(fields: (String, natchez.TraceValue)*): F[Unit] =
        local.get.flatMap(_.log(fields: _*))

      def log(event: String): F[Unit] =
        local.get.flatMap(_.log(event))

      def attachError(err: Throwable, fields: (String, natchez.TraceValue)*): F[Unit] =
        local.get.flatMap(_.attachError(err, fields: _*))

      def kernel: F[Kernel] =
        local.get.flatMap(_.kernel)

      def spanR(name: String, options: Span.Options = Span.Options.Defaults): Resource[F,F ~> F] =
        Resource.eval(local.get).flatMap { parent =>
          parent.span(name, options).map { child =>
            new (F ~> F) {
              def apply[A](fa: F[A]): F[A] =
                Resource.make(local.set(child))(_ => local.set(parent)).use(_ => fa)
              }
          }
        }

      def span[A](name: String, options: natchez.Span.Options = Span.Options.Defaults)(k: F[A]): F[A] =
        local.get.flatMap { parent =>
          parent.span(name, options).flatMap { child =>
            Resource.make(local.set(child))(_ => local.set(parent))
          } .use { _ => k }
        }

      def traceId: F[Option[String]] =
        local.get.flatMap(_.traceId)

      def traceUri =
        local.get.flatMap(_.traceUri)
    }

}