/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.util.function.Function;

import org.hibernate.search.backend.lucene.logging.impl.QueryLog;
import org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl.CountDistinctValuesCollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl.CountValuesCollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.JoiningLongMultiValuesSource;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationRequestContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexNodeContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.engine.search.aggregation.spi.CountValuesAggregationBuilder;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementFactory;

public class LuceneCountValuesAggregation extends AbstractLuceneMetricNumericLongAggregation {
	private final Function<JoiningLongMultiValuesSource, CollectorFactory<?, Long, ?>> collectorFactorySupplier;

	LuceneCountValuesAggregation(Builder builder) {
		super( builder );
		collectorFactorySupplier = builder.collectorFactorySupplier;
	}

	public static Factory factory() {
		return Factory.INSTANCE;
	}

	protected static class Factory
			implements
			SearchQueryElementFactory<CountValuesAggregationBuilder.TypeSelector,
					LuceneSearchIndexScope<?>,
					LuceneSearchIndexNodeContext> {

		private static final Factory INSTANCE = new Factory();

		private Factory() {
		}

		@Override
		public CountValuesAggregationBuilder.TypeSelector create(LuceneSearchIndexScope<?> scope,
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
			implements CountValuesAggregationBuilder.TypeSelector {

		@Override
		public CountValuesAggregationBuilder builder() {
			return new Builder( scope, node.toValueField() );
		}
	}

	@Override
	CollectorKey<?, Long> fillCollectors(JoiningLongMultiValuesSource source, AggregationRequestContext context) {
		var collectorFactory = collectorFactorySupplier.apply( source );
		var collectorKey = collectorFactory.getCollectorKey();
		context.requireCollector( collectorFactory );
		return collectorKey;
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
