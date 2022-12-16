/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.loading.impl;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingOptions;
import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.loading.SelectionLoadingOptions;
import org.hibernate.search.mapper.pojo.standalone.loading.SelectionLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.loading.dsl.SelectionLoadingOptionsStep;
import org.hibernate.search.mapper.pojo.standalone.logging.impl.Log;
import org.hibernate.search.mapper.pojo.standalone.massindexing.impl.StandalonePojoMassIndexingLoadingStrategy;
import org.hibernate.search.mapper.pojo.standalone.massindexing.impl.StandalonePojoMassIndexingMappingContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingTypeContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionEntityLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingStrategy;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingLoadingStrategy;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class StandalonePojoLoadingContext
		implements PojoSelectionLoadingContext, PojoMassIndexingContext, MassLoadingOptions, SelectionLoadingOptions {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final StandalonePojoMassIndexingMappingContext mappingContext;
	private final LoadingTypeContextProvider typeContextProvider;

	private int batchSize = 10;
	private final Map<Class<?>, Object> contextData;

	private StandalonePojoLoadingContext(Builder builder) {
		this.mappingContext = builder.mappingContext;
		this.typeContextProvider = builder.typeContextProvider;
		this.contextData = builder.contextData;
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
				.map( s -> new StandalonePojoSelectionLoadingStrategy<>( s ) );
	}

	@Override
	public <T> PojoMassIndexingLoadingStrategy<? super T, ?> loadingStrategy(
			PojoRawTypeIdentifier<T> expectedType) {
		LoadingTypeContext<T> typeContext = typeContextProvider.forExactType( expectedType );
		Optional<MassLoadingStrategy<? super T, ?>> strategyOptional = typeContext.massLoadingStrategy();
		if ( !strategyOptional.isPresent() ) {
			throw log.entityLoadingStrategyNotRegistered( typeContext.typeIdentifier() );
		}
		return new StandalonePojoMassIndexingLoadingStrategy<>( mappingContext, typeContextProvider,
				strategyOptional.get(), this );
	}

	public static final class Builder implements StandalonePojoSelectionLoadingContextBuilder, SelectionLoadingOptionsStep {
		private final StandalonePojoMassIndexingMappingContext mappingContext;
		private final LoadingTypeContextProvider typeContextProvider;
		private final Map<Class<?>, Object> contextData = new HashMap<>();

		public Builder(StandalonePojoMassIndexingMappingContext mappingContext,
				LoadingTypeContextProvider typeContextProvider) {
			this.mappingContext = mappingContext;
			this.typeContextProvider = typeContextProvider;
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
		public StandalonePojoLoadingContext build() {
			return new StandalonePojoLoadingContext( this );
		}
	}

	private class StandalonePojoSelectionLoadingStrategy<E> implements PojoSelectionLoadingStrategy<E> {

		private final SelectionLoadingStrategy<E> delegate;

		private StandalonePojoSelectionLoadingStrategy(SelectionLoadingStrategy<E> delegate) {
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
		public PojoSelectionEntityLoader<E> createLoader(
				Set<? extends PojoLoadingTypeContext<? extends E>> expectedTypes) {
			Set<PojoRawTypeIdentifier<? extends E>> includedTypeIdentifiers = new LinkedHashSet<>();
			for ( PojoLoadingTypeContext<? extends E> expectedType : expectedTypes ) {
				includedTypeIdentifiers.add( expectedType.typeIdentifier() );
			}
			StandalonePojoLoadingTypeGroup<E> includedTypes = new StandalonePojoLoadingTypeGroup<>(
					typeContextProvider, includedTypeIdentifiers, runtimeIntrospector() );
			return new StandalonePojoSelectionEntityLoader<>(
					delegate.createEntityLoader( includedTypes, StandalonePojoLoadingContext.this ) );
		}
	}
}
