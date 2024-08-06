/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.AnalyzedStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;

public final class PrefixPredicateTestValues extends AbstractPredicateTestValues<String> {
	private final List<String> values;

	public PrefixPredicateTestValues(FieldTypeDescriptor<String, ?> fieldType) {
		super( fieldType );
		List<String> single = fieldType.getAscendingUniqueTermValues().getSingle();
		List<String> values = new ArrayList<>( single );
		for ( String value : single ) {
			Set<String> samePrefix = single.stream()
					.filter( s -> !s.equals( value ) )
					.filter( s -> s.toLowerCase( Locale.ROOT ).startsWith( value.toLowerCase( Locale.ROOT ) ) )
					.collect( Collectors.toSet() );
			values.removeAll( samePrefix );
		}
		this.values = values;
	}

	@Override
	public String fieldValue(int docOrdinal) {
		return values.get( docOrdinal );
	}

	public String matchingArg(int docOrdinal) {
		String value = fieldValue( docOrdinal );
		value = value.substring( 0, value.length() - 3 );
		if ( AnalyzedStringFieldTypeDescriptor.INSTANCE.equals( fieldType )
				&& !TckConfiguration.get().getBackendFeatures()
						.normalizesStringArgumentToPrefixPredicateForAnalyzedStringField() ) {
			// The backend doesn't normalize missing value replacements automatically, we have to do it ourselves
			value = value.toLowerCase( Locale.ROOT );
		}
		return value;
	}

	@Override
	public int size() {
		return values.size();
	}
}
