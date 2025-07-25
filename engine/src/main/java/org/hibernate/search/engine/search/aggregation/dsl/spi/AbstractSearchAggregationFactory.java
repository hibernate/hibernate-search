/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl.spi;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.dsl.AvgAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.CompositeAggregationInnerStep;
import org.hibernate.search.engine.search.aggregation.dsl.CountDistinctValuesAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.CountDocumentsAggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.dsl.CountValuesAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.ExtendedSearchAggregationFactory;
import org.hibernate.search.engine.search.aggregation.dsl.MaxAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.MinAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.RangeAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactoryExtension;
import org.hibernate.search.engine.search.aggregation.dsl.SumAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.TermsAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.impl.AvgAggregationFieldStepImpl;
import org.hibernate.search.engine.search.aggregation.dsl.impl.CompositeAggregationInnerStepImpl;
import org.hibernate.search.engine.search.aggregation.dsl.impl.CountDistinctValuesAggregationFieldStepImpl;
import org.hibernate.search.engine.search.aggregation.dsl.impl.CountDocumentsAggregationFinalStepImpl;
import org.hibernate.search.engine.search.aggregation.dsl.impl.CountValuesAggregationFieldStepImpl;
import org.hibernate.search.engine.search.aggregation.dsl.impl.MaxAggregationFieldStepImpl;
import org.hibernate.search.engine.search.aggregation.dsl.impl.MinAggregationFieldStepImpl;
import org.hibernate.search.engine.search.aggregation.dsl.impl.RangeAggregationFieldStepImpl;
import org.hibernate.search.engine.search.aggregation.dsl.impl.SumAggregationFieldStepImpl;
import org.hibernate.search.engine.search.aggregation.dsl.impl.TermsAggregationFieldStepImpl;
import org.hibernate.search.engine.search.aggregation.dsl.impl.WithParametersAggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationIndexScope;
import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;

public abstract class AbstractSearchAggregationFactory<
		SR,
		S extends ExtendedSearchAggregationFactory<SR, S, PDF>,
		SC extends SearchAggregationIndexScope<?>,
		PDF extends TypedSearchPredicateFactory<SR>>
		implements ExtendedSearchAggregationFactory<SR, S, PDF> {

	protected final SearchAggregationDslContext<SR, SC, PDF> dslContext;

	public AbstractSearchAggregationFactory(SearchAggregationDslContext<SR, SC, PDF> dslContext) {
		this.dslContext = dslContext;
	}

	@Override
	public RangeAggregationFieldStep<SR, PDF> range() {
		return new RangeAggregationFieldStepImpl<>( dslContext );
	}

	@Override
	public TermsAggregationFieldStep<SR, PDF> terms() {
		return new TermsAggregationFieldStepImpl<>( dslContext );
	}

	@Override
	public SumAggregationFieldStep<SR, PDF> sum() {
		return new SumAggregationFieldStepImpl<>( dslContext );
	}

	@Override
	public MinAggregationFieldStep<SR, PDF> min() {
		return new MinAggregationFieldStepImpl<>( dslContext );
	}

	@Override
	public MaxAggregationFieldStep<SR, PDF> max() {
		return new MaxAggregationFieldStepImpl<>( dslContext );
	}

	@Override
	public CountValuesAggregationFieldStep<SR, PDF> countValues() {
		return new CountValuesAggregationFieldStepImpl<>( dslContext );
	}

	@Override
	public CountDistinctValuesAggregationFieldStep<SR, PDF> countDistinctValues() {
		return new CountDistinctValuesAggregationFieldStepImpl<>( dslContext );
	}

	@Override
	public CountDocumentsAggregationFinalStep countDocuments() {
		return new CountDocumentsAggregationFinalStepImpl( dslContext );
	}

	public AvgAggregationFieldStep<SR, PDF> avg() {
		return new AvgAggregationFieldStepImpl<>( dslContext );
	}

	@Override
	public CompositeAggregationInnerStep composite() {
		return new CompositeAggregationInnerStepImpl( dslContext );
	}

	@Override
	public AggregationFinalStep<List<?>> composite(SearchAggregation<?>... aggregations) {
		return composite()
				.from( aggregations )
				.asList();
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
