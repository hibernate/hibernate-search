/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl;

import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaRootNodeBuilder;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;
import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexSchemaRootNodeBuilder;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

public class StubIndexManagerBuilder implements IndexManagerBuilder {

	public static final StaticCounters.Key INSTANCE_COUNTER_KEY = StaticCounters.createKey();
	public static final StaticCounters.Key CLOSE_ON_FAILURE_COUNTER_KEY = StaticCounters.createKey();
	public static final StaticCounters.Key BUILD_COUNTER_KEY = StaticCounters.createKey();

	private final StubBackend backend;
	private final String name;
	private final String mappedTypeName;
	private final StubIndexSchemaRootNodeBuilder schemaRootNodeBuilder;

	private boolean closed = false;

	public StubIndexManagerBuilder(StubBackend backend, String name, String mappedTypeName1,
			String mappedTypeName) {
		StaticCounters.get().increment( INSTANCE_COUNTER_KEY );
		this.backend = backend;
		this.name = name;
		this.mappedTypeName = mappedTypeName;
		this.schemaRootNodeBuilder = new StubIndexSchemaRootNodeBuilder( backend.getBehavior(), name, mappedTypeName );
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
	public IndexSchemaRootNodeBuilder getSchemaRootNodeBuilder() {
		return schemaRootNodeBuilder;
	}

	@Override
	public IndexManagerImplementor build() {
		if ( closed ) {
			throw new AssertionFailure( "Unexpected call to build after a call to build() or to closeOnFailure()" );
		}
		StaticCounters.get().increment( BUILD_COUNTER_KEY );
		closed = true;
		return new StubIndexManager( backend, name, mappedTypeName, schemaRootNodeBuilder.build() );
	}
}
