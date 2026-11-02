# Interpreter receiver bindings

`EdeltaDefaultInterpreter.configureContextForJavaThis` binds the runtime receiver
under `this`. When `EdeltaJvmModelHelper.findJvmGenericType` finds an inferred JVM
type for the current program, it also binds that receiver under the type's simple
name. This supports reflective access and calls to operations in the sources.

A program without an inferred JVM type still gets its `this` binding. The
interpreter skips the class-name alias instead of dereferencing the missing type.

`EdeltaInterpreterTest.testConfigureJavaThisWithoutInferredType` covers a program
with no JVM associations. `testConfigureJavaThisWithInferredType` checks that both
names resolve to the same receiver when the inferred type is available.
