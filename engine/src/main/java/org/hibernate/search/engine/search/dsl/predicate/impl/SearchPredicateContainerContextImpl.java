/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.util.Optional;
import java.util.function.Consumer;

import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.dsl.predicate.BooleanJunctionPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.MatchAllPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.MatchPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.NestedPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.RangePredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;
import org.hibernate.search.engine.search.dsl.predicate.SpatialPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContextExtension;
import org.hibernate.search.engine.search.dsl.predicate.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;


public class SearchPredicateContainerContextImpl<N, B> implements SearchPredicateContainerContext<N> {

	private final SearchPredicateFactory<?, B> factory;

	private final SearchPredicateDslContext<N, ? super B> dslContext;

	public SearchPredicateContainerContextImpl(SearchPredicateFactory<?, B> factory,
			SearchPredicateDslContext<N, ? super B> dslContext) {
		this.factory = factory;
		this.dslContext = dslContext;
	}

	@Override
	public MatchAllPredicateContext<N> matchAll() {
		MatchAllPredicateContextImpl<N, B> child = new MatchAllPredicateContextImpl<>( factory, dslContext::getNextContext );
		dslContext.addChild( child );
		return child;
	}

	@Override
	public BooleanJunctionPredicateContext<N> bool() {
		BooleanJunctionPredicateContextImpl<N, B> child = new BooleanJunctionPredicateContextImpl<>( factory, dslContext::getNextContext );
		dslContext.addChild( child );
		return child;
	}

	@Override
	public N bool(Consumer<? super BooleanJunctionPredicateContext<?>> clauseContributor) {
		BooleanJunctionPredicateContext<N> context = bool();
		clauseContributor.accept( context );
		return context.end();
	}

	@Override
	public MatchPredicateContext<N> match() {
		MatchPredicateContextImpl<N, B> child = new MatchPredicateContextImpl<>( factory, dslContext::getNextContext );
		dslContext.addChild( child );
		return child;
	}

	@Override
	public RangePredicateContext<N> range() {
		RangePredicateContextImpl<N, B> child = new RangePredicateContextImpl<>( factory, dslContext::getNextContext );
		dslContext.addChild( child );
		return child;
	}

	@Override
	public NestedPredicateContext<N> nested() {
		NestedPredicateContextImpl<N, B> child = new NestedPredicateContextImpl<>( factory, dslContext::getNextContext );
		dslContext.addChild( child );
		return child;
	}

	@Override
	public SpatialPredicateContext<N> spatial() {
		SpatialPredicateContextImpl<N, B> child = new SpatialPredicateContextImpl<>( factory, dslContext::getNextContext );
		dslContext.addChild( child );
		return child;
	}

	@Override
	public N predicate(SearchPredicate predicate) {
		dslContext.addChild( factory.toImplementation( predicate ) );
		return dslContext.getNextContext();
	}

	@Override
	public <T> T withExtension(SearchPredicateContainerContextExtension<N, T> extension) {
		return extension.extendOrFail( this, factory, dslContext );
	}

	@Override
	public <T> N withExtensionOptional(
			SearchPredicateContainerContextExtension<N, T> extension, Consumer<T> predicateContributor) {
		extension.extendOptional( this, factory, dslContext ).ifPresent( predicateContributor );
		return dslContext.getNextContext();
	}

	@Override
	public <T> N withExtensionOptional(
			SearchPredicateContainerContextExtension<N, T> extension,
			Consumer<T> predicateContributor,
			Consumer<SearchPredicateContainerContext<N>> fallbackPredicateContributor) {
		Optional<T> optional = extension.extendOptional( this, factory, dslContext );
		if ( optional.isPresent() ) {
			predicateContributor.accept( optional.get() );
		}
		else {
			fallbackPredicateContributor.accept( this );
		}
		return dslContext.getNextContext();
	}

}
