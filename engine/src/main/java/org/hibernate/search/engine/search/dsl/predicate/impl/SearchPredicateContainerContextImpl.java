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
import org.hibernate.search.engine.search.dsl.predicate.MatchPredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.RangePredicateContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;
import org.hibernate.search.engine.search.dsl.spi.SearchPredicateContainerContextExtension;
import org.hibernate.search.engine.search.dsl.spi.SearchDslContext;
import org.hibernate.search.engine.search.dsl.spi.SearchTargetContext;


public class SearchPredicateContainerContextImpl<N, C> implements SearchPredicateContainerContext<N> {

	private final SearchTargetContext<C> targetContext;

	private final SearchDslContext<N, C> dslContext;

	public SearchPredicateContainerContextImpl(SearchTargetContext<C> targetContext, SearchDslContext<N, C> dslContext) {
		this.targetContext = targetContext;
		this.dslContext = dslContext;
	}

	@Override
	public BooleanJunctionPredicateContext<N> bool() {
		BooleanJunctionPredicateContextImpl<N, C> child = new BooleanJunctionPredicateContextImpl<>( targetContext, dslContext::getNextContext );
		dslContext.addContributor( child );
		return child;
	}

	@Override
	public MatchPredicateContext<N> match() {
		MatchPredicateContextImpl<N, C> child = new MatchPredicateContextImpl<>( targetContext, dslContext::getNextContext );
		dslContext.addContributor( child );
		return child;
	}

	@Override
	public RangePredicateContext<N> range() {
		RangePredicateContextImpl<N, C> child = new RangePredicateContextImpl<>( targetContext, dslContext::getNextContext );
		dslContext.addContributor( child );
		return child;
	}

	@Override
	public N predicate(SearchPredicate predicate) {
		dslContext.addContributor( targetContext.toContributor( predicate ) );
		return dslContext.getNextContext();
	}

	@Override
	public <T> T withExtension(SearchPredicateContainerContextExtension<N, T> extension) {
		return extension.extendOrFail( this, targetContext, dslContext );
	}

	@Override
	public <T> N withExtensionOptional(
			SearchPredicateContainerContextExtension<N, T> extension, Consumer<T> clauseContributor) {
		extension.extendOptional( this, targetContext, dslContext ).ifPresent( clauseContributor );
		return dslContext.getNextContext();
	}

	@Override
	public <T> N withExtensionOptional(
			SearchPredicateContainerContextExtension<N, T> extension,
			Consumer<T> clauseContributor,
			Consumer<SearchPredicateContainerContext<N>> fallbackClauseContributor) {
		Optional<T> optional = extension.extendOptional( this, targetContext, dslContext );
		if ( optional.isPresent() ) {
			clauseContributor.accept( optional.get() );
		}
		else {
			fallbackClauseContributor.accept( this );
		}
		return dslContext.getNextContext();
	}

}
