/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.util.common.stub.backend.document.model.impl;

import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaNestingContext;
import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.spi.ObjectFieldIndexSchemaCollector;
import org.hibernate.search.integrationtest.util.common.stub.backend.document.model.StubIndexSchemaNode;

class StubObjectFieldIndexSchemaCollector extends StubIndexSchemaCollector
		implements ObjectFieldIndexSchemaCollector {

	private final String relativeName;

	StubObjectFieldIndexSchemaCollector(String relativeName, StubIndexSchemaNode.Builder builder) {
		super( builder );
		this.relativeName = relativeName;
	}

	@Override
	public IndexSchemaObjectField withContext(IndexSchemaNestingContext context) {
		return new StubIndexSchemaObjectField( relativeName, builder, context, true );
	}
}
