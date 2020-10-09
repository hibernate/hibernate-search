/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.hibernate.search.backend.elasticsearch.index.layout.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionRegistry;
import org.hibernate.search.backend.elasticsearch.document.model.lowlevel.impl.LowLevelIndexMetadataBuilder;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchIndexContext;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldFilter;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.backend.metamodel.IndexDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexFieldDescriptor;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.common.reporting.EventContext;


public class ElasticsearchIndexModel implements IndexDescriptor, ElasticsearchSearchIndexContext {

	private final IndexNames names;
	private final String mappedTypeName;
	private final EventContext eventContext;

	private final ElasticsearchAnalysisDefinitionRegistry analysisDefinitionRegistry;
	private final RootTypeMapping mapping;

	private final ToDocumentIdentifierValueConverter<?> idDslConverter;
	private final ElasticsearchIndexSchemaObjectNode rootNode;
	private final Map<String, ElasticsearchIndexSchemaObjectFieldNode> objectFieldNodes;
	private final Map<String, ElasticsearchIndexSchemaValueFieldNode<?>> valueFieldNodes;
	private final List<IndexFieldDescriptor> staticFields;
	private final List<ElasticsearchIndexSchemaObjectFieldTemplate> objectFieldTemplates;
	private final List<ElasticsearchIndexSchemaValueFieldTemplate> valueFieldTemplates;
	private final ConcurrentMap<String, ElasticsearchIndexSchemaObjectFieldNode> dynamicObjectFieldNodesCache = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, ElasticsearchIndexSchemaValueFieldNode<?>> dynamicValueFieldNodesCache = new ConcurrentHashMap<>();

	public ElasticsearchIndexModel(IndexNames names,
			String mappedTypeName,
			ElasticsearchAnalysisDefinitionRegistry analysisDefinitionRegistry,
			RootTypeMapping mapping, ToDocumentIdentifierValueConverter<?> idDslConverter,
			ElasticsearchIndexSchemaObjectNode rootNode,
			Map<String, ElasticsearchIndexSchemaObjectFieldNode> objectFieldNodes,
			Map<String, ElasticsearchIndexSchemaValueFieldNode<?>> valueFieldNodes,
			List<ElasticsearchIndexSchemaObjectFieldTemplate> objectFieldTemplates,
			List<ElasticsearchIndexSchemaValueFieldTemplate> valueFieldTemplates) {
		this.names = names;
		this.mappedTypeName = mappedTypeName;
		this.eventContext = EventContexts.fromIndexName( hibernateSearchName() );
		this.analysisDefinitionRegistry = analysisDefinitionRegistry;
		this.mapping = mapping;
		this.idDslConverter = idDslConverter;
		this.rootNode = rootNode;
		this.objectFieldNodes = objectFieldNodes;
		this.valueFieldNodes = valueFieldNodes;
		List<IndexFieldDescriptor> theStaticFields = new ArrayList<>();
		objectFieldNodes.values().stream()
				.filter( field -> IndexFieldInclusion.INCLUDED.equals( field.inclusion() ) )
				.forEach( theStaticFields::add );
		valueFieldNodes.values().stream()
				.filter( field -> IndexFieldInclusion.INCLUDED.equals( field.inclusion() ) )
				.forEach( theStaticFields::add );
		this.staticFields = CollectionHelper.toImmutableList( theStaticFields );
		this.objectFieldTemplates = objectFieldTemplates;
		this.valueFieldTemplates = valueFieldTemplates;
	}

	@Override
	public String hibernateSearchName() {
		return names.getHibernateSearch();
	}

	@Override
	public IndexNames names() {
		return names;
	}

	@Override
	public String mappedTypeName() {
		return mappedTypeName;
	}

	@Override
	public ToDocumentIdentifierValueConverter<?> idDslConverter() {
		return idDslConverter;
	}

	@Override
	public ElasticsearchIndexSchemaObjectNode root() {
		return rootNode;
	}

	@Override
	public Optional<IndexFieldDescriptor> field(String absolutePath) {
		return Optional.ofNullable( fieldOrNull( absolutePath ) );
	}

	public AbstractElasticsearchIndexSchemaFieldNode fieldOrNull(String absolutePath) {
		AbstractElasticsearchIndexSchemaFieldNode fieldDescriptor =
				getFieldNode( absolutePath, IndexFieldFilter.INCLUDED_ONLY );
		if ( fieldDescriptor == null ) {
			fieldDescriptor = getObjectFieldNode( absolutePath, IndexFieldFilter.INCLUDED_ONLY );
		}
		return fieldDescriptor;
	}

	@Override
	public Collection<IndexFieldDescriptor> staticFields() {
		return staticFields;
	}

	public EventContext getEventContext() {
		return eventContext;
	}

	public ElasticsearchIndexSchemaObjectFieldNode getObjectFieldNode(String absolutePath, IndexFieldFilter filter) {
		ElasticsearchIndexSchemaObjectFieldNode node =
				getNode( objectFieldNodes, objectFieldTemplates, dynamicObjectFieldNodesCache, absolutePath );
		return node == null ? null : filter.filter( node, node.inclusion() );
	}

	public ElasticsearchIndexSchemaValueFieldNode<?> getFieldNode(String absolutePath, IndexFieldFilter filter) {
		ElasticsearchIndexSchemaValueFieldNode<?> node =
				getNode( valueFieldNodes, valueFieldTemplates, dynamicValueFieldNodesCache, absolutePath );
		return node == null ? null : filter.filter( node, node.inclusion() );
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
			if ( node != null ) {
				N previous = dynamicNodesCache.putIfAbsent( absolutePath, node );
				if ( previous != null ) {
					// Some other thread created the node before us.
					// Keep the first created node, discard ours: they are identical.
					node = previous;
				}
				break;
			}
		}
		return node;
	}
}
