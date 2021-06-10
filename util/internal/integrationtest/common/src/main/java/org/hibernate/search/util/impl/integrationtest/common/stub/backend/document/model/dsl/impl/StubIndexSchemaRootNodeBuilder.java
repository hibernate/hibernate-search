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
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexField;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexRoot;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.dsl.impl.StubIndexFieldTypeFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.impl.StubIndexCompositeNodeType;

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
		Map<String, StubIndexField> allFields = new LinkedHashMap<>();
		Map<String, StubIndexField> staticChildren = new LinkedHashMap<>();
		StubIndexCompositeNodeType type = new StubIndexCompositeNodeType.Builder( ObjectStructure.DEFAULT ).build();
		type.apply( schemaDataNodeBuilder );
		StubIndexRoot root = new StubIndexRoot( type, staticChildren, schemaDataNodeBuilder.build() );
		contributeChildren( root, staticChildren, allFields::put );
		return new StubIndexModel( indexName, idDslConverter, root, allFields );
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
