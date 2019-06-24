/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort.spi;

import java.util.function.Consumer;

import org.hibernate.search.engine.search.dsl.sort.CompositeSortComponentsStep;
import org.hibernate.search.engine.search.dsl.sort.DistanceSortOptionsStep;
import org.hibernate.search.engine.search.dsl.sort.FieldSortOptionsStep;
import org.hibernate.search.engine.search.dsl.sort.SortThenStep;
import org.hibernate.search.engine.search.dsl.sort.ScoreSortOptionsStep;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactory;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactoryExtension;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactoryExtensionIfSupportedStep;
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
	public ScoreSortOptionsStep byScore() {
		return delegate.byScore();
	}

	@Override
	public SortThenStep byIndexOrder() {
		return delegate.byIndexOrder();
	}

	@Override
	public FieldSortOptionsStep byField(String absoluteFieldPath) {
		return delegate.byField( absoluteFieldPath );
	}

	@Override
	public DistanceSortOptionsStep byDistance(String absoluteFieldPath, GeoPoint location) {
		return delegate.byDistance( absoluteFieldPath, location );
	}

	@Override
	public DistanceSortOptionsStep byDistance(String absoluteFieldPath, double latitude, double longitude) {
		return delegate.byDistance( absoluteFieldPath, latitude, longitude );
	}

	@Override
	public CompositeSortComponentsStep byComposite() {
		return delegate.byComposite();
	}

	@Override
	public SortThenStep byComposite(Consumer<? super CompositeSortComponentsStep> elementContributor) {
		return delegate.byComposite( elementContributor );
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
