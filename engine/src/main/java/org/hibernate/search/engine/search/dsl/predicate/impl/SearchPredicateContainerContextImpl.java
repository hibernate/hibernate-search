/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.util.function.Consumer;

import org.hibernate.search.engine.common.dsl.impl.DslExtensionState;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.dsl.predicate.BooleanJunctionPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.MatchAllPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.MatchPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.NestedPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.RangePredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContextExtension;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerExtensionContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateTerminalContext;
import org.hibernate.search.engine.search.dsl.predicate.SpatialPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;


public class SearchPredicateContainerContextImpl<B> implements SearchPredicateContainerContext {

	private final SearchPredicateFactory<?, B> factory;

	private final SearchPredicateDslContext<? super B> dslContext;

	public SearchPredicateContainerContextImpl(SearchPredicateFactory<?, B> factory,
			SearchPredicateDslContext<? super B> dslContext) {
		this.factory = factory;
		this.dslContext = dslContext;
	}

	@Override
	public MatchAllPredicateContext matchAll() {
		MatchAllPredicateContextImpl<B> child = new MatchAllPredicateContextImpl<>( factory );
		dslContext.addChild( child );
		return child;
	}

	@Override
	public BooleanJunctionPredicateContext bool() {
		BooleanJunctionPredicateContextImpl<B> child = new BooleanJunctionPredicateContextImpl<>( factory );
		dslContext.addChild( child );
		return child;
	}

	@Override
	public SearchPredicateTerminalContext bool(Consumer<? super BooleanJunctionPredicateContext> clauseContributor) {
		BooleanJunctionPredicateContext context = bool();
		clauseContributor.accept( context );
		return context;
	}

	@Override
	public MatchPredicateContext match() {
		MatchPredicateContextImpl<B> child = new MatchPredicateContextImpl<>( factory );
		dslContext.addChild( child );
		return child;
	}

	@Override
	public RangePredicateContext range() {
		RangePredicateContextImpl<B> child = new RangePredicateContextImpl<>( factory );
		dslContext.addChild( child );
		return child;
	}

	@Override
	public NestedPredicateContext nested() {
		NestedPredicateContextImpl<B> child = new NestedPredicateContextImpl<>( factory );
		dslContext.addChild( child );
		return child;
	}

	@Override
	public SpatialPredicateContext spatial() {
		SpatialPredicateContextImpl<B> child = new SpatialPredicateContextImpl<>( factory );
		dslContext.addChild( child );
		return child;
	}

	@Override
	public void predicate(SearchPredicate predicate) {
		dslContext.addChild( factory.toImplementation( predicate ) );
	}

	@Override
	public <T> T extension(SearchPredicateContainerContextExtension<T> extension) {
		return DslExtensionState.returnIfSupported(
				extension, extension.extendOptional( this, factory, dslContext )
		);
	}

	@Override
	public SearchPredicateContainerExtensionContext extension() {
		return new SearchPredicateContainerExtensionContextImpl<>( this, factory, dslContext );
	}

}
