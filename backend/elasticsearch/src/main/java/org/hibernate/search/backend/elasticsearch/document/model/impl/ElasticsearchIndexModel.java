/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.util.impl.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.RootTypeMapping;

/**
 * @author Yoann Rodiere
 */
public class ElasticsearchIndexModel {

	private final URLEncodedString indexName;
	private final RootTypeMapping mapping;
	private final Map<String, ElasticsearchIndexSchemaObjectNode> objectNodes = new HashMap<>();
	private final Map<String, ElasticsearchIndexSchemaFieldNode> fieldNodes = new HashMap<>();

	public ElasticsearchIndexModel(URLEncodedString indexName, ElasticsearchRootIndexSchemaContributor contributor) {
		this.indexName = indexName;
		this.mapping = contributor.contribute( new ElasticsearchIndexSchemaNodeCollector() {
			@Override
			public void collect(String absolutePath, ElasticsearchIndexSchemaObjectNode node) {
				objectNodes.put( absolutePath, node );
			}

			@Override
			public void collect(String absoluteFieldPath, ElasticsearchIndexSchemaFieldNode node) {
				fieldNodes.put( absoluteFieldPath, node );
			}
		} );
	}

	public URLEncodedString getIndexName() {
		return indexName;
	}

	public RootTypeMapping getMapping() {
		return mapping;
	}

	public ElasticsearchIndexSchemaObjectNode getObjectNode(String absolutePath) {
		return objectNodes.get( absolutePath );
	}

	public ElasticsearchIndexSchemaFieldNode getFieldNode(String absoluteFieldPath) {
		return fieldNodes.get( absoluteFieldPath );
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "indexName=" ).append( indexName )
				.append( ", mapping=" ).append( mapping )
				.append( "]" )
				.toString();
	}

}
