/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import java.util.List;
import java.util.Locale;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.AnalyzedStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.util.common.data.Range;

public final class RangePredicateTestValues<F> extends AbstractPredicateTestValues<F> {
	private final List<F> values;

	public RangePredicateTestValues(FieldTypeDescriptor<F, ?> fieldType) {
		super( fieldType );
		this.values = fieldType.getAscendingUniqueTermValues().getSingle();
	}

	@Override
	public F fieldValue(int docOrdinal) {
		return values.get( docOrdinal );
	}

	@SuppressWarnings("unchecked")
	public F matchingValue(int docOrdinal) {
		F valueToMatch = fieldValue( docOrdinal );
		if ( AnalyzedStringFieldTypeDescriptor.INSTANCE.equals( fieldType )
				&& !TckConfiguration.get().getBackendFeatures()
						.normalizesStringArgumentToRangePredicateForAnalyzedStringField() ) {
			// The backend doesn't normalize missing value replacements automatically, we have to do it ourselves
			// TODO HSEARCH-3959 Remove this once all backends correctly normalize range predicate arguments
			valueToMatch = (F) ( (String) valueToMatch ).toLowerCase( Locale.ROOT );
		}
		return valueToMatch;
	}

	public Range<F> matchingRange(int docOrdinal) {
		F matchingValue = matchingValue( docOrdinal );
		return Range.between( matchingValue, matchingValue );
	}

	@Override
	public int size() {
		return values.size();
	}
}
