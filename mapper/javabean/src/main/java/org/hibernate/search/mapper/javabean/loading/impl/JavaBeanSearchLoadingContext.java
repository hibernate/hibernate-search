/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.loading.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.mapper.javabean.loading.EntityLoader;
import org.hibernate.search.mapper.javabean.loading.LoadingOptions;
import org.hibernate.search.mapper.javabean.log.impl.Log;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingContextBuilder;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class JavaBeanSearchLoadingContext implements PojoLoadingContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Map<PojoRawTypeIdentifier<?>, PojoLoader<?>> loaderByType;

	private JavaBeanSearchLoadingContext(Builder builder) {
		this.loaderByType = builder.loaderByType == null ? Collections.emptyMap() : builder.loaderByType;
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
	public <T> PojoLoader<T> createLoader(Set<PojoRawTypeIdentifier<? extends T>> expectedTypes) {
		PojoRawTypeIdentifier<? extends T> type = expectedTypes.iterator().next();
		PojoLoader<T> loader = (PojoLoader<T>) loaderByType.get( type );
		if ( loader == null ) {
			throw log.entityLoaderNotRegistered( type );
		}
		return loader;
	}

	public static final class Builder implements PojoLoadingContextBuilder<LoadingOptions>, LoadingOptions {
		private final LoadingTypeContextProvider typeContextProvider;
		private Map<PojoRawTypeIdentifier<?>, PojoLoader<?>> loaderByType;

		public Builder(LoadingTypeContextProvider typeContextProvider) {
			this.typeContextProvider = typeContextProvider;
		}

		@Override
		public LoadingOptions toAPI() {
			return this;
		}

		@Override
		public <T> LoadingOptions registerLoader(Class<T> type, EntityLoader<T> loader) {
			LoadingTypeContext<T> typeContext = typeContextProvider.indexedForExactClass( type );
			if ( typeContext == null ) {
				throw log.notIndexedEntityType( type );
			}
			if ( loaderByType == null ) {
				loaderByType = new LinkedHashMap<>();
			}
			loaderByType.put( typeContext.typeIdentifier(), new JavaBeanLoader<>( loader ) );
			return this;
		}

		@Override
		public PojoLoadingContext build() {
			return new JavaBeanSearchLoadingContext( this );
		}
	}
}
