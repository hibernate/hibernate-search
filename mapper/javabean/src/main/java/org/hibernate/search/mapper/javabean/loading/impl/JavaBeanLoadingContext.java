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
import java.util.Map;
import java.util.Set;

import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.mapper.javabean.loading.MassLoadingOptions;
import org.hibernate.search.mapper.javabean.loading.EntityLoader;
import org.hibernate.search.mapper.javabean.loading.MassLoadingStrategy;
import org.hibernate.search.mapper.javabean.loading.LoadingOptions;
import org.hibernate.search.mapper.javabean.log.impl.Log;
import org.hibernate.search.mapper.javabean.massindexing.impl.JavaBeanMassIndexingLoadingStrategy;
import org.hibernate.search.mapper.javabean.massindexing.impl.JavaBeanMassIndexingMappingContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingTypeContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingLoadingStrategy;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class JavaBeanLoadingContext implements PojoLoadingContext, PojoMassIndexingContext, MassLoadingOptions {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final JavaBeanMassIndexingMappingContext mappingContext;
	private final LoadingTypeContextProvider typeContextProvider;
	private final DetachedBackendSessionContext sessionContext;
	private final Map<PojoRawTypeIdentifier<?>, PojoLoader<?>> loaderByType;
	private final Map<PojoRawTypeIdentifier<?>, MassLoadingStrategy<?, ?>> massLoadingStrategyByType;

	private int batchSize = 10;
	private final Map<Class<?>, Object> contextData = new HashMap<>();

	private JavaBeanLoadingContext(Builder builder) {
		this.mappingContext = builder.mappingContext;
		this.typeContextProvider = builder.typeContextProvider;
		this.sessionContext = builder.sessionContext;
		this.loaderByType = builder.loaderByType == null ? Collections.emptyMap() : builder.loaderByType;
		this.massLoadingStrategyByType = builder.massLoadingStrategyByType == null ? Collections.emptyMap() : builder.massLoadingStrategyByType;
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
	public Object loaderKey(PojoLoadingTypeContext<?> type) {
		return type.typeIdentifier();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> PojoLoader<? super T> createLoader(Set<PojoLoadingTypeContext<? extends T>> expectedTypes) {
		PojoRawTypeIdentifier<? extends T> type = expectedTypes.iterator().next().typeIdentifier();
		PojoLoader<? super T> loader = (PojoLoader<T>) loaderByType.get( type );
		if ( loader == null ) {
			for ( Map.Entry<PojoRawTypeIdentifier<?>, PojoLoader<?>> entry : loaderByType.entrySet() ) {
				if ( entry.getKey().javaClass().isAssignableFrom( type.javaClass() ) ) {
					loader = (PojoLoader<? super T>) entry.getValue();
					break;
				}
			}
		}
		if ( loader == null ) {
			throw log.entityLoaderNotRegistered( type );
		}
		return loader;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> PojoMassIndexingLoadingStrategy<? super T, ?> loadingStrategy(PojoRawTypeIdentifier<T> expectedType) {
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

	public static final class Builder implements JavaBeanLoadingContextBuilder, LoadingOptions {
		private final JavaBeanMassIndexingMappingContext mappingContext;
		private final LoadingTypeContextProvider typeContextProvider;
		private final DetachedBackendSessionContext sessionContext;
		private Map<PojoRawTypeIdentifier<?>, PojoLoader<?>> loaderByType;
		private Map<PojoRawTypeIdentifier<?>, MassLoadingStrategy<?, ?>> massLoadingStrategyByType;

		public Builder(JavaBeanMassIndexingMappingContext mappingContext,
				LoadingTypeContextProvider typeContextProvider,
				DetachedBackendSessionContext sessionContext) {
			this.mappingContext = mappingContext;
			this.typeContextProvider = typeContextProvider;
			this.sessionContext = sessionContext;
		}

		@Override
		public LoadingOptions toAPI() {
			return this;
		}

		@Override
		public <T> LoadingOptions registerLoader(Class<T> type, EntityLoader<T> loader) {
			LoadingTypeContext<T> typeContext = typeContextProvider.indexedForExactClass( type );
			PojoRawTypeIdentifier<T> typeIdentifier = typeContext != null
					? typeContext.typeIdentifier() : PojoRawTypeIdentifier.of( type );
			if ( loaderByType == null ) {
				loaderByType = new LinkedHashMap<>();
			}
			loaderByType.put( typeIdentifier, new JavaBeanLoader<>( loader ) );
			return this;
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
		public JavaBeanLoadingContext build() {
			return new JavaBeanLoadingContext( this );
		}
	}
}
