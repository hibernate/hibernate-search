/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.dsl.impl;

import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.engine.backend.types.dsl.VectorFieldTypeOptionsStep;

class StubVectorFieldTypeOptionsStep<F>
		extends AbstractStubSearchableProjectableIndexFieldTypeOptionsStep<StubVectorFieldTypeOptionsStep<F>, F>
		implements VectorFieldTypeOptionsStep<StubVectorFieldTypeOptionsStep<F>, F> {

	StubVectorFieldTypeOptionsStep(Class<F> klass) {
		super( klass );
	}

	@Override
	public StubVectorFieldTypeOptionsStep<F> vectorSimilarity(VectorSimilarity vectorSimilarity) {
		builder.modifier( b -> b.vectorSimilarity( vectorSimilarity ) );
		return this;
	}

	@Override
	public StubVectorFieldTypeOptionsStep<F> efConstruction(int efConstruction) {
		builder.modifier( b -> b.efConstruction( efConstruction ) );
		return this;
	}

	@Override
	public StubVectorFieldTypeOptionsStep<F> m(int m) {
		builder.modifier( b -> b.m( m ) );
		return this;
	}

	@Override
	public StubVectorFieldTypeOptionsStep<F> dimension(int dimension) {
		builder.modifier( b -> b.dimension( dimension ) );
		return this;
	}

	@Override
	StubVectorFieldTypeOptionsStep<F> thisAsS() {
		return this;
	}
}
