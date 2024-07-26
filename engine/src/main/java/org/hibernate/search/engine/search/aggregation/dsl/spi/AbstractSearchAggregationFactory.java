/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl.spi;

import java.util.function.Function;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.dsl.AvgAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.CountAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.CountDistinctAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.ExtendedSearchAggregationFactory;
import org.hibernate.search.engine.search.aggregation.dsl.MaxAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.MinAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.RangeAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactoryExtension;
import org.hibernate.search.engine.search.aggregation.dsl.SumAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.TermsAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.impl.AvgAggregationFieldStepImpl;
import org.hibernate.search.engine.search.aggregation.dsl.impl.CountAggregationFieldStepImpl;
import org.hibernate.search.engine.search.aggregation.dsl.impl.CountDistinctAggregationFieldStepImpl;
import org.hibernate.search.engine.search.aggregation.dsl.impl.MaxAggregationFieldStepImpl;
import org.hibernate.search.engine.search.aggregation.dsl.impl.MinAggregationFieldStepImpl;
import org.hibernate.search.engine.search.aggregation.dsl.impl.RangeAggregationFieldStepImpl;
import org.hibernate.search.engine.search.aggregation.dsl.impl.SumAggregationFieldStepImpl;
import org.hibernate.search.engine.search.aggregation.dsl.impl.TermsAggregationFieldStepImpl;
import org.hibernate.search.engine.search.aggregation.dsl.impl.WithParametersAggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationIndexScope;
import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

public abstract class AbstractSearchAggregationFactory<
		S extends ExtendedSearchAggregationFactory<S, PDF>,
		SC extends SearchAggregationIndexScope<?>,
		PDF extends SearchPredicateFactory>
		implements ExtendedSearchAggregationFactory<S, PDF> {

	protected final SearchAggregationDslContext<SC, PDF> dslContext;

	public AbstractSearchAggregationFactory(SearchAggregationDslContext<SC, PDF> dslContext) {
		this.dslContext = dslContext;
	}

	@Override
	public RangeAggregationFieldStep<PDF> range() {
		return new RangeAggregationFieldStepImpl<>( dslContext );
	}

	@Override
	public TermsAggregationFieldStep<PDF> terms() {
		return new TermsAggregationFieldStepImpl<>( dslContext );
	}

	@Override
	public SumAggregationFieldStep<PDF> sum() {
		return new SumAggregationFieldStepImpl<>( dslContext );
	}

	@Override
	public MinAggregationFieldStep<PDF> min() {
		return new MinAggregationFieldStepImpl<>( dslContext );
	}

	@Override
	public MaxAggregationFieldStep<PDF> max() {
		return new MaxAggregationFieldStepImpl<>( dslContext );
	}

	@Override
	public CountAggregationFieldStep<PDF> count() {
		return new CountAggregationFieldStepImpl<>( dslContext );
	}

	@Override
	public CountDistinctAggregationFieldStep<PDF> countDistinct() {
		return new CountDistinctAggregationFieldStepImpl<>( dslContext );
	}

	public AvgAggregationFieldStep<PDF> avg() {
		return new AvgAggregationFieldStepImpl<>( dslContext );
	}

	@Override
	public <T> AggregationFinalStep<T> withParameters(
			Function<? super NamedValues, ? extends AggregationFinalStep<T>> aggregationCreator) {
		return new WithParametersAggregationFinalStep<>( dslContext, aggregationCreator );
	}

	@Override
	public <T> T extension(SearchAggregationFactoryExtension<T> extension) {
		return DslExtensionState.returnIfSupported( extension, extension.extendOptional( this ) );
	}

	@Override
	public final String toAbsolutePath(String relativeFieldPath) {
		return dslContext.scope().toAbsolutePath( relativeFieldPath );
	}
}
