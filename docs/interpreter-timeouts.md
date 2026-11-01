# Interpreter timeouts

`EdeltaDefaultInterpreter` evaluates each `modifyEcore` operation on the calling
thread. `EdeltaInterpreterTimeoutGuard` interrupts that thread when the operation
exceeds `edelta.interpreter.timeout` (5000 ms by default; zero disables the timeout),
including while it calls blocking Java code. The interpreter reports a timeout warning for a cancelled
evaluation or an `InterruptedException` returned by the evaluator.

Each operation owns its guard through a try-with-resources block. This cancels the
timer even if evaluation throws an unchecked exception. Closing the guard and
delivering its interrupt use the same monitor: once the guard is closed, a timer
callback cannot interrupt subsequent work. Closing also consumes a timeout's
remaining interrupt flag, while preserving an interrupt already set on entry or
an external interrupt when the guard did not fire.

This matters for compilation tests as well as editor/build threads. An orphaned
timeout thread can interrupt later Java compilation. JDT's parser silently aborts
when its thread is interrupted; Xtext's in-memory compiler can then report
`ClassNotFoundException` for a generated class with an empty `PROBLEMS` section,
even though the generated Java is correct.

`EdeltaInterpreterTimeoutGuardTest` covers blocking timeout delivery, cancellation,
a callback arriving after close, interrupt-state handling, and successful Java
compilation after timeout cleanup. `EdeltaInterpreterTest` checks that an exception
escaping evaluation cannot leave a timer that interrupts later work, alongside
its existing interpreted and external-Java timeout tests.
