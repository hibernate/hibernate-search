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

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaNodeCollector;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaObjectNode;
import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.DynamicType;
import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.RoutingType;
import org.hibernate.search.backend.elasticsearch.index.settings.impl.ElasticsearchIndexSettingsBuilder;
import org.hibernate.search.backend.elasticsearch.types.dsl.ElasticsearchIndexFieldTypeFactory;
import org.hibernate.search.backend.elasticsearch.types.dsl.provider.impl.ElasticsearchIndexFieldTypeFactoryProvider;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
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

	private final String mappedTypeName;

	private RoutingType routing = null;
	private ToDocumentIdentifierValueConverter<?> idDslConverter;

	public ElasticsearchIndexSchemaRootNodeBuilder(ElasticsearchIndexFieldTypeFactoryProvider typeFactoryProvider,
			EventContext indexEventContext,
			String mappedTypeName) {
		this.typeFactoryProvider = typeFactoryProvider;
		this.indexEventContext = indexEventContext;
		this.mappedTypeName = mappedTypeName;
	}

	@Override
	public EventContext getEventContext() {
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

	public ElasticsearchIndexModel build(String hibernateSearchIndexName, URLEncodedString elasticsearchIndexName,
			ElasticsearchIndexSettingsBuilder settingsBuilder) {
		RootTypeMapping mapping = new RootTypeMapping();
		if ( routing != null ) {
			mapping.setRouting( routing );
		}

		for ( IndexSchemaRootContributor schemaRootContributor : schemaRootContributors ) {
			schemaRootContributor.contribute( mapping );
		}

		// TODO HSEARCH-3273 allow to configure this, both at index level (configuration properties) and at field level (ElasticsearchExtension)
		mapping.setDynamic( DynamicType.STRICT );

		final Map<String, ElasticsearchIndexSchemaObjectNode> objectNodes = new HashMap<>();
		final Map<String, ElasticsearchIndexSchemaFieldNode<?>> fieldNodes = new HashMap<>();

		ElasticsearchIndexSchemaNodeCollector collector = new ElasticsearchIndexSchemaNodeCollector() {
			@Override
			public void collect(String absolutePath, ElasticsearchIndexSchemaObjectNode node) {
				objectNodes.put( absolutePath, node );
			}

			@Override
			public void collect(String absoluteFieldPath, ElasticsearchIndexSchemaFieldNode<?> node) {
				fieldNodes.put( absoluteFieldPath, node );
			}
		};

		ElasticsearchIndexSchemaObjectNode rootNode = ElasticsearchIndexSchemaObjectNode.root();
		contributeChildren( mapping, rootNode, collector );

		return new ElasticsearchIndexModel(
				hibernateSearchIndexName,
				elasticsearchIndexName,
				mappedTypeName,
				settingsBuilder,
				mapping,
				idDslConverter == null ? new StringToDocumentIdentifierValueConverter() : idDslConverter,
				objectNodes,
				fieldNodes
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
