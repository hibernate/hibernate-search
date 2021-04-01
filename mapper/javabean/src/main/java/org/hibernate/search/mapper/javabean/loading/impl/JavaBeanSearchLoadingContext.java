/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.loading.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.search.mapper.javabean.loading.EntityLoader;

import org.hibernate.search.mapper.javabean.loading.LoadingOptions;
import org.hibernate.search.mapper.javabean.log.impl.Log;
import org.hibernate.search.mapper.javabean.massindexing.impl.JavaBeanMassIndexingMappingContext;
import org.hibernate.search.mapper.javabean.massindexing.impl.JavaBeanSessionContextInterceptor;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.mapper.pojo.massindexing.spi.MassIndexingContext;
import org.hibernate.search.mapper.javabean.massindexing.loader.JavaBeanIndexingOptions;
import org.hibernate.search.mapper.pojo.loading.LoadingInterceptor;
import org.hibernate.search.mapper.pojo.massindexing.loader.MassIndexingEntityLoadingStrategy;

public final class JavaBeanSearchLoadingContext implements PojoLoadingContext, MassIndexingContext<JavaBeanIndexingOptions> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Map<PojoRawTypeIdentifier<?>, PojoLoader<?>> loaderByType;
	private final Map<PojoRawTypeIdentifier<?>, MassIndexingEntityLoadingStrategy<?, ?>> indexLoadingStrategyByType;
	private final List<LoadingInterceptor<? super JavaBeanIndexingOptions>> identifierInterceptors;
	private final List<LoadingInterceptor<? super JavaBeanIndexingOptions>> documentInterceptors;

	private JavaBeanSearchLoadingContext(Builder builder) {
		this.loaderByType = builder.loaderByType == null ? Collections.emptyMap() : builder.loaderByType;
		this.indexLoadingStrategyByType = builder.indexeStrategyByType == null ? Collections.emptyMap() : builder.indexeStrategyByType;
		this.identifierInterceptors = builder.identifierInterceptors;
		this.documentInterceptors = builder.documentInterceptors;
	}

	@Override
	public void checkOpen() {
		// Nothing to do: we're always "open",
		// but don't ever try to use createLoader(), as *that* will fail.
	}

	@Override
	public Object loaderKey(PojoRawTypeIdentifier<?> type) {
		return type;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> PojoLoader<? super T> createLoader(Set<PojoRawTypeIdentifier<? extends T>> expectedTypes) {
		PojoRawTypeIdentifier<? extends T> type = expectedTypes.iterator().next();
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
	public <T> MassIndexingEntityLoadingStrategy<? super T, JavaBeanIndexingOptions> indexLoadingStrategy(PojoRawTypeIdentifier<T> expectedType) {
		MassIndexingEntityLoadingStrategy<? super T, JavaBeanIndexingOptions> strategy =
				(MassIndexingEntityLoadingStrategy<T, JavaBeanIndexingOptions>) indexLoadingStrategyByType.get( expectedType );
		if ( strategy == null ) {
			for ( Map.Entry<PojoRawTypeIdentifier<?>, MassIndexingEntityLoadingStrategy<?, ?>> entry : indexLoadingStrategyByType
					.entrySet() ) {
				if ( entry.getKey().javaClass().isAssignableFrom( expectedType.javaClass() ) ) {
					strategy = (MassIndexingEntityLoadingStrategy<? super T, JavaBeanIndexingOptions>) entry.getValue();
					break;
				}
			}
		}
		if ( strategy == null ) {
			throw log.indexLoaderNotRegistered( expectedType );
		}
		return strategy;
	}

	@Override
	public List<LoadingInterceptor<? super JavaBeanIndexingOptions>> identifierInterceptors() {
		return identifierInterceptors;
	}

	@Override
	public List<LoadingInterceptor<? super JavaBeanIndexingOptions>> documentInterceptors() {
		return documentInterceptors;
	}

	public static final class Builder implements JavaBeanLoadingContextBuilder, LoadingOptions {
		private final LoadingTypeContextProvider typeContextProvider;
		private Map<PojoRawTypeIdentifier<?>, PojoLoader<?>> loaderByType;
		private Map<PojoRawTypeIdentifier<?>, MassIndexingEntityLoadingStrategy<?, ?>> indexeStrategyByType;
		private final List<LoadingInterceptor<? super JavaBeanIndexingOptions>> identifierInterceptors = new ArrayList<>();
		private final List<LoadingInterceptor<? super JavaBeanIndexingOptions>> documentInterceptors = new ArrayList<>();

		public Builder(LoadingTypeContextProvider typeContextProvider, JavaBeanMassIndexingMappingContext mappingContext) {
			this.typeContextProvider = typeContextProvider;
			identifierInterceptors.add( JavaBeanSessionContextInterceptor.of( mappingContext ) );
			documentInterceptors.add( JavaBeanSessionContextInterceptor.of( mappingContext ) );
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
		public <T> void massIndexingLoadingStrategy(Class<T> type, MassIndexingEntityLoadingStrategy<T, JavaBeanIndexingOptions> loadingStrategy) {
			LoadingTypeContext<T> typeContext = typeContextProvider.indexedForExactClass( type );
			PojoRawTypeIdentifier<T> typeIdentifier = typeContext != null
					? typeContext.typeIdentifier() : PojoRawTypeIdentifier.of( type );
			if ( indexeStrategyByType == null ) {
				indexeStrategyByType = new LinkedHashMap<>();
			}

			indexeStrategyByType.put( typeIdentifier, loadingStrategy );
		}

		@Override
		public void identifierInterceptor(LoadingInterceptor<JavaBeanIndexingOptions> interceptor) {
			identifierInterceptors.add( interceptor );
		}

		@Override
		public void documentInterceptor(LoadingInterceptor<JavaBeanIndexingOptions> interceptor) {
			documentInterceptors.add( interceptor );
		}

		@Override
		public JavaBeanSearchLoadingContext build() {
			return new JavaBeanSearchLoadingContext( this );
		}
	}
}
