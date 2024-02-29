/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	public StubVectorFieldTypeOptionsStep<F> magnitude(float magnitude) {
		builder.modifier( b -> b.magnitude( magnitude ) );
		return this;
	}

	@Override
	StubVectorFieldTypeOptionsStep<F> thisAsS() {
		return this;
	}
}
