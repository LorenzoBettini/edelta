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
}
