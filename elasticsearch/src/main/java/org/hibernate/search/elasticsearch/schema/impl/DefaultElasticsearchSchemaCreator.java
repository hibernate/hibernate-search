/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.schema.impl;

import java.util.Map;
import java.util.Properties;

import org.hibernate.search.elasticsearch.schema.impl.model.IndexMetadata;
import org.hibernate.search.elasticsearch.schema.impl.model.TypeMapping;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.engine.service.spi.Stoppable;
import org.hibernate.search.spi.BuildContext;

/**
 * The default {@link ElasticsearchSchemaCreator} implementation.
 * @author Gunnar Morling
 * @author Yoann Rodiere
 */
public class DefaultElasticsearchSchemaCreator implements ElasticsearchSchemaCreator, Startable, Stoppable {

	private ServiceManager serviceManager;
	private ElasticsearchSchemaAccessor schemaAccessor;

	@Override
	public void start(Properties properties, BuildContext context) {
		serviceManager = context.getServiceManager();
		schemaAccessor = serviceManager.requestService( ElasticsearchSchemaAccessor.class );
	}

	@Override
	public void stop() {
		schemaAccessor = null;
		serviceManager.releaseService( ElasticsearchSchemaAccessor.class );
		serviceManager = null;
	}

	@Override
	public void create(IndexMetadata indexMetadata, ExecutionOptions executionOptions) {
		String indexName = indexMetadata.getName();

		schemaAccessor.createIndex( indexName, executionOptions );

		schemaAccessor.waitForIndexStatus( indexName, executionOptions );

		for ( Map.Entry<String, TypeMapping> entry : indexMetadata.getMappings().entrySet() ) {
			String mappingName = entry.getKey();
			TypeMapping mapping = entry.getValue();
			schemaAccessor.putMapping( indexName, mappingName, mapping );
		}
	}

	@Override
	public void createIfAbsent(IndexMetadata indexMetadata, ExecutionOptions executionOptions) {
		String indexName = indexMetadata.getName();

		boolean created = false;

		if ( !schemaAccessor.indexExists( indexName ) ) {
			created = schemaAccessor.createIndexIfAbsent( indexName, executionOptions );
		}

		if ( created ) {
			schemaAccessor.waitForIndexStatus( indexName, executionOptions );

			for ( Map.Entry<String, TypeMapping> entry : indexMetadata.getMappings().entrySet() ) {
				String mappingName = entry.getKey();
				TypeMapping mapping = entry.getValue();
				schemaAccessor.putMapping( indexName, mappingName, mapping );
			}
		}
	}

}
