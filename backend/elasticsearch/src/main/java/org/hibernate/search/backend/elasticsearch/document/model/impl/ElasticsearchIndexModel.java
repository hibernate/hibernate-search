/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.analysis.model.impl.ElasticsearchAnalysisDefinitionRegistry;
import org.hibernate.search.backend.elasticsearch.metamodel.ElasticsearchIndexDescriptor;
import org.hibernate.search.backend.elasticsearch.document.model.lowlevel.impl.LowLevelIndexMetadataBuilder;
import org.hibernate.search.backend.elasticsearch.index.layout.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.IndexSettings;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexContext;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldFilter;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.backend.metamodel.IndexFieldDescriptor;
import org.hibernate.search.engine.backend.types.converter.spi.DocumentIdentifierValueConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.common.reporting.EventContext;

public class ElasticsearchIndexModel implements ElasticsearchIndexDescriptor, ElasticsearchSearchIndexContext {

	private final IndexNames names;
	private final String mappedTypeName;
	private final EventContext eventContext;

	private final ElasticsearchAnalysisDefinitionRegistry analysisDefinitionRegistry;
	private final IndexSettings customIndexSettings;
	private final RootTypeMapping mapping;

	private final DocumentIdentifierValueConverter<?> idDslConverter;
	private final ElasticsearchIndexRoot rootNode;
	private final Map<String, AbstractElasticsearchIndexField> staticFields;
	private final List<IndexFieldDescriptor> includedStaticFields;
	private final List<AbstractElasticsearchIndexSchemaFieldTemplate<?>> fieldTemplates;
	private final ConcurrentMap<String, AbstractElasticsearchIndexField> dynamicFieldsCache = new ConcurrentHashMap<>();

	public ElasticsearchIndexModel(IndexNames names,
			String mappedTypeName,
			ElasticsearchAnalysisDefinitionRegistry analysisDefinitionRegistry, IndexSettings customIndexSettings,
			RootTypeMapping mapping, DocumentIdentifierValueConverter<?> idDslConverter,
			ElasticsearchIndexRoot rootNode,
			Map<String, AbstractElasticsearchIndexField> staticFields,
			List<AbstractElasticsearchIndexSchemaFieldTemplate<?>> fieldTemplates) {
		this.names = names;
		this.mappedTypeName = mappedTypeName;
		this.eventContext = EventContexts.fromIndexName( hibernateSearchName() );
		this.analysisDefinitionRegistry = analysisDefinitionRegistry;
		this.customIndexSettings = customIndexSettings;
		this.mapping = mapping;
		this.idDslConverter = idDslConverter;
		this.rootNode = rootNode;
		this.staticFields = staticFields;
		this.includedStaticFields = CollectionHelper.toImmutableList( staticFields.values().stream()
				.filter( field -> IndexFieldInclusion.INCLUDED.equals( field.inclusion() ) )
				.collect( Collectors.toList() ) );
		this.fieldTemplates = fieldTemplates;
	}

	@Override
	public String hibernateSearchName() {
		return names.hibernateSearchIndex();
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
	public DocumentIdentifierValueConverter<?> idDslConverter() {
		return idDslConverter;
	}

	@Override
	public int maxResultWindow() {
		return ( customIndexSettings == null || customIndexSettings.getMaxResultWindow() == null ) ?
				IndexSettings.MAX_RESULT_WINDOW_DEFAULT :
				customIndexSettings.getMaxResultWindow();
	}

	@Override
	public ElasticsearchIndexRoot root() {
		return rootNode;
	}

	@Override
	public Optional<IndexFieldDescriptor> field(String absolutePath) {
		return Optional.ofNullable( fieldOrNull( absolutePath ) );
	}

	public AbstractElasticsearchIndexField fieldOrNull(String absolutePath) {
		return fieldOrNull( absolutePath, IndexFieldFilter.INCLUDED_ONLY );
	}

	public AbstractElasticsearchIndexField fieldOrNull(String absolutePath, IndexFieldFilter filter) {
		AbstractElasticsearchIndexField node = fieldOrNullIgnoringInclusion( absolutePath );
		return node == null ? null : filter.filter( node, node.inclusion() );
	}

	@Override
	public Collection<IndexFieldDescriptor> staticFields() {
		return includedStaticFields;
	}

	public EventContext getEventContext() {
		return eventContext;
	}

	public void contributeLowLevelMetadata(LowLevelIndexMetadataBuilder builder) {
		builder.setAnalysisDefinitionRegistry( analysisDefinitionRegistry );
		builder.setCustomIndexSettings( customIndexSettings );
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

	private AbstractElasticsearchIndexField fieldOrNullIgnoringInclusion(String absolutePath) {
		AbstractElasticsearchIndexField field = staticFields.get( absolutePath );
		if ( field != null ) {
			return field;
		}
		field = dynamicFieldsCache.get( absolutePath );
		if ( field != null ) {
			return field;
		}
		for ( AbstractElasticsearchIndexSchemaFieldTemplate<?> template : fieldTemplates ) {
			field = template.createNodeIfMatching( this, absolutePath );
			if ( field != null ) {
				AbstractElasticsearchIndexField previous = dynamicFieldsCache.putIfAbsent( absolutePath, field );
				if ( previous != null ) {
					// Some other thread created the node before us.
					// Keep the first created node, discard ours: they are identical.
					field = previous;
				}
				break;
			}
		}
		return field;
	}

	@Override
	public String readName() {
		return names.read().toString();
	}

	@Override
	public String writeName() {
		return names.write().toString();
	}
}
