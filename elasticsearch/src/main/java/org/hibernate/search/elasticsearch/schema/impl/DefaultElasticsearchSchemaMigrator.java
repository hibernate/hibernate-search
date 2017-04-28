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
import org.hibernate.search.elasticsearch.settings.impl.model.IndexSettings;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * The default {@link ElasticsearchSchemaMigrator} implementation.
 * @author Gunnar Morling
 * @author Yoann Rodiere
 */
public class DefaultElasticsearchSchemaMigrator implements ElasticsearchSchemaMigrator {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private final ElasticsearchSchemaAccessor schemaAccessor;
	private final ElasticsearchSchemaValidator schemaValidator;

	public DefaultElasticsearchSchemaMigrator(ElasticsearchSchemaAccessor schemaAccessor,
			ElasticsearchSchemaValidator schemaValidator) {
		super();
		this.schemaAccessor = schemaAccessor;
		this.schemaValidator = schemaValidator;
	}

	@Override
	public void migrate(IndexMetadata indexMetadata, ExecutionOptions executionOptions) {
		URLEncodedString indexName = indexMetadata.getName();
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

			for ( Map.Entry<String, TypeMapping> entry : indexMetadata.getMappings().entrySet() ) {
				URLEncodedString mappingName = URLEncodedString.fromString( entry.getKey() );
				TypeMapping mapping = entry.getValue();

				// Elasticsearch itself takes care of the actual merging
				schemaAccessor.putMapping( indexName, mappingName, mapping );
			}
		}
		catch (SearchException e) {
			throw LOG.schemaUpdateFailed( indexName, e );
		}
	}

}
