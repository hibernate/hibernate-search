/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.aggregation.dsl.spi;

import java.util.function.Function;

import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.dsl.AvgAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.CountAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.CountDistinctAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.MaxAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.MinAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.RangeAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactoryExtension;
import org.hibernate.search.engine.search.aggregation.dsl.SumAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.TermsAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.TypedSearchAggregationFactory;
import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.engine.search.common.NonStaticMetamodelScope;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public record SearchAggregationFactoryDelegate(TypedSearchAggregationFactory<NonStaticMetamodelScope> delegate)
		implements SearchAggregationFactory {
	@Override
	public RangeAggregationFieldStep<NonStaticMetamodelScope, ?> range() {
		return delegate.range();
	}

	@Override
	public TermsAggregationFieldStep<NonStaticMetamodelScope, ?> terms() {
		return delegate.terms();
	}

	@Override
	public SumAggregationFieldStep<NonStaticMetamodelScope, ?> sum() {
		return delegate.sum();
	}

	@Override
	public MinAggregationFieldStep<NonStaticMetamodelScope, ?> min() {
		return delegate().min();
	}

	@Override
	public MaxAggregationFieldStep<NonStaticMetamodelScope, ?> max() {
		return delegate.max();
	}

	@Override
	public CountAggregationFieldStep<NonStaticMetamodelScope, ?> count() {
		return delegate.count();
	}

	@Override
	public CountDistinctAggregationFieldStep<NonStaticMetamodelScope, ?> countDistinct() {
		return delegate.countDistinct();
	}

	@Override
	public AvgAggregationFieldStep<NonStaticMetamodelScope, ?> avg() {
		return delegate.avg();
	}

	@Override
	public <T> AggregationFinalStep<T> withParameters(
			Function<? super NamedValues, ? extends AggregationFinalStep<T>> aggregationCreator) {
		return delegate.withParameters( aggregationCreator );
	}

	@Override
	public <T> T extension(SearchAggregationFactoryExtension<NonStaticMetamodelScope, T> extension) {
		return delegate.extension( extension );
	}

	@Override
	public SearchAggregationFactory withRoot(String objectFieldPath) {
		return new SearchAggregationFactoryDelegate( delegate.withRoot( objectFieldPath ) );
	}

	@Override
	public String toAbsolutePath(String relativeFieldPath) {
		return delegate.toAbsolutePath( relativeFieldPath );
	}
}
