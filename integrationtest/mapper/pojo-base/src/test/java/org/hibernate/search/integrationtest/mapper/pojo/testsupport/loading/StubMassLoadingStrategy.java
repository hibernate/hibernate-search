/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.loading;

import java.util.List;

import org.hibernate.search.mapper.pojo.standalone.loading.LoadingTypeGroup;
import org.hibernate.search.mapper.pojo.standalone.loading.MassEntityLoader;
import org.hibernate.search.mapper.pojo.standalone.loading.MassEntitySink;
import org.hibernate.search.mapper.pojo.standalone.loading.MassIdentifierLoader;
import org.hibernate.search.mapper.pojo.standalone.loading.MassIdentifierSink;
import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingOptions;
import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingStrategy;

public class StubMassLoadingStrategy<E, I> implements MassLoadingStrategy<E, I> {
	private final PersistenceTypeKey<E, I> key;

	public StubMassLoadingStrategy(PersistenceTypeKey<E, I> key) {
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
		StubMassLoadingStrategy<?, ?> that = (StubMassLoadingStrategy<?, ?>) o;
		return key.equals( that.key );
	}

	@Override
	public int hashCode() {
		return key.hashCode();
	}

	@Override
	public MassIdentifierLoader createIdentifierLoader(LoadingTypeGroup<E> includedTypes, MassIdentifierSink<I> sink,
			MassLoadingOptions options) {
		StubLoadingContext context = options.context( StubLoadingContext.class );
		// Important: get the map from the context, not from this strategy's constructor,
		// because in real-world scenarios that's where the information (connection, ...) will come from.
		MassLoadingStrategy<E, I> delegate = MassLoadingStrategy.fromMap( context.persistenceMap( key ) );
		return delegate.createIdentifierLoader( includedTypes, sink, options );
	}

	@Override
	public MassEntityLoader<I> createEntityLoader(LoadingTypeGroup<E> includedTypes, MassEntitySink<E> sink,
			MassLoadingOptions options) {
		StubLoadingContext context = options.context( StubLoadingContext.class );
		// Important: get the map from the context, not from this strategy's constructor,
		// because in real-world scenarios that's where the information (connection, ...) will come from.
		MassLoadingStrategy<E, I> delegateStrategy = MassLoadingStrategy.fromMap( context.persistenceMap( key ) );
		MassEntityLoader<I> delegateLoader = delegateStrategy.createEntityLoader( includedTypes, sink, options );
		return new MassEntityLoader<I>() {
			@Override
			public void close() {
				delegateLoader.close();
			}

			@Override
			public void load(List<I> identifiers) throws InterruptedException {
				context.loaderCalls()
						.add( new StubLoadingContext.LoaderCall( StubMassLoadingStrategy.this, identifiers ) );
				delegateLoader.load( identifiers );
			}
		};
	}
}
