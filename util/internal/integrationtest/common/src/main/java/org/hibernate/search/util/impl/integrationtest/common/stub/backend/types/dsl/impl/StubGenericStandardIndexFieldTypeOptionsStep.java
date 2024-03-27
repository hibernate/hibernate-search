/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.dsl.impl;

class StubGenericStandardIndexFieldTypeOptionsStep<F>
		extends AbstractStubStandardIndexFieldTypeOptionsStep<StubGenericStandardIndexFieldTypeOptionsStep<F>, F> {

	StubGenericStandardIndexFieldTypeOptionsStep(Class<F> inputType) {
		super( inputType );
	}

	@Override
	StubGenericStandardIndexFieldTypeOptionsStep<F> thisAsS() {
		return this;
	}

}
