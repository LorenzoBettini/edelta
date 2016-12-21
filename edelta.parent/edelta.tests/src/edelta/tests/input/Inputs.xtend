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
}
