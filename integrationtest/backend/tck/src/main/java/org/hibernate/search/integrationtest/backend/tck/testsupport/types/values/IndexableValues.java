/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types.values;

import static java.util.Arrays.asList;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class IndexableValues<F> {

	private final List<F> single = Collections.unmodifiableList( createSingle() );

	private final List<List<F>> multi = Collections.unmodifiableList( makeMulti( single ) );

	public List<F> getSingle() {
		return single;
	}

	public List<List<F>> getMulti() {
		return multi;
	}

	protected abstract List<F> createSingle();

	protected static <T> List<List<T>> makeMulti(List<T> single) {
		if ( single.size() < 3 ) {
			return valuesThatWontBeUsed();
		}
		return asList(
				asList( single.get( 0 ), single.get( 1 ), single.get( 2 ) ),
				asList( single.get( 2 ), single.get( 1 ), single.get( 0 ) ),
				asList( single.get( 0 ), single.get( 0 ), single.get( 0 ) )
		);
	}

	// When this is called, we expect the values to be indexed, but not actually tested
	// Used for Boolean types in particular
	protected static <F> List<List<F>> valuesThatWontBeUsed() {
		return Stream.generate( Collections::<F>emptyList ).limit( 10 ).collect( Collectors.toList() );
	}
}
