/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaCollector;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaNestingContext;
import org.hibernate.search.engine.backend.document.model.dsl.spi.ObjectFieldIndexSchemaCollector;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;

class StubIndexSchemaCollector implements IndexSchemaCollector {

	protected final StubIndexSchemaNode.Builder builder;

	StubIndexSchemaCollector(StubIndexSchemaNode.Builder builder) {
		this.builder = builder;
	}

	@Override
	public ObjectFieldIndexSchemaCollector objectField(String relativeName, ObjectFieldStorage storage) {
		StubIndexSchemaNode.Builder childBuilder = StubIndexSchemaNode.objectField( builder, relativeName, storage );
		builder.child( childBuilder );
		return new StubObjectFieldIndexSchemaCollector( childBuilder );
	}

	@Override
	public IndexSchemaElement withContext(IndexSchemaNestingContext context) {
		return new StubIndexSchemaElement( builder, context );
	}

	@Override
	public void explicitRouting() {
		builder.explicitRouting();
	}
}
