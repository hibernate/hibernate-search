/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl;

import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexRootBuilder;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;
import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.dsl.impl.StubIndexRootBuilder;
import org.hibernate.search.util.impl.test.extension.StaticCounters;

public class StubIndexManagerBuilder implements IndexManagerBuilder {

	public static final StaticCounters.Key INSTANCE_COUNTER_KEY = StaticCounters.createKey();
	public static final StaticCounters.Key CLOSE_ON_FAILURE_COUNTER_KEY = StaticCounters.createKey();
	public static final StaticCounters.Key BUILD_COUNTER_KEY = StaticCounters.createKey();

	private final StubBackend backend;
	private final String name;
	private final StubIndexRootBuilder schemaRootNodeBuilder;

	private boolean closed = false;

	public StubIndexManagerBuilder(StubBackend backend, String name, String mappedTypeName) {
		StaticCounters.get().increment( INSTANCE_COUNTER_KEY );
		this.backend = backend;
		this.name = name;
		this.schemaRootNodeBuilder = new StubIndexRootBuilder( backend.getBehavior(), name, mappedTypeName );
	}

	@Override
	public void closeOnFailure() {
		/*
		 * This is important so that multiple calls to close on a single index manager builder
		 * won't be interpreted as closing multiple objects in test assertions.
		 */
		if ( closed ) {
			return;
		}
		StaticCounters.get().increment( CLOSE_ON_FAILURE_COUNTER_KEY );
		closed = true;
	}

	@Override
	public IndexRootBuilder schemaRootNodeBuilder() {
		return schemaRootNodeBuilder;
	}

	@Override
	public IndexManagerImplementor build() {
		if ( closed ) {
			throw new AssertionFailure( "Unexpected call to build after a call to build() or to closeOnFailure()" );
		}
		StaticCounters.get().increment( BUILD_COUNTER_KEY );
		closed = true;
		return new StubIndexManager( backend, name, schemaRootNodeBuilder.buildModel() );
	}
}
