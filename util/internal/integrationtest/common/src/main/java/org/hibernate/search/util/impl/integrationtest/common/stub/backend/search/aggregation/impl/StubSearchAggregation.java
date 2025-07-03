/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.aggregation.impl;

import java.util.Map;
import java.util.function.Function;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.spi.RangeAggregationBuilder;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.aggregation.spi.WithParametersAggregationBuilder;
import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.AbstractStubSearchQueryElementFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexNodeContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexScope;

public class StubSearchAggregation<A> implements SearchAggregation<A> {

	private StubSearchAggregation() {
	}

	public void simulateBuild() {
		// No-op, just simulates a call on this object
	}

	public static class TermsFactory extends AbstractStubSearchQueryElementFactory<TermsAggregationBuilder.TypeSelector> {
		@Override
		public TermsAggregationBuilder.TypeSelector create(StubSearchIndexScope<?> scope,
				StubSearchIndexNodeContext node) {
			return new TermsTypeSelector();
		}
	}

	public static class RangeFactory extends AbstractStubSearchQueryElementFactory<RangeAggregationBuilder.TypeSelector> {
		@Override
		public RangeAggregationBuilder.TypeSelector create(StubSearchIndexScope<?> scope,
				StubSearchIndexNodeContext node) {
			return new RangeTypeSelector();
		}
	}

	public static class TermsTypeSelector implements TermsAggregationBuilder.TypeSelector {
		@Override
		public <V> TermsBuilder<V, Long> type(Class<V> expectedType, ValueModel valueModel) {
			return new TermsBuilder<>();
		}
	}

	public static class RangeTypeSelector implements RangeAggregationBuilder.TypeSelector {
		@Override
		public <V> RangeBuilder<V, Long> type(Class<V> expectedType, ValueModel valueModel) {
			return new RangeBuilder<>();
		}
	}

	static class TermsBuilder<K, V> implements TermsAggregationBuilder<K, V> {

		@Override
		public void orderByCountDescending() {
			// No-op
		}

		@Override
		public void orderByCountAscending() {
			// No-op
		}

		@Override
		public void orderByTermDescending() {
			// No-op
		}

		@Override
		public void orderByTermAscending() {
			// No-op
		}

		@Override
		public void minDocumentCount(int minDocumentCount) {
			// No-op
		}

		@Override
		public void maxTermCount(int maxTermCount) {
			// No-op
		}

		@Override
		public <T> TermsAggregationBuilder<K, T> withValue(SearchAggregation<T> aggregation) {
			return new TermsBuilder<>();
		}

		@Override
		public void filter(SearchPredicate filter) {
			// No-op
		}

		@Override
		public SearchAggregation<Map<K, V>> build() {
			return new StubSearchAggregation<>();
		}
	}

	static class RangeBuilder<K, A> implements RangeAggregationBuilder<K, A> {

		@Override
		public void range(Range<? extends K> range) {
			// No-op
		}

		@Override
		public void filter(SearchPredicate filter) {
			// No-op
		}

		@Override
		public <T> RangeAggregationBuilder<K, T> withValue(SearchAggregation<T> aggregation) {
			return new RangeBuilder<>();
		}

		@Override
		public SearchAggregation<Map<Range<K>, A>> build() {
			return new StubSearchAggregation<>();
		}
	}

	public static class StubWithParametersAggregationBuilder<T> implements WithParametersAggregationBuilder<T> {
		@Override
		public void creator(Function<? super NamedValues, ? extends AggregationFinalStep<T>> aggregationCreator) {

		}

		@Override
		public SearchAggregation<T> build() {
			return new StubSearchAggregation<>();
		}
	}
}
