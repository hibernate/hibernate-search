/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.loading.impl;

import java.util.Set;

import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingTypeContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassEntityLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassEntityLoadingContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassIdentifierLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassIdentifierLoadingContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassLoadingContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.loading.MassEntitySink;
import org.hibernate.search.mapper.pojo.standalone.loading.MassIdentifierSink;
import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.massindexing.impl.StandalonePojoMassIndexingSessionContext;
import org.hibernate.search.util.common.impl.SuppressingCloser;

public class StandalonePojoMassLoadingStrategy<E, I>
		implements PojoMassLoadingStrategy<E, I> {
	private final MassLoadingStrategy<E, I> delegate;

	public StandalonePojoMassLoadingStrategy(MassLoadingStrategy<E, I> delegate) {
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
		StandalonePojoMassLoadingStrategy<?, ?> that = (StandalonePojoMassLoadingStrategy<?, ?>) o;
		return delegate.equals( that.delegate );
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public boolean groupingAllowed(PojoLoadingTypeContext<? extends E> type, PojoMassLoadingContext context) {
		// No restriction.
		return true;
	}

	@Override
	public PojoMassIdentifierLoader createIdentifierLoader(
			Set<? extends PojoLoadingTypeContext<? extends E>> expectedTypes, PojoMassIdentifierLoadingContext<I> context) {
		StandalonePojoLoadingContext parentContext = (StandalonePojoLoadingContext) context.parent();
		StandalonePojoLoadingTypeGroup<E> includedTypes = new StandalonePojoLoadingTypeGroup<>(
				expectedTypes, parentContext.runtimeIntrospector() );
		MassIdentifierSink<I> sink = new StandalonePojoMassIdentifierSink<>( context.createSink() );
		return new StandalonePojoMassIdentifierLoader( delegate.createIdentifierLoader( includedTypes, sink, parentContext ) );
	}

	@Override
	public PojoMassEntityLoader<I> createEntityLoader(Set<? extends PojoLoadingTypeContext<? extends E>> expectedTypes,
			PojoMassEntityLoadingContext<E> context) {
		StandalonePojoLoadingContext parentContext = (StandalonePojoLoadingContext) context.parent();
		StandalonePojoMassIndexingSessionContext session = parentContext.mapping().createSession( context.tenantIdentifier() );
		try {
			StandalonePojoLoadingTypeGroup<E> includedTypes = new StandalonePojoLoadingTypeGroup<>(
					expectedTypes, parentContext.runtimeIntrospector() );
			MassEntitySink<E> sink = new StandalonePojoMassEntitySink<>( context.createSink( session ) );
			return new StandalonePojoMassEntityLoader<>( session,
					delegate.createEntityLoader( includedTypes, sink, parentContext ) );
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e ).push( session );
			throw e;
		}
	}
}
