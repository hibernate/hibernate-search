/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
public class StubElasticsearchWorkFactory implements ElasticsearchWorkFactory {

	@Override
	public ElasticsearchWork<?> createIndex(String indexName, JsonObject model) {
		return new StubElasticsearchWork<>( "createIndex", model )
				.addParam( "indexName", indexName );
	}

	@Override
	public ElasticsearchWork<?> add(String indexName, String id, JsonObject document) {
		return new StubElasticsearchWork<>( "add", document )
				.addParam( "indexName", indexName )
				.addParam( "id", id );
	}

	@Override
	public ElasticsearchWork<?> update(String indexName, String id, JsonObject document) {
		return new StubElasticsearchWork<>( "update", document )
				.addParam( "indexName", indexName )
				.addParam( "id", id );
	}

	@Override
	public ElasticsearchWork<?> delete(String indexName, String id) {
		return new StubElasticsearchWork<>( "delete", null )
				.addParam( "indexName", indexName )
				.addParam( "id", id );
	}

	@Override
	public ElasticsearchWork<?> flush(String indexName) {
		return new StubElasticsearchWork<>( "flush", null )
				.addParam( "indexName", indexName );
	}

	@Override
	public ElasticsearchWork<?> optimize(String indexName) {
		return new StubElasticsearchWork<>( "optimize", null )
				.addParam( "indexName", indexName );
	}

}
