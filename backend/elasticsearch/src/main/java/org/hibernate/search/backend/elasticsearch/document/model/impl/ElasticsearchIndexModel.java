/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.index.settings.impl.ElasticsearchIndexSettingsBuilder;
import org.hibernate.search.backend.elasticsearch.index.settings.impl.esnative.IndexSettings;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.reporting.EventContext;


public class ElasticsearchIndexModel {

	private final String hibernateSearchIndexName;
	private final URLEncodedString elasticsearchIndexName;
	private final RootTypeMapping mapping;
	private final IndexSettings settings;

	private final ToDocumentIdentifierValueConverter<?> idDslConverter;
	private final Map<String, ElasticsearchIndexSchemaObjectNode> objectNodes;
	private final Map<String, ElasticsearchIndexSchemaFieldNode<?>> fieldNodes;

	public ElasticsearchIndexModel(String hibernateSearchIndexName,
			URLEncodedString elasticsearchIndexName,
			ElasticsearchIndexSettingsBuilder settingsBuilder,
			RootTypeMapping mapping, ToDocumentIdentifierValueConverter<?> idDslConverter,
			Map<String, ElasticsearchIndexSchemaObjectNode> objectNodes,
			Map<String, ElasticsearchIndexSchemaFieldNode<?>> fieldNodes) {
		this.hibernateSearchIndexName = hibernateSearchIndexName;
		this.elasticsearchIndexName = elasticsearchIndexName;
		this.settings = settingsBuilder.build();
		this.idDslConverter = idDslConverter;
		this.objectNodes = objectNodes;
		this.fieldNodes = fieldNodes;
		this.mapping = mapping;
	}

	public String getHibernateSearchIndexName() {
		return hibernateSearchIndexName;
	}

	public URLEncodedString getElasticsearchIndexName() {
		return elasticsearchIndexName;
	}

	public EventContext getEventContext() {
		return EventContexts.fromIndexName( hibernateSearchIndexName );
	}

	public RootTypeMapping getMapping() {
		return mapping;
	}

	public IndexSettings getSettings() {
		return settings;
	}

	public ToDocumentIdentifierValueConverter<?> getIdDslConverter() {
		return idDslConverter;
	}

	public ElasticsearchIndexSchemaObjectNode getObjectNode(String absolutePath) {
		return objectNodes.get( absolutePath );
	}

	public ElasticsearchIndexSchemaFieldNode<?> getFieldNode(String absoluteFieldPath) {
		return fieldNodes.get( absoluteFieldPath );
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "elasticsearchIndexName=" ).append( elasticsearchIndexName )
				.append( ", mapping=" ).append( mapping )
				.append( "]" )
				.toString();
	}

}
