package io.chrisdavenport.natchezlocal

import cats.effect.{Trace => _, _}
import io.chrisdavenport.fiberlocal.FiberLocal
import munit.CatsEffectSuite
import natchez.Span
import natchez.noop.NoopSpan

class TraceLocalSpec extends CatsEffectSuite {

  test("TraceLocal.spanR should restore the FiberLocal span and not the span in place when the FunctionK is created") {
    val originalSpan: Span[IO] = NoopSpan[IO]()
    val replacementSpan: Span[IO] = NoopSpan[IO]()

    IOLocal(originalSpan)
      .map(FiberLocal.fromIOLocal[IO, Span[IO]])
      .flatMap { fiberLocal =>
        val trace = TraceLocal.fromFiberLocal(fiberLocal)
        trace.spanR("test").use { fk =>
          for {
            spanBeforeRunningFk <- fiberLocal.get
            _ <- fiberLocal.set(replacementSpan)
            _ <- fk(IO.unit)
            spanAfterRunningFk <- fiberLocal.get
          } yield {
            assert(originalSpan eq spanBeforeRunningFk)
            assert(replacementSpan eq spanAfterRunningFk)
          }
        }
      }
  }

}
