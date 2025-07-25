/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.util.Set;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.logging.impl.QueryLog;
import org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl.CountDistinctValuesCollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl.CountDocuemntsCollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl.CountValuesCollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.JoiningLongMultiValuesSource;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationExtractContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationRequestContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.LuceneSearchAggregation;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexNodeContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.engine.search.aggregation.spi.CountAggregationBuilder;
import org.hibernate.search.engine.search.aggregation.spi.CountDocumentAggregationBuilder;
import org.hibernate.search.engine.search.aggregation.spi.CountValuesAggregationBuilder;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementFactory;

public abstract class LuceneCountAggregation {
	private LuceneCountAggregation() {
	}

	public static Factory factory() {
		return Factory.INSTANCE;
	}

	protected static class Factory
			implements
			SearchQueryElementFactory<CountAggregationBuilder.TypeSelector,
					LuceneSearchIndexScope<?>,
					LuceneSearchIndexNodeContext> {

		private static final Factory INSTANCE = new Factory();

		private Factory() {
		}

		@Override
		public CountAggregationBuilder.TypeSelector create(LuceneSearchIndexScope<?> scope,
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

	protected record TypeSelector(LuceneSearchIndexScope<?> scope, LuceneSearchIndexNodeContext node)
			implements CountAggregationBuilder.TypeSelector {

		@Override
		public CountValuesAggregationBuilder values() {
			return new LuceneCountValuesAggregation.Builder( scope, node.toValueField() );
		}

		@Override
		public CountDocumentAggregationBuilder documents() {
			return new LuceneCountDocumentAggregation.Builder( scope );
		}
	}

	private static class LuceneCountValuesAggregation extends AbstractLuceneMetricNumericLongAggregation {
		private final Function<JoiningLongMultiValuesSource, CollectorFactory<?, Long, ?>> collectorFactorySupplier;

		LuceneCountValuesAggregation(Builder builder) {
			super( builder );
			collectorFactorySupplier = builder.collectorFactorySupplier;
		}

		@Override
		void fillCollectors(JoiningLongMultiValuesSource source, AggregationRequestContext context) {
			var collectorFactory = collectorFactorySupplier.apply( source );
			collectorKey = collectorFactory.getCollectorKey();
			context.requireCollector( collectorFactory );
		}

		protected static class Builder extends AbstractBuilder<Long> implements CountValuesAggregationBuilder {
			private Function<JoiningLongMultiValuesSource, CollectorFactory<?, Long, ?>> collectorFactorySupplier;

			public Builder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<?> field) {
				super( scope, field );
				collectorFactorySupplier = CountValuesCollectorFactory::new;
			}

			@Override
			public void distinct(boolean distinct) {
				if ( distinct ) {
					collectorFactorySupplier = CountDistinctValuesCollectorFactory::new;
				}
				else {
					collectorFactorySupplier = CountValuesCollectorFactory::new;
				}
			}

			@Override
			public AbstractLuceneMetricNumericLongAggregation build() {
				return new LuceneCountValuesAggregation( this );
			}
		}
	}

	private static class LuceneCountDocumentAggregation implements LuceneSearchAggregation<Long> {

		private final Set<String> indexNames;

		LuceneCountDocumentAggregation(Builder builder) {
			this.indexNames = builder.scope.hibernateSearchIndexNames();
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

		public static class Builder implements CountDocumentAggregationBuilder {

			protected final LuceneSearchIndexScope<?> scope;

			public Builder(LuceneSearchIndexScope<?> scope) {
				this.scope = scope;
			}

			@Override
			public LuceneCountDocumentAggregation build() {
				return new LuceneCountDocumentAggregation( this );
			}
		}
	}

}
