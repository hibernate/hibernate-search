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
import org.hibernate.search.engine.search.dsl.sort.spi.NonEmptySortContextImpl;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;
import org.hibernate.search.engine.search.sort.spi.SearchSortFactory;
import org.hibernate.search.engine.spatial.GeoPoint;


public class SearchSortContainerContextImpl<B> implements SearchSortContainerContext {

	private final SearchSortFactory<?, B> factory;

	private final SearchSortDslContext<? super B> dslContext;

	public SearchSortContainerContextImpl(SearchSortFactory<?, B> factory, SearchSortDslContext<? super B> dslContext) {
		this.factory = factory;
		this.dslContext = dslContext;
	}

	@Override
	public NonEmptySortContext by(SearchSort sort) {
		factory.toImplementation( sort, dslContext::addChild );
		return nonEmptyContext();
	}

	@Override
	public ScoreSortContext byScore() {
		ScoreSortContextImpl<B> child = new ScoreSortContextImpl<>( this, factory, dslContext );
		dslContext.addChild( child );
		return child;
	}

	@Override
	public NonEmptySortContext byIndexOrder() {
		dslContext.addChild( factory.indexOrder() );
		return nonEmptyContext();
	}

	@Override
	public FieldSortContext byField(String absoluteFieldPath) {
		FieldSortContextImpl<B> child = new FieldSortContextImpl<>(
				this, factory, dslContext, absoluteFieldPath
		);
		dslContext.addChild( child );
		return child;
	}

	@Override
	public DistanceSortContext byDistance(String absoluteFieldPath, GeoPoint location) {
		DistanceSortContextImpl<B> child = new DistanceSortContextImpl<>(
				this, factory, dslContext, absoluteFieldPath, location
		);
		dslContext.addChild( child );
		return child;
	}

	@Override
	public <T> T extension(SearchSortContainerContextExtension<T> extension) {
		return DslExtensionState.returnIfSupported(
				extension, extension.extendOptional( this, factory, dslContext )
		);
	}

	@Override
	public SearchSortContainerExtensionContext extension() {
		return new SearchSortContainerExtensionContextImpl<>( this, factory, dslContext );
	}

	private NonEmptySortContext nonEmptyContext() {
		return new NonEmptySortContextImpl( this, dslContext );
	}

}
