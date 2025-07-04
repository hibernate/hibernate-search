/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.hibernate.search.backend.elasticsearch.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Sometimes we need to pass something we created while building up the json in one of the "doRequest" methods
 * in the aggregation build up to the "later" steps e.g. to when we create the extractor.
 */
@Incubating
public final class AggregationRequestBuildingContextContext implements AggregationRequestContext {
	private final AggregationRequestContext aggregationRequestContext;
	private final Map<Key<?>, Object> buildingContext = new HashMap<>();

	public AggregationRequestBuildingContextContext(AggregationRequestContext aggregationRequestContext) {
		this.aggregationRequestContext = aggregationRequestContext;
	}

	public <T> T get(Key<T> key) {
		Object value = buildingContext.get( key );
		return key.cast( value );
	}

	public void add(Key<?> key, Object value) {
		buildingContext.put( key, value );
	}

	public AggregationRequestContext rootAggregationRequestContext() {
		return aggregationRequestContext;
	}

	@Override
	public PredicateRequestContext getRootPredicateContext() {
		return aggregationRequestContext.getRootPredicateContext();
	}

	public static <V> Key<V> buildingContextKey(String name) {
		return new Key<>( name );
	}

	public static class Key<V> {

		private final String name;

		private Key(String name) {
			this.name = name;
		}

		@SuppressWarnings("unchecked")
		private V cast(Object value) {
			return (V) value;
		}

		@Override
		public boolean equals(Object o) {
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Key<?> key = (Key<?>) o;
			return Objects.equals( name, key.name );
		}

		@Override
		public int hashCode() {
			return Objects.hashCode( name );
		}
	}
}
