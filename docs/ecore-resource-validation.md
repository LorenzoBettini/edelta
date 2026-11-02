# Ecore resource validation

`EdeltaResourceUtils.getEPackage(Resource)` returns null when a resource is empty
or its first root object is not an `EPackage`.

`EdeltaDependencyAnalizer.analyzeEPackage(String)` requires that first package to
select the metamodel for dependency analysis.
`EdeltaVersionMigrator.loadEcore(String)` and
`loadEcore(String, InputStream)` require it to register the metamodel by nsURI.
These entry points reject resources without a first-root `EPackage` with an
`IllegalArgumentException` that identifies the resource URI.

`EdeltaDependencyAnalyzerTest` and `EdeltaVersionMigratorTest` cover empty
resources and resources rooted in an `EClass`. The migrator tests cover both
file and stream loading. Existing dependency and version migration tests cover
valid metamodels and model migration.
