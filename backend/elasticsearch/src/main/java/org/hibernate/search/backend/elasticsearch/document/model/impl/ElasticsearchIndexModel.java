/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.TypeMapping;

/**
 * @author Yoann Rodiere
 */
public class ElasticsearchIndexModel {

	private final String indexName;
	private final TypeMapping mapping;
	private final Map<String, ElasticsearchObjectNodeModel> objectFieldModels = new HashMap<>();
	private final Map<String, ElasticsearchFieldModel> fieldModels = new HashMap<>();

	public ElasticsearchIndexModel(String indexName, ElasticsearchRootIndexSchemaCollectorImpl collector) {
		this.indexName = indexName;
		this.mapping = collector.contribute( new ElasticsearchIndexSchemaNodeCollector() {
			@Override
			public void collect(String absolutePath, ElasticsearchObjectNodeModel model) {
				objectFieldModels.put( absolutePath, model );
			}

			@Override
			public void collect(String absolutePath, ElasticsearchFieldModel model) {
				fieldModels.put( absolutePath, model );
			}
		} );
	}

	public String getIndexName() {
		return indexName;
	}

	public TypeMapping getMapping() {
		return mapping;
	}

	public ElasticsearchObjectNodeModel getObjectNodeModel(String absolutePath) {
		return objectFieldModels.get( absolutePath );
	}

	public ElasticsearchFieldModel getFieldModel(String absolutePath) {
		return fieldModels.get( absolutePath );
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
