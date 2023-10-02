/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import java.util.List;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;

public final class KnnPredicateTestValues<F> extends AbstractPredicateTestValues<F> {
	private final List<F> values;

	public KnnPredicateTestValues(FieldTypeDescriptor<F, ?> fieldType) {
		super( fieldType );
		this.values = fieldType.getUniquelyMatchableValues();
	}

	@Override
	public F fieldValue(int docOrdinal) {
		return values.get( docOrdinal );
	}

	public F matchingArg(int docOrdinal) {
		return fieldValue( docOrdinal );
	}

	@Override
	public int size() {
		return values.size();
	}
}
