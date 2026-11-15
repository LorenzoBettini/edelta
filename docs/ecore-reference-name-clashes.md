# Ecore reference name clashes

When a root EPackage and a feature have the same name, an unqualified
`ecoreref(name)` refers to the root package. A root package has no longer name
that can distinguish it from the feature. Use the fully qualified Ecore path to
refer to the feature:

```edelta
metamodel "foo"

modifyEcore example epackage foo {
	ecoreref(FooClass).addNewEAttribute("foo", null)
	val rootPackage = ecoreref(foo)
	val feature = ecoreref(foo.FooClass.foo)
}
```

Content assist offers both `foo` and `foo.FooClass.foo` in this situation.
Other duplicate short names continue to produce an ambiguity diagnostic.

`EdeltaScopeProvider` gives root packages priority during linking, and
`EdeltaDefaultInterpreter` preserves that choice when it relinks references
after metamodel changes. `EdeltaScopeProviderTest`, `EdeltaInterpreterTest`,
`EdeltaCompilerTest`, and `EdeltaContentAssistTest` cover the behavior.
