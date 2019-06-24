/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.spi;

import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.dsl.predicate.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;

/**
 * An abstract base for {@link PredicateFinalStep} implementations.
 *
 * @param <B> The implementation type of builders
 * This type is backend-specific. See {@link SearchPredicateBuilder#toImplementation()}
 */
public abstract class AbstractPredicateFinalStep<B> implements PredicateFinalStep {

	protected final SearchPredicateBuilderFactory<?, B> factory;

	private SearchPredicate predicateResult;

	public AbstractPredicateFinalStep(SearchPredicateBuilderFactory<?, B> factory) {
		this.factory = factory;
	}

	@Override
	public SearchPredicate toPredicate() {
		if ( predicateResult == null ) {
			predicateResult = factory.toSearchPredicate( toImplementation() );
		}
		return predicateResult;
	}

	protected abstract B toImplementation();
}
