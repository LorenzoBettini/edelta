package edelta.tests.input

class Inputs {
	def referenceToMetamodel() {
		'''
			metamodel "foo"
		'''
	}

	def referenceToEClass() {
		'''
			metamodel "foo"
			
			eclass FooClass
		'''
	}

	def useImportedJavaTypes() {
		'''
		package foo;
		
		import java.util.List
		
		def bar(List<String> s) {
			s.empty
		}
		'''
	}

	def operationWithInferredReturnType() {
		'''
		package foo;
		
		def bar(String s) {
			s.empty
		}
		'''
	}

	def operationWithReturnType() {
		'''
		package foo;
		
		def bar(String s) : boolean {
			s.empty
		}
		'''
	}

	def operationAccessingLib() {
		'''
		package foo;
		
		def bar(String s) {
			newEClass(s)
		}
		'''
	}

	def operationNewEClassWithInitializer() {
		'''
		package foo;
		
		def bar(String s) {
			newEClass(s) [
				ESuperTypes += newEClass("Base")
			]
		}
		'''
	}

	def programWithMainExpression() {
		'''
		package foo;
		
		def bar(String s) {
			newEClass(s)
		}
		
		println(bar("foo"))
		'''
	}

	def eclassifierExpression() {
		'''
		package foo;
		
		metamodel "foo"
		
		eclassifier FooClass
		println(eclassifier FooClass)
		'''
	}

	def eclassExpression() {
		'''
		package foo;
		
		metamodel "foo"
		
		eclass FooClass
		println(eclass FooClass)
		'''
	}

	def edatatypeExpression() {
		'''
		package foo;
		
		metamodel "foo"
		
		edatatype FooDataType
		println(edatatype FooDataType)
		'''
	}

	def efeatureExpressions() {
		'''
		package foo;
		
		metamodel "foo"
		
		efeature myAttribute
		println(efeature myAttribute)
		eattribute myAttribute
		println(eattribute myAttribute)
		ereference myReference
		println(ereference myReference)
		val ref = ereference myReference
		'''
	}

	def createEClass() {
		'''
			metamodel "foo"
			
			createEClass NewClass in foo
		'''
	}

	def referenceToCreatedEClass() {
		'''
			metamodel "foo"
			
			createEClass NewClass in foo
			eclass NewClass
		'''
	}
}
