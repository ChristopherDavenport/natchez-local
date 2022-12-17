package io.chrisdavenport.natchezlocal

import io.chrisdavenport.fiberlocal.FiberLocal
import natchez._
import cats.effect.kernel._
import cats.effect.syntax.all._
import cats.syntax.all._
import cats._

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

      override def log(fields: (String, TraceValue)*): F[Unit] =
        local.get.flatMap(_.log(fields: _*))

      override def log(event: String): F[Unit] =
        local.get.flatMap(_.log(event))

      override def attachError(err: Throwable): F[Unit] =
        local.get.flatMap(_.attachError(err))

      override def spanR(name: String, kernel: Option[Kernel]): Resource[F, F ~> F] =
        local.get.toResource
          .flatMap { parent =>
            kernel.fold(parent.span(name))(parent.span(name, _))
              .map { child =>
                new (F ~> F) {
                  override def apply[A](fa: F[A]): F[A] =
                    local.get.flatMap { old =>
                      local.set(child).bracket { _ =>
                        fa.onError {
                          case e => child.attachError(e)
                        }
                      } { _ => local.set(old) }
                    }
                }
              }
          }

      override def span[A](name: String, kernel: Kernel)(k: F[A]): F[A] =
        local.get.flatMap { parent =>
          parent.span(name, kernel).use { child =>
            (local.set(child) >> k).guarantee(local.set(parent))
          }
        }
    }
}
