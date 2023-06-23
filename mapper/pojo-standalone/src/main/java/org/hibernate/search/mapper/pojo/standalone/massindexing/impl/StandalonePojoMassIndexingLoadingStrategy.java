/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.massindexing.impl;

import org.hibernate.search.mapper.pojo.loading.spi.PojoMassEntityLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassIdentifierLoader;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingEntityLoadingContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingIdentifierLoadingContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.loading.MassEntitySink;
import org.hibernate.search.mapper.pojo.standalone.loading.MassIdentifierSink;
import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingOptions;
import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.loading.impl.LoadingTypeContextProvider;
import org.hibernate.search.mapper.pojo.standalone.loading.impl.StandalonePojoLoadingTypeGroup;
import org.hibernate.search.mapper.pojo.standalone.loading.impl.StandalonePojoMassEntityLoader;
import org.hibernate.search.mapper.pojo.standalone.loading.impl.StandalonePojoMassEntitySink;
import org.hibernate.search.mapper.pojo.standalone.loading.impl.StandalonePojoMassIdentifierLoader;
import org.hibernate.search.mapper.pojo.standalone.loading.impl.StandalonePojoMassIdentifierSink;
import org.hibernate.search.util.common.impl.SuppressingCloser;

public class StandalonePojoMassIndexingLoadingStrategy<E, I>
		implements PojoMassIndexingLoadingStrategy<E, I> {

	private final StandalonePojoMassIndexingMappingContext mappingContext;
	private final LoadingTypeContextProvider typeContextProvider;
	private final MassLoadingStrategy<E, I> delegate;
	private final MassLoadingOptions options;

	public StandalonePojoMassIndexingLoadingStrategy(StandalonePojoMassIndexingMappingContext mappingContext,
			LoadingTypeContextProvider typeContextProvider,
			MassLoadingStrategy<E, I> delegate,
			MassLoadingOptions options) {
		this.mappingContext = mappingContext;
		this.typeContextProvider = typeContextProvider;
		this.delegate = delegate;
		this.options = options;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		StandalonePojoMassIndexingLoadingStrategy<?, ?> that = (StandalonePojoMassIndexingLoadingStrategy<?, ?>) o;
		return delegate.equals( that.delegate );
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public PojoMassIdentifierLoader createIdentifierLoader(PojoMassIndexingIdentifierLoadingContext<E, I> context) {
		StandalonePojoLoadingTypeGroup<E> includedTypes = new StandalonePojoLoadingTypeGroup<>(
				typeContextProvider, context.includedTypes(), mappingContext.runtimeIntrospector() );
		MassIdentifierSink<I> sink = new StandalonePojoMassIdentifierSink<>( context.createSink() );
		return new StandalonePojoMassIdentifierLoader( delegate.createIdentifierLoader( includedTypes, sink, options ) );
	}

	@Override
	public PojoMassEntityLoader<I> createEntityLoader(PojoMassIndexingEntityLoadingContext<E> context) {
		StandalonePojoMassIndexingSessionContext session = mappingContext.createSession( context.tenantIdentifier() );
		try {
			StandalonePojoLoadingTypeGroup<E> includedTypes = new StandalonePojoLoadingTypeGroup<>(
					typeContextProvider, context.includedTypes(), mappingContext.runtimeIntrospector() );
			MassEntitySink<E> sink = new StandalonePojoMassEntitySink<>( context.createSink( session ) );
			return new StandalonePojoMassEntityLoader<>( session,
					delegate.createEntityLoader( includedTypes, sink, options ) );
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e ).push( session );
			throw e;
		}
	}
}
