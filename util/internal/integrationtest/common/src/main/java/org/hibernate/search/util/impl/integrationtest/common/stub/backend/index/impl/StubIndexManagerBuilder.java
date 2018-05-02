/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl;

import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaRootNodeBuilder;
import org.hibernate.search.engine.backend.index.spi.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.impl.StubDocumentElement;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexSchemaRootNodeBuilder;

public class StubIndexManagerBuilder implements IndexManagerBuilder<StubDocumentElement> {

	private final StubBackend backend;
	private final String name;
	private final StubIndexSchemaRootNodeBuilder schemaRootNodeBuilder = new StubIndexSchemaRootNodeBuilder();

	public StubIndexManagerBuilder(StubBackend backend, String name) {
		this.backend = backend;
		this.name = name;
	}

	@Override
	public IndexSchemaRootNodeBuilder getSchemaRootNodeBuilder() {
		return schemaRootNodeBuilder;
	}

	@Override
	public IndexManager<StubDocumentElement> build() {
		return new StubIndexManager( backend, name, schemaRootNodeBuilder.build() );
	}
}
