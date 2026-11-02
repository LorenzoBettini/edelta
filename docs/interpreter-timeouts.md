# Interpreter timeouts

`EdeltaDefaultInterpreter` evaluates each `modifyEcore` operation on the calling
thread. `EdeltaInterpreterTimeoutGuard` interrupts that thread when the operation
exceeds `edelta.interpreter.timeout` (5000 ms by default; zero disables the timeout),
including while it calls blocking Java code. The interpreter reports a timeout warning for a cancelled
evaluation or an `InterruptedException` returned by the evaluator.

Each operation creates its guard through the protected `createTimeoutGuard()`
factory and owns it through a try-with-resources block. The default factory uses
the configured interpreter timeout. The try-with-resources block cancels the
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
compilation after timeout cleanup. `EdeltaInterpreterTest` captures real guards
through the factory with automatic timeout delivery disabled, then explicitly
fires their callbacks after successful and exceptional evaluation. This checks
that neither path leaves a guard that can interrupt later work, without waiting
for a timer. Both tests close captured guards and clear interrupts in `finally`
so assertion failures cannot affect subsequent tests.

The interpreted timeout fixtures in `EdeltaInterpreterTest` and the external Java
fixture `MyCustomEdeltaWithTimeout` block on a `CountDownLatch` instead of sleeping.
The interpreter timeout interrupts the wait; its 30-second bound lets the tests
fail if timeout delivery is broken. The tests check the warning location and that
model changes following the wait do not execute.

For asynchronous outline linking, `EdeltaOutlineWithEditorLinkerTest` processes
SWT events until the expected node is selected. A three-second display timer
bounds the wait and is cancelled in `finally`. Checking the expected label also
prevents a stale, nonempty selection from satisfying the wait.
