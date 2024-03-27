/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.scope.spi.IndexScope;
import org.hibernate.search.engine.backend.scope.spi.IndexScopeBuilder;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexModel;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexScope;

public class StubIndexScope implements IndexScope {
	private final StubSearchIndexScope searchScope;

	private StubIndexScope(Builder builder) {
		Set<StubIndexModel> immutableIndexModels =
				Collections.unmodifiableSet( new LinkedHashSet<>( builder.indexModels ) );
		searchScope = new StubSearchIndexScope( builder.mappingContext, builder.backend, immutableIndexModels );
	}

	@Override
	public StubSearchIndexScope searchScope() {
		return searchScope;
	}

	static class Builder implements IndexScopeBuilder {

		private final StubBackend backend;
		private final BackendMappingContext mappingContext;
		private final Set<StubIndexModel> indexModels = new LinkedHashSet<>();

		Builder(StubBackend backend, BackendMappingContext mappingContext, StubIndexModel model) {
			this.backend = backend;
			this.mappingContext = mappingContext;
			this.indexModels.add( model );
		}

		void add(StubBackend backend, StubIndexModel model) {
			if ( !this.backend.equals( backend ) ) {
				throw new IllegalStateException(
						"Attempt to build a scope spanning two distinct backends; this is not possible." );
			}
			indexModels.add( model );
		}

		@Override
		public StubIndexScope build() {
			return new StubIndexScope( this );
		}
	}
}
