/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.massindexing.impl;

import org.hibernate.search.mapper.javabean.loading.MassIdentifierSink;
import org.hibernate.search.mapper.javabean.loading.MassLoadingOptions;
import org.hibernate.search.mapper.javabean.loading.MassEntitySink;
import org.hibernate.search.mapper.javabean.loading.MassLoadingStrategy;
import org.hibernate.search.mapper.javabean.loading.impl.JavaBeanMassIdentifierLoader;
import org.hibernate.search.mapper.javabean.loading.impl.JavaBeanMassIdentifierSink;
import org.hibernate.search.mapper.javabean.loading.impl.JavaBeanMassEntityLoader;
import org.hibernate.search.mapper.javabean.loading.impl.JavaBeanLoadingTypeGroup;
import org.hibernate.search.mapper.javabean.loading.impl.JavaBeanMassEntitySink;
import org.hibernate.search.mapper.javabean.loading.impl.LoadingTypeContextProvider;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassIdentifierLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassEntityLoader;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingIdentifierLoadingContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingEntityLoadingContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingLoadingStrategy;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingSessionContext;

public class JavaBeanMassIndexingLoadingStrategy<E, I>
		implements PojoMassIndexingLoadingStrategy<E, I> {

	private final JavaBeanMassIndexingMappingContext mappingContext;
	private final LoadingTypeContextProvider typeContextProvider;
	private final MassLoadingStrategy<E, I> delegate;
	private final MassLoadingOptions options;

	public JavaBeanMassIndexingLoadingStrategy(JavaBeanMassIndexingMappingContext mappingContext,
			LoadingTypeContextProvider typeContextProvider,
			MassLoadingStrategy<E, I> delegate, MassLoadingOptions options) {
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
		JavaBeanMassIndexingLoadingStrategy<?, ?> that = (JavaBeanMassIndexingLoadingStrategy<?, ?>) o;
		return delegate.equals( that.delegate );
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public PojoMassIdentifierLoader createIdentifierLoader(PojoMassIndexingIdentifierLoadingContext<E, I> context) {
		JavaBeanLoadingTypeGroup<E> includedTypes = new JavaBeanLoadingTypeGroup<>(
				typeContextProvider, context.includedTypes(), mappingContext.runtimeIntrospector() );
		MassIdentifierSink<I> sink = new JavaBeanMassIdentifierSink<>( context.createSink() );
		return new JavaBeanMassIdentifierLoader( delegate.createIdentifierLoader( includedTypes, sink, options ) );
	}

	@Override
	public PojoMassEntityLoader<I> createEntityLoader(PojoMassIndexingEntityLoadingContext<E> context) {
		PojoMassIndexingSessionContext sessionContext = mappingContext.sessionContext();
		JavaBeanLoadingTypeGroup<E> includedTypes = new JavaBeanLoadingTypeGroup<>(
				typeContextProvider, context.includedTypes(), mappingContext.runtimeIntrospector() );
		MassEntitySink<E> sink = new JavaBeanMassEntitySink<>( context.createSink( sessionContext ) );
		return new JavaBeanMassEntityLoader<>( delegate.createEntityLoader( includedTypes, sink, options ) );
	}
}
