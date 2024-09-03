/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl.spi;

import java.util.function.Function;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.dsl.ExtendedSearchAggregationFactory;
import org.hibernate.search.engine.search.aggregation.dsl.RangeAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactoryExtension;
import org.hibernate.search.engine.search.aggregation.dsl.TermsAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.impl.RangeAggregationFieldStepImpl;
import org.hibernate.search.engine.search.aggregation.dsl.impl.TermsAggregationFieldStepImpl;
import org.hibernate.search.engine.search.aggregation.dsl.impl.WithParametersAggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationIndexScope;
import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

public abstract class AbstractSearchAggregationFactory<
		SR,
		S extends ExtendedSearchAggregationFactory<SR, S, PDF>,
		SC extends SearchAggregationIndexScope<?>,
		PDF extends SearchPredicateFactory<SR>>
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
	public <T> AggregationFinalStep<T> withParameters(
			Function<? super NamedValues, ? extends AggregationFinalStep<T>> aggregationCreator) {
		return new WithParametersAggregationFinalStep<>( dslContext, aggregationCreator );
	}

	@Override
	public <T> T extension(SearchAggregationFactoryExtension<SR, T> extension) {
		return DslExtensionState.returnIfSupported( extension, extension.extendOptional( this ) );
	}

	@Override
	public final String toAbsolutePath(String relativeFieldPath) {
		return dslContext.scope().toAbsolutePath( relativeFieldPath );
	}
}
