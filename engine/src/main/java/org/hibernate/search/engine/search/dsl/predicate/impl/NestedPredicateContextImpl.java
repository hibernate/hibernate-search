/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.util.function.Consumer;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateTerminalContext;
import org.hibernate.search.engine.search.dsl.predicate.NestedPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.NestedPredicateFieldContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;
import org.hibernate.search.engine.search.dsl.predicate.spi.AbstractObjectCreatingSearchPredicateContributor;
import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateContributor;
import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.NestedPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;
import org.hibernate.search.util.impl.common.LoggerFactory;


class NestedPredicateContextImpl<B>
		extends AbstractObjectCreatingSearchPredicateContributor<B>
		implements NestedPredicateContext, NestedPredicateFieldContext, SearchPredicateTerminalContext,
		SearchPredicateDslContext<B>, SearchPredicateContributor<B> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final SearchPredicateContainerContext containerContext;
	private NestedPredicateBuilder<B> builder;
	private SearchPredicateContributor<? extends B> childPredicateContributor;

	NestedPredicateContextImpl(SearchPredicateFactory<?, B> factory) {
		super( factory );
		this.containerContext = new SearchPredicateContainerContextImpl<>( factory, this );
	}

	@Override
	public NestedPredicateFieldContext onObjectField(String absoluteFieldPath) {
		this.builder = factory.nested( absoluteFieldPath );
		return this;
	}

	@Override
	public SearchPredicateTerminalContext nest(SearchPredicate searchPredicate) {
		containerContext.predicate( searchPredicate );
		return this;
	}

	@Override
	public SearchPredicateTerminalContext nest(Consumer<? super SearchPredicateContainerContext> predicateContributor) {
		predicateContributor.accept( containerContext );
		return this;
	}

	@Override
	public void addChild(SearchPredicateContributor<? extends B> child) {
		if ( this.childPredicateContributor != null ) {
			throw log.cannotAddMultiplePredicatesToNestedPredicate();
		}
		this.childPredicateContributor = child;
	}

	@Override
	protected B doContribute() {
		builder.nested( childPredicateContributor.contribute() );
		return builder.toImplementation();
	}

}
