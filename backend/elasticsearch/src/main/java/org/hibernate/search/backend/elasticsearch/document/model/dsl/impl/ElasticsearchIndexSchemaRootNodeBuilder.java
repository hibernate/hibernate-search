/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionRegistry;
import org.hibernate.search.backend.elasticsearch.document.model.impl.AbstractElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaValueFieldNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaValueFieldTemplate;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeCollector;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectFieldNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectFieldTemplate;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaRootNode;
import org.hibernate.search.backend.elasticsearch.index.DynamicMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DynamicType;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.NamedDynamicTemplate;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RoutingType;
import org.hibernate.search.backend.elasticsearch.index.layout.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchIndexFieldTypeFactory;
import org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl.ElasticsearchIndexFieldTypeFactoryProvider;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.StringToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaRootNodeBuilder;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.reporting.EventContext;

public class ElasticsearchIndexSchemaRootNodeBuilder extends AbstractElasticsearchIndexSchemaObjectNodeBuilder
		implements IndexSchemaRootNodeBuilder {

	private final ElasticsearchIndexFieldTypeFactoryProvider typeFactoryProvider;
	private final EventContext indexEventContext;
	private final List<IndexSchemaRootContributor> schemaRootContributors = new ArrayList<>();

	private final IndexNames indexNames;
	private final String mappedTypeName;
	private final ElasticsearchAnalysisDefinitionRegistry analysisDefinitionRegistry;
	private final DynamicType defaultDynamicType;

	private RoutingType routing = null;
	private ToDocumentIdentifierValueConverter<?> idDslConverter;

	public ElasticsearchIndexSchemaRootNodeBuilder(ElasticsearchIndexFieldTypeFactoryProvider typeFactoryProvider,
			EventContext indexEventContext,
			IndexNames indexNames, String mappedTypeName,
			ElasticsearchAnalysisDefinitionRegistry analysisDefinitionRegistry, DynamicMapping dynamicMapping) {
		this.typeFactoryProvider = typeFactoryProvider;
		this.indexEventContext = indexEventContext;
		this.indexNames = indexNames;
		this.mappedTypeName = mappedTypeName;
		this.analysisDefinitionRegistry = analysisDefinitionRegistry;
		this.defaultDynamicType = DynamicType.create( dynamicMapping );
	}

	@Override
	public EventContext eventContext() {
		return getIndexEventContext()
				.append( EventContexts.indexSchemaRoot() );
	}

	@Override
	public ElasticsearchIndexFieldTypeFactory createTypeFactory(IndexFieldTypeDefaultsProvider defaultsProvider) {
		return typeFactoryProvider.create( indexEventContext, defaultsProvider );
	}

	@Override
	public void explicitRouting() {
		this.routing = RoutingType.REQUIRED;
	}

	@Override
	public void idDslConverter(ToDocumentIdentifierValueConverter<?> idDslConverter) {
		this.idDslConverter = idDslConverter;
	}

	public void addSchemaRootContributor(IndexSchemaRootContributor schemaRootContributor) {
		schemaRootContributors.add( schemaRootContributor );
	}

	public ElasticsearchIndexModel build() {
		RootTypeMapping mapping = new RootTypeMapping();
		if ( routing != null ) {
			mapping.setRouting( routing );
		}

		for ( IndexSchemaRootContributor schemaRootContributor : schemaRootContributors ) {
			schemaRootContributor.contribute( mapping );
		}

		mapping.setDynamic( resolveSelfDynamicType( defaultDynamicType ) );

		final Map<String, ElasticsearchIndexSchemaObjectFieldNode> objectFieldNodes = new HashMap<>();
		final Map<String, ElasticsearchIndexSchemaValueFieldNode<?>> valueFieldNodes = new HashMap<>();
		final List<ElasticsearchIndexSchemaObjectFieldTemplate> objectFieldTemplates = new ArrayList<>();
		final List<ElasticsearchIndexSchemaValueFieldTemplate> valueFieldTemplates = new ArrayList<>();

		ElasticsearchIndexSchemaNodeCollector collector = new ElasticsearchIndexSchemaNodeCollector() {
			@Override
			public void collect(String absolutePath, ElasticsearchIndexSchemaObjectFieldNode node) {
				objectFieldNodes.put( absolutePath, node );
			}

			@Override
			public void collect(String absoluteFieldPath, ElasticsearchIndexSchemaValueFieldNode<?> node) {
				valueFieldNodes.put( absoluteFieldPath, node );
			}

			@Override
			public void collect(ElasticsearchIndexSchemaObjectFieldTemplate template) {
				objectFieldTemplates.add( template );
			}

			@Override
			public void collect(ElasticsearchIndexSchemaValueFieldTemplate template) {
				valueFieldTemplates.add( template );
			}

			@Override
			public void collect(NamedDynamicTemplate templateForMapping) {
				mapping.addDynamicTemplate( templateForMapping );
			}
		};

		List<AbstractElasticsearchIndexSchemaFieldNode> staticChildren = new ArrayList<>();
		ElasticsearchIndexSchemaObjectNode rootNode = new ElasticsearchIndexSchemaRootNode( staticChildren );
		contributeChildren( mapping, rootNode, collector, staticChildren );

		return new ElasticsearchIndexModel(
				indexNames,
				mappedTypeName,
				analysisDefinitionRegistry,
				mapping,
				idDslConverter == null ? new StringToDocumentIdentifierValueConverter() : idDslConverter,
				rootNode, objectFieldNodes, valueFieldNodes,
				objectFieldTemplates, valueFieldTemplates
		);
	}

	@Override
	ElasticsearchIndexSchemaRootNodeBuilder getRootNodeBuilder() {
		return this;
	}

	@Override
	String getAbsolutePath() {
		return null;
	}

	EventContext getIndexEventContext() {
		return indexEventContext;
	}
}
