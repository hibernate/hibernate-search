/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.schema.impl;

import java.util.Map;

import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.schema.impl.model.IndexMetadata;
import org.hibernate.search.elasticsearch.schema.impl.model.TypeMapping;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * The default {@link ElasticsearchSchemaCreator} implementation.
 * @author Gunnar Morling
 * @author Yoann Rodiere
 */
public class DefaultElasticsearchSchemaCreator implements ElasticsearchSchemaCreator {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private final ElasticsearchSchemaAccessor schemaAccessor;

	public DefaultElasticsearchSchemaCreator(ElasticsearchSchemaAccessor schemaAccessor) {
		super();
		this.schemaAccessor = schemaAccessor;
	}

	@Override
	public void createIndex(IndexMetadata indexMetadata, ExecutionOptions executionOptions) {
		URLEncodedString indexName = indexMetadata.getName();

		schemaAccessor.createIndex( indexName, indexMetadata.getSettings(), executionOptions );

		schemaAccessor.waitForIndexStatus( indexName, executionOptions );
	}

	@Override
	public boolean createIndexIfAbsent(IndexMetadata indexMetadata, ExecutionOptions executionOptions) {
		URLEncodedString indexName = indexMetadata.getName();

		boolean created = false;

		if ( !schemaAccessor.indexExists( indexName ) ) {
			created = schemaAccessor.createIndexIfAbsent( indexName, indexMetadata.getSettings(), executionOptions );
		}

		schemaAccessor.waitForIndexStatus( indexName, executionOptions );

		return created;
	}

	@Override
	public void checkIndexExists(URLEncodedString indexName, ExecutionOptions executionOptions) {
		if ( schemaAccessor.indexExists( indexName ) ) {
			schemaAccessor.waitForIndexStatus( indexName, executionOptions );
		}
		else {
			throw LOG.indexMissing( indexName );
		}
	}

	@Override
	public void createMappings(IndexMetadata indexMetadata, ExecutionOptions executionOptions) {
		URLEncodedString indexName = indexMetadata.getName();

		for ( Map.Entry<String, TypeMapping> entry : indexMetadata.getMappings().entrySet() ) {
			URLEncodedString mappingName = URLEncodedString.fromString( entry.getKey() );
			TypeMapping mapping = entry.getValue();
			schemaAccessor.putMapping( indexName, mappingName, mapping );
		}
	}

}
