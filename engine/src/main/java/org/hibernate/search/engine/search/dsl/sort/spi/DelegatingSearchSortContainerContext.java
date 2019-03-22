/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.spi;

import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.search.dsl.sort.DistanceSortContext;
import org.hibernate.search.engine.search.dsl.sort.FieldSortContext;
import org.hibernate.search.engine.search.dsl.sort.NonEmptySortContext;
import org.hibernate.search.engine.search.dsl.sort.ScoreSortContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContextExtension;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerExtensionContext;
import org.hibernate.search.engine.spatial.GeoPoint;

/**
 * A delegating {@link SearchSortContainerContext}.
 * <p>
 * Mainly useful when implementing a {@link SearchSortContainerContextExtension}.
 */
public class DelegatingSearchSortContainerContext implements SearchSortContainerContext {

	private final SearchSortContainerContext delegate;

	public DelegatingSearchSortContainerContext(SearchSortContainerContext delegate) {
		this.delegate = delegate;
	}

	@Override
	public ScoreSortContext byScore() {
		return delegate.byScore();
	}

	@Override
	public NonEmptySortContext byIndexOrder() {
		return delegate.byIndexOrder();
	}

	@Override
	public FieldSortContext byField(String absoluteFieldPath) {
		return delegate.byField( absoluteFieldPath );
	}

	@Override
	public DistanceSortContext byDistance(String absoluteFieldPath, GeoPoint location) {
		return delegate.byDistance( absoluteFieldPath, location );
	}

	@Override
	public DistanceSortContext byDistance(String absoluteFieldPath, double latitude, double longitude) {
		return delegate.byDistance( absoluteFieldPath, latitude, longitude );
	}

	@Override
	public NonEmptySortContext by(SearchSort sort) {
		return delegate.by( sort );
	}

	@Override
	public <T> T extension(SearchSortContainerContextExtension<T> extension) {
		return delegate.extension( extension );
	}

	@Override
	public SearchSortContainerExtensionContext extension() {
		return delegate.extension();
	}

	protected SearchSortContainerContext getDelegate() {
		return delegate;
	}
}
