/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.MatchNonePredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.MatchNonePredicateBuilder;

public final class MatchNonePredicateFinalStepImpl extends AbstractPredicateFinalStep
		implements MatchNonePredicateFinalStep {

	private final MatchNonePredicateBuilder matchNoneBuilder;

	public MatchNonePredicateFinalStepImpl(SearchPredicateDslContext<?> dslContext) {
		super( dslContext );
		this.matchNoneBuilder = dslContext.scope().predicateBuilders().matchNone();
	}

	@Override
	protected SearchPredicate build() {
		return matchNoneBuilder.build();
	}
}
