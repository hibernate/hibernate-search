/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.admin.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.index.settings.impl.esnative.IndexSettings;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * The default {@link ElasticsearchSchemaMigrator} implementation.
 * @author Gunnar Morling
 * @author Yoann Rodiere
 */
public class ElasticsearchSchemaMigratorImpl implements ElasticsearchSchemaMigrator {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchSchemaAccessor schemaAccessor;
	private final ElasticsearchSchemaValidator schemaValidator;

	public ElasticsearchSchemaMigratorImpl(ElasticsearchSchemaAccessor schemaAccessor,
			ElasticsearchSchemaValidator schemaValidator) {
		super();
		this.schemaAccessor = schemaAccessor;
		this.schemaValidator = schemaValidator;
	}

	@Override
	public void migrate(IndexMetadata indexMetadata, ExecutionOptions executionOptions) {
		URLEncodedString indexName = indexMetadata.getName();
		URLEncodedString typeName = indexMetadata.getTypeName();
		IndexSettings settings = indexMetadata.getSettings();

		try {
			/*
			 * We only update settings if it's really necessary, because closing the index,
			 * even for just a moment, may hurt if other clients are using the index.
			 */
			if ( !settings.isEmpty() && !schemaValidator.isSettingsValid( indexMetadata, executionOptions ) ) {
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

			// Elasticsearch itself takes care of the actual merging
			schemaAccessor.putMapping( indexName, typeName, indexMetadata.getMapping() );
		}
		catch (SearchException e) {
			throw log.schemaUpdateFailed( indexName, e );
		}
	}

}
