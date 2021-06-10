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
import java.util.TreeMap;

import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionRegistry;
import org.hibernate.search.backend.elasticsearch.document.model.impl.AbstractElasticsearchIndexField;
import org.hibernate.search.backend.elasticsearch.document.model.impl.AbstractElasticsearchIndexSchemaFieldTemplate;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexValueField;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaValueFieldTemplate;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexObjectField;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectFieldTemplate;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexRoot;
import org.hibernate.search.backend.elasticsearch.index.DynamicMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DynamicType;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.NamedDynamicTemplate;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RoutingType;
import org.hibernate.search.backend.elasticsearch.index.layout.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.IndexSettings;
import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchIndexFieldTypeFactory;
import org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl.ElasticsearchIndexFieldTypeFactoryProvider;
import org.hibernate.search.engine.backend.types.converter.spi.DocumentIdentifierValueConverter;
import org.hibernate.search.engine.backend.types.converter.spi.StringDocumentIdentifierValueConverter;
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
	private final IndexSettings customIndexSettings;
	private final DynamicType defaultDynamicType;

	private RoutingType routing = null;
	private DocumentIdentifierValueConverter<?> idDslConverter;

	public ElasticsearchIndexSchemaRootNodeBuilder(ElasticsearchIndexFieldTypeFactoryProvider typeFactoryProvider,
			EventContext indexEventContext,
			IndexNames indexNames, String mappedTypeName,
			ElasticsearchAnalysisDefinitionRegistry analysisDefinitionRegistry, IndexSettings customIndexSettings,
			DynamicMapping dynamicMapping) {
		this.typeFactoryProvider = typeFactoryProvider;
		this.indexEventContext = indexEventContext;
		this.indexNames = indexNames;
		this.mappedTypeName = mappedTypeName;
		this.analysisDefinitionRegistry = analysisDefinitionRegistry;
		this.customIndexSettings = customIndexSettings;
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
	public void idDslConverter(DocumentIdentifierValueConverter<?> idDslConverter) {
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

		Map<String, AbstractElasticsearchIndexField> staticFields = new HashMap<>();
		List<AbstractElasticsearchIndexSchemaFieldTemplate<?>> fieldTemplates = new ArrayList<>();

		ElasticsearchIndexSchemaNodeCollector collector = new ElasticsearchIndexSchemaNodeCollector() {
			@Override
			public void collect(String absolutePath, ElasticsearchIndexObjectField node) {
				staticFields.put( absolutePath, node );
			}

			@Override
			public void collect(String absoluteFieldPath, ElasticsearchIndexValueField<?> node) {
				staticFields.put( absoluteFieldPath, node );
			}

			@Override
			public void collect(ElasticsearchIndexSchemaObjectFieldTemplate template) {
				fieldTemplates.add( template );
			}

			@Override
			public void collect(ElasticsearchIndexSchemaValueFieldTemplate template) {
				fieldTemplates.add( template );
			}

			@Override
			public void collect(NamedDynamicTemplate templateForMapping) {
				mapping.addDynamicTemplate( templateForMapping );
			}
		};

		Map<String, AbstractElasticsearchIndexField> staticChildrenByName = new TreeMap<>();
		ElasticsearchIndexRoot rootNode = new ElasticsearchIndexRoot( staticChildrenByName,
				buildQueryElementFactoryMap() );
		contributeChildren( mapping, rootNode, collector, staticChildrenByName );

		return new ElasticsearchIndexModel(
				indexNames,
				mappedTypeName,
				analysisDefinitionRegistry, customIndexSettings,
				mapping,
				idDslConverter == null ? new StringDocumentIdentifierValueConverter() : idDslConverter,
				rootNode, staticFields, fieldTemplates
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
