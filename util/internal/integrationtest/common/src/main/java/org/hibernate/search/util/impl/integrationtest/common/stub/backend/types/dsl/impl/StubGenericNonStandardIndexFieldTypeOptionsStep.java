package org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.dsl.impl;

class StubGenericNonStandardIndexFieldTypeOptionsStep<F>
		extends
		AbstractStubIndexFieldTypeOptionsStep<StubGenericNonStandardIndexFieldTypeOptionsStep<F>, F> {

	StubGenericNonStandardIndexFieldTypeOptionsStep(Class<F> inputType) {
		super( inputType );
	}

	@Override
	StubGenericNonStandardIndexFieldTypeOptionsStep<F> thisAsS() {
		return this;
	}

}
