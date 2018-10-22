/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.impl;

import org.hibernate.search.engine.common.dsl.impl.DslExtensionState;
import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.dsl.sort.DistanceSortContext;
import org.hibernate.search.engine.search.dsl.sort.FieldSortContext;
import org.hibernate.search.engine.search.dsl.sort.NonEmptySortContext;
import org.hibernate.search.engine.search.dsl.sort.ScoreSortContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContextExtension;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerExtensionContext;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.spi.SearchSortFactory;
import org.hibernate.search.engine.spatial.GeoPoint;


public class SearchSortContainerContextImpl<N, B> implements SearchSortContainerContext<N> {

	private final SearchSortFactory<?, B> factory;

	private final SearchSortDslContext<N, ? super B> dslContext;

	public SearchSortContainerContextImpl(SearchSortFactory<?, B> factory, SearchSortDslContext<N, ? super B> dslContext) {
		this.factory = factory;
		this.dslContext = dslContext;
	}

	@Override
	public NonEmptySortContext<N> by(SearchSort sort) {
		factory.toImplementation( sort, dslContext::addChild );
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
		ScoreSortContextImpl<N, B> child = new ScoreSortContextImpl<>( this, factory, dslContext::getNextContext );
		dslContext.addChild( child );
		return child;
	}

	@Override
	public NonEmptySortContext<N> byIndexOrder() {
		dslContext.addChild( factory.indexOrder() );
		return nonEmptyContext();
	}

	@Override
	public FieldSortContext<N> byField(String absoluteFieldPath) {
		FieldSortContextImpl<N, B> child = new FieldSortContextImpl<>(
				this, factory, dslContext::getNextContext, absoluteFieldPath
		);
		dslContext.addChild( child );
		return child;
	}

	@Override
	public DistanceSortContext<N> byDistance(String absoluteFieldPath, GeoPoint location) {
		DistanceSortContextImpl<N, B> child = new DistanceSortContextImpl<>(
				this, factory, dslContext::getNextContext, absoluteFieldPath, location
		);
		dslContext.addChild( child );
		return child;
	}

	@Override
	public <T> T extension(SearchSortContainerContextExtension<N, T> extension) {
		return DslExtensionState.returnIfSupported(
				extension, extension.extendOptional( this, factory, dslContext )
		);
	}

	@Override
	public SearchSortContainerExtensionContext<N> extension() {
		return new SearchSortContainerExtensionContextImpl<>( this, factory, dslContext );
	}

	private NonEmptySortContext<N> nonEmptyContext() {
		return new NonEmptySortContextImpl<>( this, dslContext );
	}

}
