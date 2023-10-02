/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import java.util.List;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;

public final class MatchPredicateTestValues<F> extends AbstractPredicateTestValues<F> {
	private final List<F> values;

	public MatchPredicateTestValues(FieldTypeDescriptor<F, ?> fieldType) {
		this( fieldType, fieldType.getUniquelyMatchableValues() );
	}

	public MatchPredicateTestValues(FieldTypeDescriptor<F, ?> fieldType, List<F> values) {
		super( fieldType );
		this.values = values;
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
