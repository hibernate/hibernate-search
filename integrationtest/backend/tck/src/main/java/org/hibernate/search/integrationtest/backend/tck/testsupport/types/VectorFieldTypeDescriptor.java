/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.util.List;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.VectorFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableValues;

public abstract class VectorFieldTypeDescriptor<F>
		extends FieldTypeDescriptor<F, VectorFieldTypeOptionsStep<?, F>> {

	protected VectorFieldTypeDescriptor(Class<F> javaType) {
		super( javaType );
	}

	protected VectorFieldTypeDescriptor(Class<F> javaType, String uniqueName) {
		super( javaType, uniqueName );
	}

	@Override
	public abstract VectorFieldTypeOptionsStep<?, F> configure(IndexFieldTypeFactory fieldContext);

	@Override
	protected IndexableValues<F> createIndexableValues() {
		return new IndexableValues<F>() {
			@Override
			protected List<F> createSingle() {
				return createUniquelyMatchableValues();
			}
		};
	}

	@Override
	protected AscendingUniqueTermValues<F> createAscendingUniqueTermValues() {
		return null;
	}

	@Override
	public boolean isFieldSortSupported() {
		return false;
	}

	@Override
	public boolean isFieldAggregationSupported() {
		return false;
	}

	@Override
	public boolean isMultivaluable() {
		return false;
	}

	public abstract int vectorSize();

	public abstract F sampleVector(int dimension);
}
