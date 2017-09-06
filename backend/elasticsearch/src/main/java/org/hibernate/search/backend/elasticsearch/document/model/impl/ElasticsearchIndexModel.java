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
	private final Map<String, ElasticsearchFieldModel> fieldModels = new HashMap<>();

	public ElasticsearchIndexModel(String indexName, ElasticsearchIndexModelCollectorImpl<TypeMapping> collector) {
		this.indexName = indexName;
		this.mapping = new TypeMapping();
		collector.contribute( mapping, fieldModels::put );
	}

	public String getIndexName() {
		return indexName;
	}

	public TypeMapping getMapping() {
		return mapping;
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
