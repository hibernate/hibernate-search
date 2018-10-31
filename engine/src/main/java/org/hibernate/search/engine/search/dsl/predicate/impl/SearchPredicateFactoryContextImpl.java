/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.util.function.Consumer;

import org.hibernate.search.engine.common.dsl.impl.DslExtensionState;
import org.hibernate.search.engine.search.dsl.predicate.BooleanJunctionPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.MatchAllPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.MatchPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.NestedPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.RangePredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContextExtension;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryExtensionContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateTerminalContext;
import org.hibernate.search.engine.search.dsl.predicate.SpatialPredicateContext;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;


public class SearchPredicateFactoryContextImpl<B> implements SearchPredicateFactoryContext {

	private final SearchPredicateBuilderFactory<?, B> factory;

	public SearchPredicateFactoryContextImpl(SearchPredicateBuilderFactory<?, B> factory) {
		this.factory = factory;
	}

	@Override
	public MatchAllPredicateContext matchAll() {
		return new MatchAllPredicateContextImpl<>( factory, this );
	}

	@Override
	public BooleanJunctionPredicateContext bool() {
		return new BooleanJunctionPredicateContextImpl<>( factory, this );
	}

	@Override
	public SearchPredicateTerminalContext bool(Consumer<? super BooleanJunctionPredicateContext> clauseContributor) {
		BooleanJunctionPredicateContext context = bool();
		clauseContributor.accept( context );
		return context;
	}

	@Override
	public MatchPredicateContext match() {
		return new MatchPredicateContextImpl<>( factory );
	}

	@Override
	public RangePredicateContext range() {
		return new RangePredicateContextImpl<>( factory );
	}

	@Override
	public NestedPredicateContext nested() {
		return new NestedPredicateContextImpl<>( factory, this );
	}

	@Override
	public SpatialPredicateContext spatial() {
		return new SpatialPredicateContextImpl<>( factory );
	}

	@Override
	public <T> T extension(SearchPredicateFactoryContextExtension<T> extension) {
		return DslExtensionState.returnIfSupported(
				extension, extension.extendOptional( this, factory )
		);
	}

	@Override
	public SearchPredicateFactoryExtensionContext extension() {
		return new SearchPredicateFactoryExtensionContextImpl<>( this, factory );
	}

}
