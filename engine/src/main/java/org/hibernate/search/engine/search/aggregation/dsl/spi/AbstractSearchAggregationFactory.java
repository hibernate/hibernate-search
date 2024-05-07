/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
		E,
		S extends ExtendedSearchAggregationFactory<E, S, PDF>,
		SC extends SearchAggregationIndexScope<?>,
		PDF extends SearchPredicateFactory<E>>
		implements ExtendedSearchAggregationFactory<E, S, PDF> {

	protected final SearchAggregationDslContext<E, SC, PDF> dslContext;

	public AbstractSearchAggregationFactory(SearchAggregationDslContext<E, SC, PDF> dslContext) {
		this.dslContext = dslContext;
	}

	@Override
	public RangeAggregationFieldStep<E, PDF> range() {
		return new RangeAggregationFieldStepImpl<>( dslContext );
	}

	@Override
	public TermsAggregationFieldStep<E, PDF> terms() {
		return new TermsAggregationFieldStepImpl<>( dslContext );
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
