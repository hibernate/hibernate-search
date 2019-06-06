/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.impl;

import java.util.function.Consumer;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.dsl.sort.CompositeSortContext;
import org.hibernate.search.engine.search.dsl.sort.DistanceSortContext;
import org.hibernate.search.engine.search.dsl.sort.FieldSortContext;
import org.hibernate.search.engine.search.dsl.sort.NonEmptySortContext;
import org.hibernate.search.engine.search.dsl.sort.ScoreSortContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactoryContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactoryContextExtension;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactoryExtensionContext;
import org.hibernate.search.engine.search.dsl.sort.spi.StaticNonEmptySortContext;
import org.hibernate.search.engine.search.dsl.sort.spi.SearchSortDslContext;
import org.hibernate.search.engine.spatial.GeoPoint;


public class DefaultSearchSortFactoryContext<B> implements SearchSortFactoryContext {

	private final SearchSortDslContext<?, B> dslContext;

	public DefaultSearchSortFactoryContext(SearchSortDslContext<?, B> dslContext) {
		this.dslContext = dslContext;
	}

	@Override
	public NonEmptySortContext by(SearchSort sort) {
		return staticNonEmptyContext( dslContext.getFactory().toImplementation( sort ) );
	}

	@Override
	public ScoreSortContext byScore() {
		return new ScoreSortContextImpl<>( dslContext );
	}

	@Override
	public NonEmptySortContext byIndexOrder() {
		return staticNonEmptyContext( dslContext.getFactory().indexOrder() );
	}

	@Override
	public FieldSortContext byField(String absoluteFieldPath) {
		return new FieldSortContextImpl<>( dslContext, absoluteFieldPath );
	}

	@Override
	public DistanceSortContext byDistance(String absoluteFieldPath, GeoPoint location) {
		return new DistanceSortContextImpl<>(
				dslContext, absoluteFieldPath, location
		);
	}

	@Override
	public CompositeSortContext byComposite() {
		return new CompositeSortContextImpl<>( dslContext );
	}

	@Override
	public NonEmptySortContext byComposite(Consumer<? super CompositeSortContext> elementContributor) {
		CompositeSortContext context = byComposite();
		elementContributor.accept( context );
		return context;
	}

	@Override
	public <T> T extension(SearchSortFactoryContextExtension<T> extension) {
		return DslExtensionState.returnIfSupported(
				extension, extension.extendOptional( this, dslContext )
		);
	}

	@Override
	public SearchSortFactoryExtensionContext extension() {
		return new SearchSortFactoryExtensionContextImpl<>( this, dslContext );
	}

	private NonEmptySortContext staticNonEmptyContext(B builder) {
		return new StaticNonEmptySortContext<B>( dslContext, builder );
	}

}
