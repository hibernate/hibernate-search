/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import java.util.List;
import java.util.Locale;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;

public final class CommonQueryStringPredicateTestValues<T> extends AbstractPredicateTestValues<T> {
	private final List<T> values;

	public CommonQueryStringPredicateTestValues(FieldTypeDescriptor<T, ?> fieldType) {
		super( fieldType );
		this.values = fieldType.getUniquelyMatchableValues();
	}

	@Override
	public T fieldValue(int docOrdinal) {
		return values.get( docOrdinal );
	}

	public String matchingArg(int docOrdinal) {
		// Using phrase queries, because that's the easiest way to achieve
		// simple, unique matches with the query string predicates (simple/regular).
		// Other types of queries are tested separately in *PredicateSpecificsIT.
		return String.format( Locale.ROOT, "\"%s\"",
				TckConfiguration.get().getBackendFeatures().formatForQueryStringPredicate(
						fieldType,
						fieldValue( docOrdinal )
				)
		);
	}

	@Override
	public int size() {
		return values.size();
	}
}
