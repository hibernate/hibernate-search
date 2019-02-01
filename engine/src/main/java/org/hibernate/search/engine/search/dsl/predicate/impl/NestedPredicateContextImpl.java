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
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateTerminalContext;
import org.hibernate.search.engine.search.dsl.predicate.NestedPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.NestedPredicateFieldContext;
import org.hibernate.search.engine.search.dsl.predicate.spi.AbstractSearchPredicateTerminalContext;
import org.hibernate.search.engine.search.predicate.spi.NestedPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.util.impl.common.LoggerFactory;


class NestedPredicateContextImpl<B>
		extends AbstractSearchPredicateTerminalContext<B>
		implements NestedPredicateContext, NestedPredicateFieldContext, SearchPredicateTerminalContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final SearchPredicateFactoryContext factoryContext;
	private NestedPredicateBuilder<B> builder;
	private B childPredicateBuilder;

	NestedPredicateContextImpl(SearchPredicateBuilderFactory<?, B> factory, SearchPredicateFactoryContext factoryContext) {
		super( factory );
		this.factoryContext = factoryContext;
	}

	@Override
	public NestedPredicateFieldContext onObjectField(String absoluteFieldPath) {
		this.builder = factory.nested( absoluteFieldPath );
		return this;
	}

	@Override
	public SearchPredicateTerminalContext nest(SearchPredicate searchPredicate) {
		if ( this.childPredicateBuilder != null ) {
			throw log.cannotAddMultiplePredicatesToNestedPredicate();
		}
		this.childPredicateBuilder = factory.toImplementation( searchPredicate );
		return this;
	}

	@Override
	public SearchPredicateTerminalContext nest(
			Function<? super SearchPredicateFactoryContext, ? extends SearchPredicateTerminalContext> predicateContributor) {
		return nest( predicateContributor.apply( factoryContext ) );
	}

	@Override
	protected B toImplementation() {
		builder.nested( childPredicateBuilder );
		return builder.toImplementation();
	}

}
