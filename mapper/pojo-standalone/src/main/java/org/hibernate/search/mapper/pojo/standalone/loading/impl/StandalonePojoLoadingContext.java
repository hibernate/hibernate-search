/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.loading.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingOptions;
import org.hibernate.search.mapper.pojo.standalone.loading.SelectionLoadingOptions;
import org.hibernate.search.mapper.pojo.standalone.loading.dsl.SelectionLoadingOptionsStep;
import org.hibernate.search.mapper.pojo.standalone.massindexing.impl.StandalonePojoMassIndexingMappingContext;

public final class StandalonePojoLoadingContext
		implements PojoSelectionLoadingContext, PojoMassIndexingContext, MassLoadingOptions, SelectionLoadingOptions {

	private final StandalonePojoMassIndexingMappingContext mappingContext;

	private int batchSize = 10;
	private final Map<Class<?>, Object> contextData;

	private StandalonePojoLoadingContext(Builder builder) {
		this.mappingContext = builder.mappingContext;
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

	public StandalonePojoMassIndexingMappingContext mapping() {
		return mappingContext;
	}

	public static final class Builder implements StandalonePojoSelectionLoadingContextBuilder, SelectionLoadingOptionsStep {
		private final StandalonePojoMassIndexingMappingContext mappingContext;
		private final Map<Class<?>, Object> contextData = new HashMap<>();

		public Builder(StandalonePojoMassIndexingMappingContext mappingContext) {
			this.mappingContext = mappingContext;
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

}
