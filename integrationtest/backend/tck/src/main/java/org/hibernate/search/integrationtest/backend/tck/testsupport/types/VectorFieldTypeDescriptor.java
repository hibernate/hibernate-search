/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.util.List;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.VectorFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableValues;

public abstract class VectorFieldTypeDescriptor<F>
		extends FieldTypeDescriptor<F, VectorFieldTypeOptionsStep<?, F>> {

	protected final int size;

	protected VectorFieldTypeDescriptor(Class<F> javaType, String uniqueName, int size) {
		super( javaType, uniqueName );
		this.size = size;
	}

	public abstract VectorFieldTypeDescriptor<F> withDimension(int dimension);

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

	public int vectorSize() {
		return size;
	}

	public abstract F sampleVector();

	public abstract List<F> unitLengthVectors();

}
