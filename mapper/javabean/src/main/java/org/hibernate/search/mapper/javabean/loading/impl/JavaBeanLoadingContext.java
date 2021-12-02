/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.loading.impl;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.mapper.javabean.loading.MassLoadingOptions;
import org.hibernate.search.mapper.javabean.loading.MassLoadingStrategy;
import org.hibernate.search.mapper.javabean.loading.SelectionLoadingOptions;
import org.hibernate.search.mapper.javabean.loading.SelectionLoadingStrategy;
import org.hibernate.search.mapper.javabean.loading.dsl.SelectionLoadingOptionsStep;
import org.hibernate.search.mapper.javabean.log.impl.Log;
import org.hibernate.search.mapper.javabean.massindexing.impl.JavaBeanMassIndexingLoadingStrategy;
import org.hibernate.search.mapper.javabean.massindexing.impl.JavaBeanMassIndexingMappingContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingTypeContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionEntityLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingStrategy;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingLoadingStrategy;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class JavaBeanLoadingContext
		implements PojoSelectionLoadingContext, PojoMassIndexingContext, MassLoadingOptions, SelectionLoadingOptions {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final JavaBeanMassIndexingMappingContext mappingContext;
	private final LoadingTypeContextProvider typeContextProvider;
	private final DetachedBackendSessionContext sessionContext;

	private int batchSize = 10;
	private final Map<Class<?>, Object> contextData;

	private JavaBeanLoadingContext(Builder builder) {
		this.mappingContext = builder.mappingContext;
		this.typeContextProvider = builder.typeContextProvider;
		this.sessionContext = builder.sessionContext;
		this.contextData = builder.contextData;
	}

	@Override
	public String tenantIdentifier() {
		return sessionContext.tenantIdentifier();
	}

	public void batchSize(int batchSize) {
		if ( batchSize < 1 ) {
			throw new IllegalArgumentException( "batchSize must be at least 1" );
		}
		this.batchSize = batchSize;
	}

	@Override
	public int batchSize() {
		return batchSize;
	}

	public <T> void context(Class<T> contextType, T context) {
		contextData.put( contextType, context );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T context(Class<T> contextType) {
		return (T) contextData.get( contextType );
	}


	@Override
	public void checkOpen() {
		// Nothing to do: we're always "open"
	}

	@Override
	public PojoRuntimeIntrospector runtimeIntrospector() {
		return mappingContext.runtimeIntrospector();
	}

	@Override
	public <T> PojoSelectionLoadingStrategy<? super T> loadingStrategy(PojoLoadingTypeContext<T> type) {
		return loadingStrategyOptional( type )
				.orElseThrow( () -> log.entityLoadingStrategyNotRegistered( type.typeIdentifier() ) );
	}

	@Override
	public <T> Optional<PojoSelectionLoadingStrategy<? super T>> loadingStrategyOptional(
			PojoLoadingTypeContext<T> type) {
		PojoRawTypeIdentifier<T> typeId = type.typeIdentifier();
		return typeContextProvider.forExactType( typeId ).selectionLoadingStrategy()
				// Eclipse will complain about a raw type if we use a method reference here... for some reason.
				.map( s -> new JavaBeanSelectionLoadingStrategy<>( s ) );
	}

	@Override
	public <T> PojoMassIndexingLoadingStrategy<? super T, ?> loadingStrategy(
			PojoRawTypeIdentifier<T> expectedType) {
		LoadingTypeContext<T> typeContext = typeContextProvider.forExactType( expectedType );
		Optional<MassLoadingStrategy<? super T, ?>> strategyOptional = typeContext.massLoadingStrategy();
		if ( !strategyOptional.isPresent() ) {
			throw log.entityLoadingStrategyNotRegistered( typeContext.typeIdentifier() );
		}
		return new JavaBeanMassIndexingLoadingStrategy<>( mappingContext, typeContextProvider,
				strategyOptional.get(), sessionContext, this );
	}

	public static final class Builder implements JavaBeanSelectionLoadingContextBuilder, SelectionLoadingOptionsStep {
		private final JavaBeanMassIndexingMappingContext mappingContext;
		private final LoadingTypeContextProvider typeContextProvider;
		private final DetachedBackendSessionContext sessionContext;
		private final Map<Class<?>, Object> contextData = new HashMap<>();

		public Builder(JavaBeanMassIndexingMappingContext mappingContext,
				LoadingTypeContextProvider typeContextProvider, DetachedBackendSessionContext sessionContext) {
			this.mappingContext = mappingContext;
			this.typeContextProvider = typeContextProvider;
			this.sessionContext = sessionContext;
		}

		@Override
		public SelectionLoadingOptionsStep toAPI() {
			return this;
		}

		@Override
		public <T> void context(Class<T> contextType, T context) {
			contextData.put( contextType, context );
		}

		@Override
		public JavaBeanLoadingContext build() {
			return new JavaBeanLoadingContext( this );
		}
	}

	private class JavaBeanSelectionLoadingStrategy<E> implements PojoSelectionLoadingStrategy<E> {

		private final SelectionLoadingStrategy<E> delegate;

		private JavaBeanSelectionLoadingStrategy(SelectionLoadingStrategy<E> delegate) {
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
			JavaBeanSelectionLoadingStrategy<?> that = (JavaBeanSelectionLoadingStrategy<?>) o;
			return delegate.equals( that.delegate );
		}

		@Override
		public int hashCode() {
			return delegate.hashCode();
		}

		@Override
		public PojoSelectionEntityLoader<E> createLoader(
				Set<? extends PojoLoadingTypeContext<? extends E>> expectedTypes) {
			Set<PojoRawTypeIdentifier<? extends E>> includedTypeIdentifiers = new LinkedHashSet<>();
			for ( PojoLoadingTypeContext<? extends E> expectedType : expectedTypes ) {
				includedTypeIdentifiers.add( expectedType.typeIdentifier() );
			}
			JavaBeanLoadingTypeGroup<E> includedTypes = new JavaBeanLoadingTypeGroup<>(
					typeContextProvider, includedTypeIdentifiers, runtimeIntrospector() );
			return new JavaBeanSelectionEntityLoader<>(
					delegate.createEntityLoader( includedTypes, JavaBeanLoadingContext.this ) );
		}
	}
}
