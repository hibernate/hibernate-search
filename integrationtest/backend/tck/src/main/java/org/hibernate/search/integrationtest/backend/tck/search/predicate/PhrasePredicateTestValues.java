/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import java.util.List;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;

public final class PhrasePredicateTestValues extends AbstractPredicateTestValues<String> {
	private final List<String> values;

	public PhrasePredicateTestValues(FieldTypeDescriptor<String, ?> fieldType) {
		super( fieldType );
		this.values = fieldType.getUniquelyMatchableValues();
	}

	@Override
	public String fieldValue(int docOrdinal) {
		return values.get( docOrdinal );
	}

	public String matchingArg(int docOrdinal) {
		return fieldValue( docOrdinal );
	}

	@Override
	public int size() {
		return values.size();
	}
}
