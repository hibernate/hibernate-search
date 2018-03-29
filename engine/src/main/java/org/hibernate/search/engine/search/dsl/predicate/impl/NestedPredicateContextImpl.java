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
import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.NestedPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateContributor;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;
import org.hibernate.search.util.impl.common.LoggerFactory;


class NestedPredicateContextImpl<N, C>
		implements NestedPredicateContext<N>, SearchPredicateContributor<C>, SearchPredicateDslContext<N, C> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final SearchPredicateFactory<C> factory;
	private final Supplier<N> nextContextProvider;

	private final SearchPredicateContainerContextImpl<N, C> containerContext;

	private NestedPredicateBuilder<C> builder;
	private SearchPredicateContributor<C> singlePredicateContributor;

	NestedPredicateContextImpl(SearchPredicateFactory<C> factory, Supplier<N> nextContextProvider) {
		this.factory = factory;
		this.nextContextProvider = nextContextProvider;
		this.containerContext = new SearchPredicateContainerContextImpl<>( factory, this );
	}

	@Override
	public NestedPredicateFieldContext<N> onObjectField(String fieldName) {
		this.builder = factory.nested( fieldName );
		return new NestedPredicateFieldContextImpl<>( containerContext, builder );
	}

	@Override
	public void contribute(C collector) {
		singlePredicateContributor.contribute( builder.getNestedCollector() );
		builder.contribute( collector );
	}

	@Override
	public void addContributor(SearchPredicateContributor<C> child) {
		if ( this.singlePredicateContributor != null ) {
			throw log.cannotAddMultiplePredicatesToNestedPredicate();
		}
		this.singlePredicateContributor = child;
	}

	@Override
	public N getNextContext() {
		return nextContextProvider.get();
	}
}
