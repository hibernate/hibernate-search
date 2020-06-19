/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.spi;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;

/**
 * An abstract base for {@link PredicateFinalStep} implementations.
 */
public abstract class AbstractPredicateFinalStep implements PredicateFinalStep {

	protected final SearchPredicateDslContext<?> dslContext;

	private SearchPredicate predicateResult;

	public AbstractPredicateFinalStep(SearchPredicateDslContext<?> dslContext) {
		this.dslContext = dslContext;
	}

	@Override
	public SearchPredicate toPredicate() {
		if ( predicateResult == null ) {
			predicateResult = build();
		}
		return predicateResult;
	}

	protected abstract SearchPredicate build();
}
