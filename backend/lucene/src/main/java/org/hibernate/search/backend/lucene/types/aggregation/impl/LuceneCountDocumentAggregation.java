/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.logging.impl.QueryLog;
import org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl.CountDocuemntsCollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationExtractContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationRequestContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.LuceneSearchAggregation;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexNodeContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.aggregation.spi.CountDocumentAggregationBuilder;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementFactory;

public class LuceneCountDocumentAggregation implements LuceneSearchAggregation<Long> {

	private final Set<String> indexNames;

	private LuceneCountDocumentAggregation(Builder builder) {
		this.indexNames = builder.scope.hibernateSearchIndexNames();
	}

	public static Factory factory() {
		return Factory.INSTANCE;
	}

	protected static class Factory
			implements
			SearchQueryElementFactory<CountDocumentAggregationBuilder.TypeSelector,
					LuceneSearchIndexScope<?>,
					LuceneSearchIndexNodeContext> {

		private static final Factory INSTANCE = new Factory();

		private Factory() {
		}

		@Override
		public CountDocumentAggregationBuilder.TypeSelector create(LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexNodeContext node) {
			return new TypeSelector( scope, node );
		}

		@Override
		public void checkCompatibleWith(SearchQueryElementFactory<?, ?, ?> other) {
			if ( !getClass().equals( other.getClass() ) ) {
				throw QueryLog.INSTANCE.differentImplementationClassForQueryElement( getClass(), other.getClass() );
			}
		}
	}

	private record TypeSelector(LuceneSearchIndexScope<?> scope, LuceneSearchIndexNodeContext node)
			implements CountDocumentAggregationBuilder.TypeSelector {

		@Override
		public CountDocumentAggregationBuilder builder() {
			return new LuceneCountDocumentAggregation.Builder( scope );
		}
	}

	@Override
	public Extractor<Long> request(AggregationRequestContext context) {
		CountDocuemntsCollectorFactory collectorFactory = CountDocuemntsCollectorFactory.instance();
		var collectorKey = collectorFactory.getCollectorKey();

		context.requireCollector( collectorFactory );
		return new CountDocumentsExtractor( collectorKey );
	}

	private record CountDocumentsExtractor(CollectorKey<?, Long> collectorKey) implements Extractor<Long> {

		@Override
		public Long extract(AggregationExtractContext context) {
			return context.getCollectorResults( collectorKey );
		}
	}

	@Override
	public Set<String> indexNames() {
		return indexNames;
	}

	private record Builder(LuceneSearchIndexScope<?> scope) implements CountDocumentAggregationBuilder {

		@Override
		public LuceneCountDocumentAggregation build() {
			return new LuceneCountDocumentAggregation( this );
		}
	}
}
