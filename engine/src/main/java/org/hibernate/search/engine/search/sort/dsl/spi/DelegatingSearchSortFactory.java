/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl.spi;

import java.util.function.Consumer;

import org.hibernate.search.engine.search.sort.dsl.CompositeSortComponentsStep;
import org.hibernate.search.engine.search.sort.dsl.DistanceSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.FieldSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SortThenStep;
import org.hibernate.search.engine.search.sort.dsl.ScoreSortOptionsStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactoryExtension;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactoryExtensionIfSupportedStep;
import org.hibernate.search.engine.spatial.GeoPoint;

/**
 * A delegating {@link SearchSortFactory}.
 * <p>
 * Mainly useful when implementing a {@link SearchSortFactoryExtension}.
 */
public class DelegatingSearchSortFactory implements SearchSortFactory {

	private final SearchSortFactory delegate;

	public DelegatingSearchSortFactory(SearchSortFactory delegate) {
		this.delegate = delegate;
	}

	@Override
	public ScoreSortOptionsStep<?> score() {
		return delegate.score();
	}

	@Override
	public SortThenStep indexOrder() {
		return delegate.indexOrder();
	}

	@Override
	public FieldSortOptionsStep<?> field(String absoluteFieldPath) {
		return delegate.field( absoluteFieldPath );
	}

	@Override
	public DistanceSortOptionsStep<?> distance(String absoluteFieldPath, GeoPoint location) {
		return delegate.distance( absoluteFieldPath, location );
	}

	@Override
	public DistanceSortOptionsStep<?> distance(String absoluteFieldPath, double latitude, double longitude) {
		return delegate.distance( absoluteFieldPath, latitude, longitude );
	}

	@Override
	public CompositeSortComponentsStep<?> composite() {
		return delegate.composite();
	}

	@Override
	public SortThenStep composite(Consumer<? super CompositeSortComponentsStep<?>> elementContributor) {
		return delegate.composite( elementContributor );
	}

	@Override
	public <T> T extension(SearchSortFactoryExtension<T> extension) {
		return delegate.extension( extension );
	}

	@Override
	public SearchSortFactoryExtensionIfSupportedStep extension() {
		return delegate.extension();
	}

	protected SearchSortFactory getDelegate() {
		return delegate;
	}
}
