/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.impl;

import java.util.Optional;
import java.util.function.Consumer;

import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.dsl.ExplicitEndContext;
import org.hibernate.search.engine.search.dsl.sort.FieldSortContext;
import org.hibernate.search.engine.search.dsl.sort.NonEmptySortContext;
import org.hibernate.search.engine.search.dsl.sort.ScoreSortContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortContainerContextExtension;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.spi.SearchSortFactory;


public class SearchSortContainerContextImpl<N, C> implements SearchSortContainerContext<N> {

	private final SearchSortFactory<C> factory;

	private final SearchSortDslContext<N, C> dslContext;

	public SearchSortContainerContextImpl(SearchSortFactory<C> factory, SearchSortDslContext<N, C> dslContext) {
		this.factory = factory;
		this.dslContext = dslContext;
	}

	@Override
	public NonEmptySortContext<N> by(SearchSort sort) {
		dslContext.addContributor( factory.toContributor( sort ) );
		return new NonEmptySortContext<N>() {
			@Override
			public SearchSortContainerContext<N> then() {
				return SearchSortContainerContextImpl.this;
			}
			@Override
			public N end() {
				return dslContext.getNextContext();
			}
		};
	}

	@Override
	public ScoreSortContext<N> byScore() {
		ScoreSortContextImpl<N, C> child = new ScoreSortContextImpl<>( this, factory, dslContext::getNextContext );
		dslContext.addContributor( child );
		return child;
	}

	@Override
	public ExplicitEndContext<N> byIndexOrder() {
		return dslContext::getNextContext;
	}

	@Override
	public FieldSortContext<N> byField(String absoluteFieldPath) {
		FieldSortContextImpl<N, C> child = new FieldSortContextImpl<>(
				this, factory, dslContext::getNextContext, absoluteFieldPath
		);
		dslContext.addContributor( child );
		return child;
	}

	@Override
	public <T> T withExtension(SearchSortContainerContextExtension<N, T> extension) {
		return extension.extendOrFail( this, factory, dslContext );
	}

	@Override
	public <T> N withExtensionOptional(
			SearchSortContainerContextExtension<N, T> extension, Consumer<T> clauseContributor) {
		extension.extendOptional( this, factory, dslContext ).ifPresent( clauseContributor );
		return dslContext.getNextContext();
	}

	@Override
	public <T> N withExtensionOptional(
			SearchSortContainerContextExtension<N, T> extension,
			Consumer<T> clauseContributor,
			Consumer<SearchSortContainerContext<N>> fallbackClauseContributor) {
		Optional<T> optional = extension.extendOptional( this, factory, dslContext );
		if ( optional.isPresent() ) {
			clauseContributor.accept( optional.get() );
		}
		else {
			fallbackClauseContributor.accept( this );
		}
		return dslContext.getNextContext();
	}

}
