/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.function.Supplier;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.search.dsl.predicate.NestedPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.NestedPredicateFieldContext;
import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateContributor;
import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.NestedPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;
import org.hibernate.search.util.impl.common.LoggerFactory;


class NestedPredicateContextImpl<N, B>
		implements NestedPredicateContext<N>, SearchPredicateDslContext<N, B>, SearchPredicateContributor<B> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final SearchPredicateFactory<?, B> factory;
	private final Supplier<N> nextContextProvider;

	private final SearchPredicateContainerContextImpl<N, B> containerContext;

	private NestedPredicateBuilder<B> builder;
	private SearchPredicateContributor<? extends B> childPredicateContributor;

	NestedPredicateContextImpl(SearchPredicateFactory<?, B> factory, Supplier<N> nextContextProvider) {
		this.factory = factory;
		this.nextContextProvider = nextContextProvider;
		this.containerContext = new SearchPredicateContainerContextImpl<>( factory, this );
	}

	@Override
	public NestedPredicateFieldContext<N> onObjectField(String absoluteFieldPath) {
		this.builder = factory.nested( absoluteFieldPath );
		return new NestedPredicateFieldContextImpl<>( containerContext, builder );
	}

	@Override
	public void addChild(SearchPredicateContributor<? extends B> child) {
		if ( this.childPredicateContributor != null ) {
			throw log.cannotAddMultiplePredicatesToNestedPredicate();
		}
		this.childPredicateContributor = child;
	}

	@Override
	public N getNextContext() {
		return nextContextProvider.get();
	}

	@Override
	public B contribute() {
		builder.nested( childPredicateContributor.contribute() );
		return builder.toImplementation();
	}
}
