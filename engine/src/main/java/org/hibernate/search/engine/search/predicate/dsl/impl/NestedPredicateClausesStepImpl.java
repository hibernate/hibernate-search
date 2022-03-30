/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.NestedPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.NestedPredicateOptionsCollector;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.NestedPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;


public final class NestedPredicateClausesStepImpl
		extends AbstractBooleanPredicateClausesStep<NestedPredicateClausesStepImpl, NestedPredicateOptionsCollector<?>>
		implements NestedPredicateClausesStep<NestedPredicateClausesStepImpl> {

	private final NestedPredicateBuilder builder;

	public NestedPredicateClausesStepImpl(SearchPredicateDslContext<?> dslContext, String objectFieldPath,
			SearchPredicateFactory factory) {
		super( dslContext, factory );
		this.builder = dslContext.scope().fieldQueryElement( objectFieldPath, PredicateTypeKeys.NESTED );
	}

	@Override
	protected NestedPredicateClausesStepImpl self() {
		return this;
	}

	@Override
	protected SearchPredicate build() {
		builder.nested( super.build() );
		return builder.build();
	}

}
