/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading;

import org.hibernate.search.mapper.javabean.loading.LoadingTypeGroup;
import org.hibernate.search.mapper.javabean.loading.SelectionEntityLoader;
import org.hibernate.search.mapper.javabean.loading.SelectionLoadingOptions;
import org.hibernate.search.mapper.javabean.loading.SelectionLoadingStrategies;
import org.hibernate.search.mapper.javabean.loading.SelectionLoadingStrategy;

public class StubSelectionLoadingStrategy<E, I> implements SelectionLoadingStrategy<E> {
	private final PersistenceTypeKey<E, I> key;

	public StubSelectionLoadingStrategy(PersistenceTypeKey<E, I> key) {
		this.key = key;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		StubSelectionLoadingStrategy<?, ?> that = (StubSelectionLoadingStrategy<?, ?>) o;
		return key.equals( that.key );
	}

	@Override
	public int hashCode() {
		return key.hashCode();
	}

	@Override
	public SelectionEntityLoader<E> createEntityLoader(LoadingTypeGroup<E> includedTypes,
			SelectionLoadingOptions options) {
		StubLoadingContext context = options.context( StubLoadingContext.class );
		// Important: get the map from the context, not from this strategy's constructor,
		// because in real-world scenarios that's where the information (connection, ...) will come from.
		SelectionLoadingStrategy<E> delegateStrategy = SelectionLoadingStrategies.from( context.persistenceMap( key ) );
		SelectionEntityLoader<E> delegateLoader = delegateStrategy.createEntityLoader( includedTypes, options );
		return (identifiers, deadline) -> {
			context.loaderCalls()
					.add( new StubLoadingContext.LoaderCall( StubSelectionLoadingStrategy.this, identifiers ) );
			return delegateLoader.load( identifiers, deadline );
		};
	}
}
