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
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldFilter;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.backend.metamodel.IndexCompositeElementDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexFieldDescriptor;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.common.reporting.EventContext;


public class ElasticsearchIndexModel implements IndexDescriptor {

	private final IndexNames names;
	private final String mappedTypeName;
	private final EventContext eventContext;

	private final ElasticsearchAnalysisDefinitionRegistry analysisDefinitionRegistry;
	private final RootTypeMapping mapping;

	private final ToDocumentIdentifierValueConverter<?> idDslConverter;
	private final ElasticsearchIndexSchemaObjectNode rootNode;
	private final Map<String, ElasticsearchIndexSchemaObjectFieldNode> objectFieldNodes;
	private final Map<String, ElasticsearchIndexSchemaFieldNode<?>> fieldNodes;
	private final List<IndexFieldDescriptor> staticFields;
	private final List<ElasticsearchIndexSchemaObjectFieldTemplate> objectFieldTemplates;
	private final List<ElasticsearchIndexSchemaFieldTemplate> fieldTemplates;
	private final ConcurrentMap<String, ElasticsearchIndexSchemaObjectFieldNode> dynamicObjectFieldNodesCache = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, ElasticsearchIndexSchemaFieldNode<?>> dynamicFieldNodesCache = new ConcurrentHashMap<>();

	public ElasticsearchIndexModel(IndexNames names,
			String mappedTypeName,
			ElasticsearchAnalysisDefinitionRegistry analysisDefinitionRegistry,
			RootTypeMapping mapping, ToDocumentIdentifierValueConverter<?> idDslConverter,
			ElasticsearchIndexSchemaObjectNode rootNode,
			Map<String, ElasticsearchIndexSchemaObjectFieldNode> objectFieldNodes,
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
		this.objectFieldNodes = objectFieldNodes;
		this.fieldNodes = fieldNodes;
		List<IndexFieldDescriptor> theStaticFields = new ArrayList<>();
		objectFieldNodes.values().stream()
				.filter( field -> IndexFieldInclusion.INCLUDED.equals( field.getInclusion() ) )
				.forEach( theStaticFields::add );
		fieldNodes.values().stream()
				.filter( field -> IndexFieldInclusion.INCLUDED.equals( field.getInclusion() ) )
				.forEach( theStaticFields::add );
		this.staticFields = CollectionHelper.toImmutableList( theStaticFields );
		this.objectFieldTemplates = objectFieldTemplates;
		this.fieldTemplates = fieldTemplates;
	}

	@Override
	public String hibernateSearchName() {
		return names.getHibernateSearch();
	}

	public String getHibernateSearchIndexName() {
		return names.getHibernateSearch();
	}

	@Override
	public IndexCompositeElementDescriptor root() {
		return rootNode;
	}

	@Override
	public Optional<IndexFieldDescriptor> field(String absolutePath) {
		IndexFieldDescriptor fieldDescriptor = getFieldNode( absolutePath, IndexFieldFilter.INCLUDED_ONLY );
		if ( fieldDescriptor == null ) {
			fieldDescriptor = getObjectFieldNode( absolutePath, IndexFieldFilter.INCLUDED_ONLY );
		}
		return Optional.ofNullable( fieldDescriptor );
	}

	@Override
	public Collection<IndexFieldDescriptor> staticFields() {
		return staticFields;
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

	public ElasticsearchIndexSchemaObjectNode getRootNode() {
		return rootNode;
	}

	public ElasticsearchIndexSchemaObjectFieldNode getObjectFieldNode(String absolutePath, IndexFieldFilter filter) {
		ElasticsearchIndexSchemaObjectFieldNode node =
				getNode( objectFieldNodes, objectFieldTemplates, dynamicObjectFieldNodesCache, absolutePath );
		return node == null ? null : filter.filter( node, node.getInclusion() );
	}

	public ElasticsearchIndexSchemaFieldNode<?> getFieldNode(String absolutePath, IndexFieldFilter filter) {
		ElasticsearchIndexSchemaFieldNode<?> node =
				getNode( fieldNodes, fieldTemplates, dynamicFieldNodesCache, absolutePath );
		return node == null ? null : filter.filter( node, node.getInclusion() );
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
