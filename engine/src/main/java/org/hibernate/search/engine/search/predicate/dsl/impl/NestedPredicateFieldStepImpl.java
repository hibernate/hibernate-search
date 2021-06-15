/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.NestedPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.NestedPredicateNestStep;
import org.hibernate.search.engine.search.predicate.dsl.NestedPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.NestedPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;


public final class NestedPredicateFieldStepImpl
		extends AbstractPredicateFinalStep
		implements NestedPredicateFieldStep<NestedPredicateNestStep<?>>,
				NestedPredicateNestStep<NestedPredicateOptionsStep<?>>,
				NestedPredicateOptionsStep<NestedPredicateOptionsStep<?>> {

	private final SearchPredicateFactory factory;
	private NestedPredicateBuilder builder;

	public NestedPredicateFieldStepImpl(SearchPredicateDslContext<?> dslContext, SearchPredicateFactory factory) {
		super( dslContext );
		this.factory = factory;
	}

	@Override
	public NestedPredicateNestStep<?> objectField(String fieldPath) {
		this.builder = dslContext.scope().fieldQueryElement( fieldPath, PredicateTypeKeys.NESTED );
		return this;
	}

	@Override
	public NestedPredicateOptionsStep<?> nest(SearchPredicate searchPredicate) {
		builder.nested( searchPredicate );
		return this;
	}

	@Override
	public NestedPredicateOptionsStep<?> nest(
			Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> predicateContributor) {
		return nest( predicateContributor.apply( factory ) );
	}

	@Override
	protected SearchPredicate build() {
		return builder.build();
	}

}
