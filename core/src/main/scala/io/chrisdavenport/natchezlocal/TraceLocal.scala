package io.chrisdavenport.natchezlocal

import io.chrisdavenport.fiberlocal.FiberLocal
import natchez._
import cats.effect.kernel._
import cats.syntax.all._

object TraceLocal {

  def fromFiberLocal[F[_]: MonadCancelThrow](local: FiberLocal[F, Span[F]]): natchez.Trace[F] = 
    new natchez.Trace[F] {
      def put(fields: (String, TraceValue)*): F[Unit] =
        local.get.flatMap(_.put(fields: _*))

      def kernel: F[Kernel] =
        local.get.flatMap(_.kernel)

      def span[A](name: String)(k: F[A]): F[A] =
        local.get.flatMap { parent =>
          parent.span(name).flatMap { child =>
            Resource.make(local.set(child))(_ => local.set(parent))
          } .use { _ => k }
        }

      def traceId: F[Option[String]] =
        local.get.flatMap(_.traceId)

      def traceUri =
        local.get.flatMap(_.traceUri)
    }

}