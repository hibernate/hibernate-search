/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.function.Function;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.dsl.predicate.NestedPredicateOptionsStep;
import org.hibernate.search.engine.search.dsl.predicate.PredicateFinalStep;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.predicate.NestedPredicateFieldStep;
import org.hibernate.search.engine.search.dsl.predicate.NestedPredicateNestStep;
import org.hibernate.search.engine.search.dsl.predicate.spi.AbstractPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.spi.NestedPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


class NestedPredicateFieldStepImpl<B>
		extends AbstractPredicateFinalStep<B>
		implements NestedPredicateFieldStep, NestedPredicateNestStep, NestedPredicateOptionsStep {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final SearchPredicateFactoryContext factoryContext;
	private NestedPredicateBuilder<B> builder;
	private B childPredicateBuilder;

	NestedPredicateFieldStepImpl(SearchPredicateBuilderFactory<?, B> factory, SearchPredicateFactoryContext factoryContext) {
		super( factory );
		this.factoryContext = factoryContext;
	}

	@Override
	public NestedPredicateNestStep onObjectField(String absoluteFieldPath) {
		this.builder = factory.nested( absoluteFieldPath );
		return this;
	}

	@Override
	public NestedPredicateOptionsStep nest(SearchPredicate searchPredicate) {
		if ( this.childPredicateBuilder != null ) {
			throw log.cannotAddMultiplePredicatesToNestedPredicate();
		}
		this.childPredicateBuilder = factory.toImplementation( searchPredicate );
		return this;
	}

	@Override
	public NestedPredicateOptionsStep nest(
			Function<? super SearchPredicateFactoryContext, ? extends PredicateFinalStep> predicateContributor) {
		return nest( predicateContributor.apply( factoryContext ) );
	}

	@Override
	protected B toImplementation() {
		builder.nested( childPredicateBuilder );
		return builder.toImplementation();
	}

}
