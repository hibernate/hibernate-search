/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.loading.impl;

import java.util.Set;

import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingTypeContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionEntityLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.loading.SelectionLoadingStrategy;

public class StandalonePojoSelectionLoadingStrategy<E> implements PojoSelectionLoadingStrategy<E> {

	private final SelectionLoadingStrategy<E> delegate;

	public StandalonePojoSelectionLoadingStrategy(SelectionLoadingStrategy<E> delegate) {
		this.delegate = delegate;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		StandalonePojoSelectionLoadingStrategy<?> that = (StandalonePojoSelectionLoadingStrategy<?>) o;
		return delegate.equals( that.delegate );
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public PojoSelectionEntityLoader<E> createEntityLoader(
			Set<? extends PojoLoadingTypeContext<? extends E>> expectedTypes, PojoSelectionLoadingContext context) {
		StandalonePojoLoadingTypeGroup<E> includedTypes = new StandalonePojoLoadingTypeGroup<>(
				expectedTypes,
				context.runtimeIntrospector()
		);
		return new StandalonePojoSelectionEntityLoader<>( delegate.createEntityLoader(
				includedTypes, (StandalonePojoLoadingContext) context ) );
	}
}
