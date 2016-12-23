/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.schema.impl;

import java.util.Map;
import java.util.Properties;

import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.schema.impl.model.IndexMetadata;
import org.hibernate.search.elasticsearch.schema.impl.model.TypeMapping;
import org.hibernate.search.elasticsearch.settings.impl.model.IndexSettings;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.engine.service.spi.Stoppable;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * The default {@link ElasticsearchSchemaMigrator} implementation.
 * @author Gunnar Morling
 * @author Yoann Rodiere
 */
public class DefaultElasticsearchSchemaMigrator implements ElasticsearchSchemaMigrator, Startable, Stoppable {

	private static final Log LOG = LoggerFactory.make( Log.class );

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
	public void merge(IndexMetadata indexMetadata, ExecutionOptions executionOptions) {
		String indexName = indexMetadata.getName();
		IndexSettings settings = indexMetadata.getSettings();

		try {
			if ( !settings.isEmpty() ) {
				schemaAccessor.closeIndex( indexName );
				try {
					schemaAccessor.updateSettings( indexName, settings );
				}
				catch (RuntimeException mainException) {
					// Try not to leave the index closed if something failed
					try {
						schemaAccessor.openIndex( indexName );
					}
					catch (RuntimeException e) {
						mainException.addSuppressed( e );
					}
					throw mainException;
				}
				// Re-open the index after the settings have been successfully updated
				schemaAccessor.openIndex( indexName );
			}

			for ( Map.Entry<String, TypeMapping> entry : indexMetadata.getMappings().entrySet() ) {
				String mappingName = entry.getKey();
				TypeMapping mapping = entry.getValue();

				// Elasticsearch itself takes care of the actual merging
				schemaAccessor.putMapping( indexName, mappingName, mapping );
			}
		}
		catch (SearchException e) {
			throw LOG.schemaMergeFailed( indexName, e );
		}
	}


}
