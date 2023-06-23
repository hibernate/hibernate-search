/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.aggregation.dsl.spi;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.aggregation.dsl.ExtendedSearchAggregationFactory;
import org.hibernate.search.engine.search.aggregation.dsl.RangeAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactoryExtension;
import org.hibernate.search.engine.search.aggregation.dsl.TermsAggregationFieldStep;
import org.hibernate.search.engine.search.aggregation.dsl.impl.RangeAggregationFieldStepImpl;
import org.hibernate.search.engine.search.aggregation.dsl.impl.TermsAggregationFieldStepImpl;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationIndexScope;
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
	public <T> T extension(SearchAggregationFactoryExtension<T> extension) {
		return DslExtensionState.returnIfSupported( extension, extension.extendOptional( this ) );
	}

	@Override
	public final String toAbsolutePath(String relativeFieldPath) {
		return dslContext.scope().toAbsolutePath( relativeFieldPath );
	}
}
