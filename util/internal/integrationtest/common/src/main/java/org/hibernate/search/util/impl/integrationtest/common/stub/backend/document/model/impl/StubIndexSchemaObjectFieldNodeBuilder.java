/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl;

import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectFieldNodeBuilder;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.impl.StubIndexObjectFieldReference;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;

class StubIndexSchemaObjectFieldNodeBuilder extends AbstractStubIndexSchemaObjectNodeBuilder
		implements IndexSchemaObjectFieldNodeBuilder {

	private final AbstractStubIndexSchemaObjectNodeBuilder parent;
	private final boolean included;
	private IndexObjectFieldReference reference;

	StubIndexSchemaObjectFieldNodeBuilder(AbstractStubIndexSchemaObjectNodeBuilder parent,
			StubIndexSchemaNode.Builder builder, boolean included) {
		super( builder );
		this.parent = parent;
		this.included = included;
	}

	@Override
	public EventContext getEventContext() {
		return getRootNodeBuilder().getIndexEventContext()
				.append( EventContexts.fromIndexFieldAbsolutePath( builder.getAbsolutePath() ) );
	}

	@Override
	public IndexObjectFieldReference toReference() {
		if ( reference == null ) {
			reference = new StubIndexObjectFieldReference(
					builder.getAbsolutePath(), builder.getRelativeName(), included
			);
		}
		return reference;
	}

	@Override
	StubIndexSchemaRootNodeBuilder getRootNodeBuilder() {
		return parent.getRootNodeBuilder();
	}
}
