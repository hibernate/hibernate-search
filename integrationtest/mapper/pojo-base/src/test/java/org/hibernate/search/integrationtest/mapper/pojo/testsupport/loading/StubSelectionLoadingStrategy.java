/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading;

import org.hibernate.search.mapper.pojo.standalone.loading.LoadingTypeGroup;
import org.hibernate.search.mapper.pojo.standalone.loading.SelectionEntityLoader;
import org.hibernate.search.mapper.pojo.standalone.loading.SelectionLoadingOptions;
import org.hibernate.search.mapper.pojo.standalone.loading.SelectionLoadingStrategy;

class StubSelectionLoadingStrategy<E, I> implements SelectionLoadingStrategy<E> {
	private final PersistenceTypeKey<E, I> key;

	StubSelectionLoadingStrategy(PersistenceTypeKey<E, I> key) {
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
		SelectionLoadingStrategy<E> delegateStrategy = SelectionLoadingStrategy.fromMap( context.persistenceMap( key ) );
		SelectionEntityLoader<E> delegateLoader = delegateStrategy.createEntityLoader( includedTypes, options );
		return (identifiers, deadline) -> {
			context.loaderCalls()
					.add( new StubLoadingContext.LoaderCall( StubSelectionLoadingStrategy.this, identifiers ) );
			return delegateLoader.load( identifiers, deadline );
		};
	}
}
