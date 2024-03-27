/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.dsl.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.backend.analysis.AnalyzerDescriptor;
import org.hibernate.search.engine.backend.analysis.NormalizerDescriptor;
import org.hibernate.search.engine.backend.analysis.spi.AnalysisDescriptorRegistry;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexRootBuilder;
import org.hibernate.search.engine.backend.document.model.spi.IndexIdentifier;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.converter.FromDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaDataNode;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexField;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexModel;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexRoot;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.dsl.impl.StubIndexFieldTypeFactory;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.impl.StubIndexCompositeNodeType;

public class StubIndexRootBuilder extends AbstractStubIndexCompositeNodeBuilder
		implements IndexRootBuilder {

	private final StubBackendBehavior backendBehavior;
	private final String indexName;
	private final String mappedTypeName;

	private DslConverter<?, String> idDslConverter;
	private ProjectionConverter<String, ?> idProjectionConverter;

	public StubIndexRootBuilder(StubBackendBehavior backendBehavior, String indexName, String mappedTypeName) {
		this( backendBehavior, indexName, mappedTypeName, StubIndexSchemaDataNode.schema() );
	}

	private StubIndexRootBuilder(StubBackendBehavior backendBehavior, String indexName, String mappedTypeName,
			StubIndexSchemaDataNode.Builder builder) {
		super( builder );
		this.backendBehavior = backendBehavior;
		this.indexName = indexName;
		this.mappedTypeName = mappedTypeName;
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
	public <I> void idDslConverter(Class<I> valueType, ToDocumentValueConverter<I, String> converter) {
		this.idDslConverter = new DslConverter<>( valueType, converter );
	}

	@Override
	public <I> void idProjectionConverter(Class<I> valueType, FromDocumentValueConverter<String, I> converter) {
		this.idProjectionConverter = new ProjectionConverter<>( valueType, converter );
	}

	@Override
	public EventContext eventContext() {
		return getIndexEventContext().append( EventContexts.indexSchemaRoot() );
	}

	public StubIndexModel buildModel() {
		IndexIdentifier identifier = new IndexIdentifier( idDslConverter, idProjectionConverter );
		Map<String, StubIndexField> allFields = new LinkedHashMap<>();
		Map<String, StubIndexField> staticChildren = new LinkedHashMap<>();
		StubIndexCompositeNodeType type = new StubIndexCompositeNodeType.Builder( ObjectStructure.DEFAULT ).build();
		type.apply( schemaDataNodeBuilder );
		StubIndexRoot root = new StubIndexRoot( type, staticChildren, schemaDataNodeBuilder.build() );
		contributeChildren( root, staticChildren, allFields::put );
		return new StubIndexModel( StubAnalysisDescriptorRegistry.REGISTRY, indexName, mappedTypeName, identifier, root,
				allFields );
	}

	@Override
	StubIndexRootBuilder getRootNodeBuilder() {
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

	private static class StubAnalysisDescriptorRegistry implements AnalysisDescriptorRegistry {
		private static final StubAnalysisDescriptorRegistry REGISTRY = new StubAnalysisDescriptorRegistry();

		@Override
		public Optional<? extends AnalyzerDescriptor> analyzerDescriptor(String name) {
			return Optional.empty();
		}

		@Override
		public Collection<? extends AnalyzerDescriptor> analyzerDescriptors() {
			return Collections.emptySet();
		}

		@Override
		public Optional<? extends NormalizerDescriptor> normalizerDescriptor(String name) {
			return Optional.empty();
		}

		@Override
		public Collection<? extends NormalizerDescriptor> normalizerDescriptors() {
			return Collections.emptySet();
		}
	}
}
