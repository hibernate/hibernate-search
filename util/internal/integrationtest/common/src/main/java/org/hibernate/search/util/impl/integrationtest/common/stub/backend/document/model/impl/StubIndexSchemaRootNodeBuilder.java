/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl;

import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaRootNodeBuilder;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.dsl.impl.StubIndexFieldTypeFactory;

public class StubIndexSchemaRootNodeBuilder extends AbstractStubIndexSchemaObjectNodeBuilder
		implements IndexSchemaRootNodeBuilder {

	private final StubBackendBehavior backendBehavior;
	private final String indexName;

	public StubIndexSchemaRootNodeBuilder(StubBackendBehavior backendBehavior, String indexName, String mappedTypeName) {
		this( backendBehavior, indexName, StubIndexSchemaNode.schema(), mappedTypeName );
	}

	private StubIndexSchemaRootNodeBuilder(StubBackendBehavior backendBehavior, String indexName,
			StubIndexSchemaNode.Builder builder, String mappedTypeName) {
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
		builder.explicitRouting();
	}

	@Override
	public void idDslConverter(ToDocumentIdentifierValueConverter<?> idConverter) {
		builder.idDslConverter( idConverter );
	}

	@Override
	public EventContext getEventContext() {
		return getIndexEventContext().append( EventContexts.indexSchemaRoot() );
	}

	public StubIndexSchemaNode build() {
		return builder.build();
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
