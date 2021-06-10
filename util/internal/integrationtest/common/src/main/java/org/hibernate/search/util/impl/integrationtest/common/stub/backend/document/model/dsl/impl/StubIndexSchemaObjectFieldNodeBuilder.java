/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.dsl.impl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectFieldNodeBuilder;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.impl.StubIndexObjectFieldReference;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaDataNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexCompositeNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexField;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexObjectField;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.impl.StubIndexCompositeNodeType;

class StubIndexSchemaObjectFieldNodeBuilder extends AbstractStubIndexSchemaObjectNodeBuilder
		implements IndexSchemaObjectFieldNodeBuilder, StubIndexSchemaFieldBuilder {

	private final AbstractStubIndexSchemaObjectNodeBuilder parent;
	private final IndexFieldInclusion inclusion;
	private final StubIndexCompositeNodeType type;

	private boolean multiValued;

	private IndexObjectFieldReference reference;

	StubIndexSchemaObjectFieldNodeBuilder(AbstractStubIndexSchemaObjectNodeBuilder parent,
			StubIndexSchemaDataNode.Builder schemaNodeBuilder, IndexFieldInclusion inclusion,
			StubIndexCompositeNodeType type) {
		super( schemaNodeBuilder );
		this.parent = parent;
		this.inclusion = inclusion;
		this.type = type;
	}

	@Override
	public EventContext eventContext() {
		return getRootNodeBuilder().getIndexEventContext()
				.append( EventContexts.fromIndexFieldAbsolutePath( schemaDataNodeBuilder.getAbsolutePath() ) );
	}

	@Override
	public void multiValued() {
		multiValued = true;
		schemaDataNodeBuilder.multiValued( true );
	}

	@Override
	public IndexObjectFieldReference toReference() {
		if ( reference == null ) {
			reference = new StubIndexObjectFieldReference(
					schemaDataNodeBuilder.getAbsolutePath(), schemaDataNodeBuilder.getRelativeName(), inclusion
			);
		}
		return reference;
	}

	@Override
	public StubIndexField build(StubIndexCompositeNode parent, BiConsumer<String, StubIndexField> fieldCollector) {
		Map<String, StubIndexField> staticChildren = new LinkedHashMap<>();
		StubIndexObjectField node = new StubIndexObjectField( parent, schemaDataNodeBuilder.getRelativeName(), type,
				inclusion, multiValued, staticChildren, schemaDataNodeBuilder.build() );
		contributeChildren( node, staticChildren, fieldCollector );
		return node;
	}

	@Override
	StubIndexSchemaRootNodeBuilder getRootNodeBuilder() {
		return parent.getRootNodeBuilder();
	}
}
