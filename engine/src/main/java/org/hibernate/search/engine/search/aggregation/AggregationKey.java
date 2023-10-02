/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation;

import java.util.Objects;

import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.util.common.impl.Contracts;

/**
 * A key allowing to retrieve an aggregation from the search result.
 *
 * @param <A> The type of result for this aggregation.
 *
 * @see SearchResult#aggregation(AggregationKey)
 */
public final class AggregationKey<A> {

	/**
	 * @param name The name of the aggregation. All root aggregation names must be unique within a single query.
	 * @param <A> The type of result for this aggregation.
	 * @return A new aggregation key.
	 */
	public static <A> AggregationKey<A> of(String name) {
		Contracts.assertNotNullNorEmpty( name, "name" );
		return new AggregationKey<>( name );
	}

	private final String name;

	private AggregationKey(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + name + "]";
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		AggregationKey<?> that = (AggregationKey<?>) o;
		return Objects.equals( name, that.name );
	}

	@Override
	public int hashCode() {
		return Objects.hash( name );
	}

	/**
	 * @return The name passed to {@link #of(String)}.
	 */
	public String name() {
		return name;
	}
}
