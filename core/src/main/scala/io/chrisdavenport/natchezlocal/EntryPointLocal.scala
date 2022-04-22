package io.chrisdavenport.natchezlocal

import io.chrisdavenport.fiberlocal._
import natchez._
import cats.effect.kernel._

trait EntrypointLocal[F[_]]{
  def continueOrElseRoot(name: String, kernel: Kernel): Resource[F, Trace[F]]
  def continue(name: String, kernel: Kernel): Resource[F, Trace[F]]
  def root(name: String): Resource[F, Trace[F]]
}

object EntrypointLocal {
  def fromEntrypoint[F[_]: GenFiberLocal: MonadCancelThrow](entrypoint: EntryPoint[F]): EntrypointLocal[F] = {
    new EntrypointLocal[F]{
      def continueOrElseRoot(name: String, kernel: Kernel): Resource[F,Trace[F]] = 
        entrypoint.continueOrElseRoot(name,kernel)
          .evalMap(GenFiberLocal[F].local(_))
          .map(TraceLocal.fromFiberLocal(_))
      
      def continue(name: String, kernel: Kernel): Resource[F,Trace[F]] = 
        entrypoint.continue(name, kernel)
          .evalMap(GenFiberLocal[F].local(_))
          .map(TraceLocal.fromFiberLocal(_))
      
      def root(name: String): Resource[F,Trace[F]] = 
        entrypoint.root(name)
          .evalMap(GenFiberLocal[F].local(_))
          .map(TraceLocal.fromFiberLocal(_))
    }
  }
}