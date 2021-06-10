/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.engine.search.aggregation.spi.AggregationTypeKeys;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys;
import org.hibernate.search.engine.search.sort.spi.SortTypeKeys;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.aggregation.impl.StubSearchAggregation;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.AbstractStubSearchQueryElementFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.predicate.impl.StubSearchPredicate;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubDistanceToFieldSearchProjection;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubFieldSearchProjection;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.sort.impl.StubSearchSort;

public final class StubSearchQueryElementFactories {
	private StubSearchQueryElementFactories() {
	}

	@SuppressWarnings("unchecked")
	public static <T> AbstractStubSearchQueryElementFactory<T> get(SearchQueryElementTypeKey<T> key) {
		if ( key.toString().startsWith( "predicate:named:" ) ) {
			return (AbstractStubSearchQueryElementFactory<T>) new StubSearchPredicate.Factory();
		}
		else {
			return (AbstractStubSearchQueryElementFactory<T>) StubSearchQueryElementFactories.ALL.get( key );
		}
	}

	private static final Map<SearchQueryElementTypeKey<?>, AbstractStubSearchQueryElementFactory<?>> ALL = new HashMap<>();

	static {
		StubSearchQueryElementFactories.stubFactories(
				new StubSearchPredicate.Factory(),
				PredicateTypeKeys.NESTED,
				PredicateTypeKeys.MATCH,
				PredicateTypeKeys.RANGE,
				PredicateTypeKeys.EXISTS,
				PredicateTypeKeys.PHRASE,
				PredicateTypeKeys.WILDCARD,
				PredicateTypeKeys.REGEXP,
				PredicateTypeKeys.TERMS,
				PredicateTypeKeys.SPATIAL_WITHIN_CIRCLE,
				PredicateTypeKeys.SPATIAL_WITHIN_POLYGON,
				PredicateTypeKeys.SPATIAL_WITHIN_BOUNDING_BOX
		);
		StubSearchQueryElementFactories.stubFactories(
				new StubSearchSort.Factory(),
				SortTypeKeys.FIELD,
				SortTypeKeys.DISTANCE
		);
		StubSearchQueryElementFactories.factory( new StubFieldSearchProjection.Factory(), ProjectionTypeKeys.FIELD );
		StubSearchQueryElementFactories.factory(
				new StubDistanceToFieldSearchProjection.Factory(), ProjectionTypeKeys.DISTANCE );
		StubSearchQueryElementFactories.factory(
				new StubSearchAggregation.TermsFactory(), AggregationTypeKeys.TERMS );
		StubSearchQueryElementFactories.factory(
				new StubSearchAggregation.RangeFactory(), AggregationTypeKeys.RANGE );
	}

	@SafeVarargs
	private static <T> void stubFactories(AbstractStubSearchQueryElementFactory<T> factory,
			SearchQueryElementTypeKey<? super T>... keys) {
		for ( SearchQueryElementTypeKey<? super T> key : keys ) {
			factory( factory, key );
		}
	}

	private static <T> void factory(AbstractStubSearchQueryElementFactory<? extends T> factory,
			SearchQueryElementTypeKey<T> key) {
		ALL.put( key, factory );
	}
}
