/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.util.common.stub.backend.document.model.impl;

import org.hibernate.search.engine.backend.document.model.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaCollector;
import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaNestingContext;
import org.hibernate.search.engine.backend.document.model.spi.ObjectFieldIndexSchemaCollector;
import org.hibernate.search.integrationtest.util.common.stub.backend.document.model.StubIndexSchemaNode;

class StubIndexSchemaCollector implements IndexSchemaCollector {

	protected final StubIndexSchemaNode.Builder builder;

	StubIndexSchemaCollector(StubIndexSchemaNode.Builder builder) {
		this.builder = builder;
	}

	@Override
	public ObjectFieldIndexSchemaCollector objectField(String relativeName, ObjectFieldStorage storage) {
		StubIndexSchemaNode.Builder childBuilder = StubIndexSchemaNode.objectField( storage );
		builder.child( relativeName, childBuilder );
		return new StubObjectFieldIndexSchemaCollector( relativeName, childBuilder );
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
