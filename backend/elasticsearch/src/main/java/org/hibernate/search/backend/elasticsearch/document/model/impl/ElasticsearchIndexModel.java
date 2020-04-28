/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.hibernate.search.backend.elasticsearch.index.layout.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionRegistry;
import org.hibernate.search.backend.elasticsearch.document.model.lowlevel.impl.LowLevelIndexMetadataBuilder;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.reporting.EventContext;


public class ElasticsearchIndexModel {

	private final IndexNames names;
	private final String mappedTypeName;
	private final EventContext eventContext;

	private final ElasticsearchAnalysisDefinitionRegistry analysisDefinitionRegistry;
	private final RootTypeMapping mapping;

	private final ToDocumentIdentifierValueConverter<?> idDslConverter;
	private final ElasticsearchIndexSchemaObjectNode rootNode;
	private final Map<String, ElasticsearchIndexSchemaObjectNode> objectNodes;
	private final Map<String, ElasticsearchIndexSchemaFieldNode<?>> fieldNodes;
	private final List<ElasticsearchIndexSchemaObjectFieldTemplate> objectFieldTemplates;
	private final List<ElasticsearchIndexSchemaFieldTemplate> fieldTemplates;
	private final ConcurrentMap<String, ElasticsearchIndexSchemaObjectNode> dynamicObjectNodesCache = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, ElasticsearchIndexSchemaFieldNode<?>> dynamicFieldNodesCache = new ConcurrentHashMap<>();

	public ElasticsearchIndexModel(IndexNames names,
			String mappedTypeName,
			ElasticsearchAnalysisDefinitionRegistry analysisDefinitionRegistry,
			RootTypeMapping mapping, ToDocumentIdentifierValueConverter<?> idDslConverter,
			ElasticsearchIndexSchemaObjectNode rootNode,
			Map<String, ElasticsearchIndexSchemaObjectNode> objectNodes,
			Map<String, ElasticsearchIndexSchemaFieldNode<?>> fieldNodes,
			List<ElasticsearchIndexSchemaObjectFieldTemplate> objectFieldTemplates,
			List<ElasticsearchIndexSchemaFieldTemplate> fieldTemplates) {
		this.names = names;
		this.mappedTypeName = mappedTypeName;
		this.eventContext = EventContexts.fromIndexName( getHibernateSearchIndexName() );
		this.analysisDefinitionRegistry = analysisDefinitionRegistry;
		this.mapping = mapping;
		this.idDslConverter = idDslConverter;
		this.rootNode = rootNode;
		this.objectNodes = objectNodes;
		this.fieldNodes = fieldNodes;
		this.objectFieldTemplates = objectFieldTemplates;
		this.fieldTemplates = fieldTemplates;
	}

	public String getHibernateSearchIndexName() {
		return names.getHibernateSearch();
	}

	public IndexNames getNames() {
		return names;
	}

	public String getMappedTypeName() {
		return mappedTypeName;
	}

	public EventContext getEventContext() {
		return eventContext;
	}

	public ToDocumentIdentifierValueConverter<?> getIdDslConverter() {
		return idDslConverter;
	}

	ElasticsearchIndexSchemaObjectNode getRootNode() {
		return rootNode;
	}

	public ElasticsearchIndexSchemaObjectNode getObjectNode(String absolutePath) {
		return getNode( objectNodes, objectFieldTemplates, dynamicObjectNodesCache, absolutePath );
	}

	public ElasticsearchIndexSchemaFieldNode<?> getFieldNode(String absolutePath) {
		return getNode( fieldNodes, fieldTemplates, dynamicFieldNodesCache, absolutePath );
	}

	public void contributeLowLevelMetadata(LowLevelIndexMetadataBuilder builder) {
		builder.setAnalysisDefinitionRegistry( analysisDefinitionRegistry );
		builder.setMapping( mapping );
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "names=" ).append( names )
				.append( ", mapping=" ).append( mapping )
				.append( "]" )
				.toString();
	}

	private <N> N getNode(Map<String, N> staticNodes,
			List<? extends AbstractElasticsearchIndexSchemaFieldTemplate<N>> templates,
			ConcurrentMap<String, N> dynamicNodesCache,
			String absolutePath) {
		N node = staticNodes.get( absolutePath );
		if ( node != null ) {
			return node;
		}
		node = dynamicNodesCache.get( absolutePath );
		if ( node != null ) {
			return node;
		}
		for ( AbstractElasticsearchIndexSchemaFieldTemplate<N> template : templates ) {
			node = template.createNodeIfMatching( this, absolutePath );
			if ( node == null ) {
				continue;
			}
			N previous = dynamicNodesCache.putIfAbsent( absolutePath, node );
			if ( previous != null ) {
				// Some other thread created the node before us.
				// Keep the first created node, discard ours: they are identical.
				node = previous;
			}
			break;
		}
		return node;
	}
}
