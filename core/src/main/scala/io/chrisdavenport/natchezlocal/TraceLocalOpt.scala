/*
package io.chrisdavenport.natchezlocal

import io.chrisdavenport.fiberlocal.FiberLocal
import natchez._
import cats.effect.kernel._
import cats.syntax.all._
import io.chrisdavenport.fiberlocal.GenFiberLocal


object TraceLocalOpt {

  trait EntryPointLocal[F[_]]{
    def continueOrElseRoot(name: String, kernel: Kernel): Resource[F, Unit]
    def continue(name: String, kernel: Kernel): Resource[F, Unit]
    def root(name: String): Resource[F, Unit]
    
    def continueOrElseRootTrace(name: String): Resource[F, Unit]
    def continueTrace(name: String): Resource[F, Unit]
  }

  def globalTrace[F[_]: MonadCancelThrow: GenFiberLocal](entrypoint: EntryPoint[F], warn: String => F[Unit]): F[(Trace[F], EntryPointLocal[F])] = 
    GenFiberLocal[F].local(Option.empty[Span[F]]).map{local => 
      val trace = fromOptionFiberLocal(entrypoint, local, warn)
      val epL = entryPointLocal(entrypoint, local, trace)
      (trace, epL)
    }

  def fromOptionFiberLocal[F[_]: MonadCancelThrow](ep: EntryPoint[F], local: FiberLocal[F, Option[Span[F]]], warn: String => F[Unit]): natchez.Trace[F] = 
    new natchez.Trace[F] {
      val putWarn = "put invoked with field but no span present in FiberLocal, make sure to use EntryPointLocal or span before put"
      def put(fields: (String, TraceValue)*): F[Unit] =
        local.get.flatMap{
          case Some(span) => span.put(fields: _*)
          case None => warn(putWarn)
        }

      def kernel: F[Kernel] =
        local.get.flatMap{
          case Some(span) => span.kernel
          case None => Kernel(Map()).pure[F]
        }

      def span[A](name: String)(k: F[A]): F[A] =
        local.get.flatMap { 
          case Some(parent) =>
            parent.span(name).flatMap { child =>
              Resource.make(local.set(child.some))(_ => local.set(parent.some))
            } .use { _ => k }
          case None => ep.root(name).flatMap(child => 
            Resource.make(local.set(child.some))(_ => local.set(None))
          ).use{_ => k}
        }

      def traceId: F[Option[String]] =
        local.get.flatMap{
          case Some(span) => span.traceId
          case None => Option.empty[String].pure[F]
        }

      def traceUri =
        local.get.flatMap{
          case Some(span) => span.traceUri
          case None => Option.empty.pure[F]
        }
    }

  def entryPointLocal[F[_]: MonadCancelThrow](ep: EntryPoint[F], local: FiberLocal[F, Option[Span[F]]], trace: Trace[F]): EntryPointLocal[F] = {
    new EntryPointLocal[F] {
      def continueOrElseRoot(name: String, kernel: Kernel): Resource[F,Unit] = 
        ep.continueOrElseRoot(name, kernel).flatMap(child => 
          Resource.make(local.get <* local.set(child.some))(local.set(_))
        ).void
      
      def continue(name: String, kernel: Kernel): Resource[F,Unit] = {
        ep.continue(name, kernel).flatMap(child => 
          Resource.make(local.get <* local.set(child.some))(local.set(_))
        ).void
      }
      
      def root(name: String): Resource[F,Unit] = {
        ep.root(name).flatMap(child => 
          Resource.make(local.get <* local.set(child.some))(local.set(_))
        ).void
      }
      
      def continueOrElseRootTrace(name: String): Resource[F,Unit] = 
        Resource.eval(trace.kernel).flatMap(continueOrElseRoot(name, _))      
      def continueTrace(name: String): Resource[F,Unit] = 
        Resource.eval(trace.kernel).flatMap(continue(name, _))
    }
  }
}
*/