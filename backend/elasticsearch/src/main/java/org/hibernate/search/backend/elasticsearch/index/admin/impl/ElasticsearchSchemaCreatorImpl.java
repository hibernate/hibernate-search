/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.admin.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * The default {@link ElasticsearchSchemaCreator} implementation.
 * @author Gunnar Morling
 */
public class ElasticsearchSchemaCreatorImpl implements ElasticsearchSchemaCreator {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchSchemaAccessor schemaAccessor;

	public ElasticsearchSchemaCreatorImpl(ElasticsearchSchemaAccessor schemaAccessor) {
		super();
		this.schemaAccessor = schemaAccessor;
	}

	@Override
	public void createIndex(IndexMetadata indexMetadata, ElasticsearchIndexLifecycleExecutionOptions executionOptions) {
		URLEncodedString indexName = indexMetadata.getName();

		schemaAccessor.createIndex(
				indexName, indexMetadata.getSettings(),
				indexMetadata.getMapping()
		);

		schemaAccessor.waitForIndexStatus( indexName, executionOptions );
	}

	@Override
	public boolean createIndexIfAbsent(IndexMetadata indexMetadata, ElasticsearchIndexLifecycleExecutionOptions executionOptions) {
		URLEncodedString indexName = indexMetadata.getName();

		boolean created = false;

		if ( !schemaAccessor.indexExists( indexName ) ) {
			created = schemaAccessor.createIndexIfAbsent(
					indexName, indexMetadata.getSettings(),
					indexMetadata.getMapping()
			);
		}

		schemaAccessor.waitForIndexStatus( indexName, executionOptions );

		return created;
	}

	@Override
	public void checkIndexExists(URLEncodedString indexName, ElasticsearchIndexLifecycleExecutionOptions executionOptions) {
		if ( schemaAccessor.indexExists( indexName ) ) {
			schemaAccessor.waitForIndexStatus( indexName, executionOptions );
		}
		else {
			throw log.indexMissing( indexName );
		}
	}

}
