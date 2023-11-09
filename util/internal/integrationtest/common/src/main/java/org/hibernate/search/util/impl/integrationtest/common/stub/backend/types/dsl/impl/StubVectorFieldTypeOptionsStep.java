/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.dsl.impl;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.engine.backend.types.dsl.VectorFieldTypeOptionsStep;

class StubVectorFieldTypeOptionsStep<F>
		extends AbstractStubIndexFieldTypeOptionsStep<StubVectorFieldTypeOptionsStep<F>, F>
		implements VectorFieldTypeOptionsStep<StubVectorFieldTypeOptionsStep<F>, F> {

	StubVectorFieldTypeOptionsStep(Class<F> klass) {
		super( klass );
	}

	@Override
	public StubVectorFieldTypeOptionsStep<F> dimension(int dimension) {
		builder.modifier( b -> b.dimension( dimension ) );
		return this;
	}

	@Override
	public StubVectorFieldTypeOptionsStep<F> projectable(Projectable projectable) {
		builder.modifier( b -> b.projectable( projectable ) );
		return this;
	}

	@Override
	public StubVectorFieldTypeOptionsStep<F> searchable(Searchable searchable) {
		builder.modifier( b -> b.searchable( searchable ) );
		return this;
	}

	@Override
	public StubVectorFieldTypeOptionsStep<F> vectorSimilarity(VectorSimilarity vectorSimilarity) {
		builder.modifier( b -> b.vectorSimilarity( vectorSimilarity ) );
		return this;
	}

	@Override
	public StubVectorFieldTypeOptionsStep<F> beamWidth(int beamWidth) {
		builder.modifier( b -> b.beamWidth( beamWidth ) );
		return this;
	}

	@Override
	public StubVectorFieldTypeOptionsStep<F> maxConnections(int maxConnections) {
		builder.modifier( b -> b.maxConnections( maxConnections ) );
		return this;
	}

	@Override
	public StubVectorFieldTypeOptionsStep<F> indexNullAs(F indexNullAs) {
		builder.modifier( b -> b.indexNullAs( indexNullAs ) );
		return this;
	}


	@Override
	StubVectorFieldTypeOptionsStep<F> thisAsS() {
		return this;
	}
}
