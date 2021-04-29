/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.loading.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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
	private final JavaBeanLoadingSessionContext sessionContext;
	private final Map<PojoRawTypeIdentifier<?>, SelectionLoadingStrategy<?>> selectionLoadingStrategyByType;
	private final Map<PojoRawTypeIdentifier<?>, MassLoadingStrategy<?, ?>> massLoadingStrategyByType;

	private int batchSize = 10;
	private final Map<Class<?>, Object> contextData;

	private JavaBeanLoadingContext(Builder builder) {
		this.mappingContext = builder.mappingContext;
		this.typeContextProvider = builder.typeContextProvider;
		this.sessionContext = builder.sessionContext;
		this.selectionLoadingStrategyByType = builder.selectionLoadingStrategyByType == null ? Collections.emptyMap() :
				builder.selectionLoadingStrategyByType;
		this.massLoadingStrategyByType =
				builder.massLoadingStrategyByType == null ? Collections.emptyMap() : builder.massLoadingStrategyByType;
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
		return sessionContext.runtimeIntrospector();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> PojoSelectionLoadingStrategy<? super T> loadingStrategy(PojoLoadingTypeContext<T> type) {
		PojoRawTypeIdentifier<T> typeId = type.typeIdentifier();
		SelectionLoadingStrategy<? super T> strategy =
				(SelectionLoadingStrategy<T>) selectionLoadingStrategyByType.get( typeId );
		if ( strategy == null ) {
			for ( Map.Entry<PojoRawTypeIdentifier<?>, SelectionLoadingStrategy<?>> entry :
					selectionLoadingStrategyByType.entrySet() ) {
				if ( entry.getKey().javaClass().isAssignableFrom( typeId.javaClass() ) ) {
					strategy = (SelectionLoadingStrategy<? super T>) entry.getValue();
					break;
				}
			}
		}
		if ( strategy == null ) {
			throw log.entityLoadingStrategyNotRegistered( typeId );
		}
		return new JavaBeanSelectionLoadingStrategy<>( strategy );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> PojoMassIndexingLoadingStrategy<? super T, ?> loadingStrategy(
			PojoRawTypeIdentifier<T> expectedType) {
		MassLoadingStrategy<? super T, ?> strategy =
				(MassLoadingStrategy<T, ?>) massLoadingStrategyByType.get( expectedType );
		if ( strategy == null ) {
			for ( Map.Entry<PojoRawTypeIdentifier<?>, MassLoadingStrategy<?, ?>> entry :
					massLoadingStrategyByType.entrySet() ) {
				if ( entry.getKey().javaClass().isAssignableFrom( expectedType.javaClass() ) ) {
					strategy = (MassLoadingStrategy<? super T, MassLoadingOptions>) entry.getValue();
					break;
				}
			}
		}
		if ( strategy == null ) {
			throw log.entityLoadingStrategyNotRegistered( expectedType );
		}
		return new JavaBeanMassIndexingLoadingStrategy<>( mappingContext, typeContextProvider, strategy, this );
	}

	public static final class Builder implements JavaBeanSelectionLoadingContextBuilder, SelectionLoadingOptionsStep {
		private final JavaBeanMassIndexingMappingContext mappingContext;
		private final LoadingTypeContextProvider typeContextProvider;
		private final JavaBeanLoadingSessionContext sessionContext;
		private Map<PojoRawTypeIdentifier<?>, SelectionLoadingStrategy<?>> selectionLoadingStrategyByType;
		private Map<PojoRawTypeIdentifier<?>, MassLoadingStrategy<?, ?>> massLoadingStrategyByType;
		private final Map<Class<?>, Object> contextData = new HashMap<>();

		public Builder(JavaBeanMassIndexingMappingContext mappingContext,
				LoadingTypeContextProvider typeContextProvider, JavaBeanLoadingSessionContext sessionContext) {
			this.mappingContext = mappingContext;
			this.typeContextProvider = typeContextProvider;
			this.sessionContext = sessionContext;
		}

		@Override
		public SelectionLoadingOptionsStep toAPI() {
			return this;
		}

		@Override
		public <T> void selectionLoadingStrategy(Class<T> type, SelectionLoadingStrategy<T> loadingStrategy) {
			LoadingTypeContext<T> typeContext = typeContextProvider.indexedForExactClass( type );
			PojoRawTypeIdentifier<T> typeIdentifier = typeContext != null
					? typeContext.typeIdentifier() : PojoRawTypeIdentifier.of( type );
			if ( selectionLoadingStrategyByType == null ) {
				selectionLoadingStrategyByType = new LinkedHashMap<>();
			}

			selectionLoadingStrategyByType.put( typeIdentifier, loadingStrategy );
		}

		@Override
		public <T> void massLoadingStrategy(Class<T> type, MassLoadingStrategy<T, ?> loadingStrategy) {
			LoadingTypeContext<T> typeContext = typeContextProvider.indexedForExactClass( type );
			PojoRawTypeIdentifier<T> typeIdentifier = typeContext != null
					? typeContext.typeIdentifier() : PojoRawTypeIdentifier.of( type );
			if ( massLoadingStrategyByType == null ) {
				massLoadingStrategyByType = new LinkedHashMap<>();
			}

			massLoadingStrategyByType.put( typeIdentifier, loadingStrategy );
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
					typeContextProvider, includedTypeIdentifiers, sessionContext.runtimeIntrospector() );
			return new JavaBeanSelectionEntityLoader<>(
					delegate.createEntityLoader( includedTypes, JavaBeanLoadingContext.this ) );
		}
	}
}
