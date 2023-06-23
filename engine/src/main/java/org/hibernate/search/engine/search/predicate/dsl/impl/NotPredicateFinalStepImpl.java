/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.NotPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.BooleanPredicateBuilder;

public final class NotPredicateFinalStepImpl extends AbstractPredicateFinalStep
		implements NotPredicateFinalStep {

	private final BooleanPredicateBuilder builder;

	public NotPredicateFinalStepImpl(SearchPredicateDslContext<?> dslContext, SearchPredicate searchPredicate) {
		super( dslContext );
		this.builder = dslContext.scope().predicateBuilders().bool();
		this.builder.mustNot( searchPredicate );
	}

	public NotPredicateFinalStepImpl(SearchPredicateDslContext<?> dslContext, PredicateFinalStep searchPredicate) {
		this( dslContext, searchPredicate.toPredicate() );
	}

	@Override
	protected SearchPredicate build() {
		return builder.build();
	}

	@Override
	public NotPredicateFinalStep boost(float boost) {
		builder.boost( boost );
		return this;
	}

	@Override
	public NotPredicateFinalStep constantScore() {
		builder.constantScore();
		return this;
	}
}
