/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.dsl.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaRootNodeBuilder;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.converter.spi.DocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaDataNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexModel;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.dsl.impl.StubIndexFieldTypeFactory;

public class StubIndexSchemaRootNodeBuilder extends AbstractStubIndexSchemaObjectNodeBuilder
		implements IndexSchemaRootNodeBuilder {

	private final StubBackendBehavior backendBehavior;
	private final String indexName;
	private DocumentIdentifierValueConverter<?> idDslConverter;

	public StubIndexSchemaRootNodeBuilder(StubBackendBehavior backendBehavior, String indexName) {
		this( backendBehavior, indexName, StubIndexSchemaDataNode.schema() );
	}

	private StubIndexSchemaRootNodeBuilder(StubBackendBehavior backendBehavior, String indexName,
			StubIndexSchemaDataNode.Builder builder) {
		super( builder );
		this.backendBehavior = backendBehavior;
		this.indexName = indexName;
	}

	@Override
	public IndexFieldTypeFactory createTypeFactory(IndexFieldTypeDefaultsProvider defaultsProvider) {
		return new StubIndexFieldTypeFactory( defaultsProvider );
	}

	@Override
	public void explicitRouting() {
		schemaDataNodeBuilder.explicitRouting();
	}

	@Override
	public void idDslConverter(DocumentIdentifierValueConverter<?> idDslConverter) {
		this.idDslConverter = idDslConverter;
	}

	@Override
	public EventContext eventContext() {
		return getIndexEventContext().append( EventContexts.indexSchemaRoot() );
	}

	public StubIndexModel buildModel() {
		Map<String, StubIndexNode> fields = new LinkedHashMap<>();
		StubIndexNode root = new StubIndexNode( schemaDataNodeBuilder.build(), null, ObjectStructure.FLATTENED );
		contributeChildren( fields::put );
		return new StubIndexModel( indexName, idDslConverter, root, fields );
	}

	@Override
	StubIndexSchemaRootNodeBuilder getRootNodeBuilder() {
		return this;
	}

	EventContext getIndexEventContext() {
		return EventContexts.fromIndexName( indexName );
	}

	StubBackendBehavior getBackendBehavior() {
		return backendBehavior;
	}

	String getIndexName() {
		return indexName;
	}
}
