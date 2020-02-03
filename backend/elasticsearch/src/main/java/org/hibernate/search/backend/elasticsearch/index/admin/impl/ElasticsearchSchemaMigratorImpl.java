/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.admin.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.aliases.impl.IndexAliasDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.impl.IndexMetadata;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.IndexSettings;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.impl.Throwables;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * The default {@link ElasticsearchSchemaMigrator} implementation.
 * @author Gunnar Morling
 */
public class ElasticsearchSchemaMigratorImpl implements ElasticsearchSchemaMigrator {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchSchemaAccessor schemaAccessor;
	private final ElasticsearchSchemaValidator schemaValidator;

	public ElasticsearchSchemaMigratorImpl(ElasticsearchSchemaAccessor schemaAccessor,
			ElasticsearchSchemaValidator schemaValidator) {
		this.schemaAccessor = schemaAccessor;
		this.schemaValidator = schemaValidator;
	}

	@Override
	public CompletableFuture<?> migrate(URLEncodedString indexName,
			IndexMetadata expectedIndexMetadata, IndexMetadata actualIndexMetadata) {
		/*
		 * We only update aliases if it's really necessary,
		 * because we might overwrite custom user attributes.
		 */
		CompletableFuture<?> aliasMigration = doMigrateAliases( indexName, expectedIndexMetadata.getAliases() );

		/*
		 * We only update settings if it's really necessary, because closing the index,
		 * even for just a moment, may hurt if other clients are using the index.
		 */
		CompletableFuture<?> settingsMigration;
		if ( schemaValidator.isSettingsValid( expectedIndexMetadata, actualIndexMetadata ) ) {
			settingsMigration = aliasMigration;
		}
		else {
			settingsMigration = aliasMigration
					.thenCompose( ignored -> doMigrateSettings( indexName, expectedIndexMetadata.getSettings() ) );
		}

		return settingsMigration.thenCompose( ignored -> doMigrateMapping( indexName, expectedIndexMetadata.getMapping() ) )
				.exceptionally( Futures.handler( e -> {
					throw log.schemaUpdateFailed(
							indexName, e.getMessage(),
							Throwables.expectException( e )
					);
				} ) );
	}

	private CompletableFuture<?> doMigrateAliases(URLEncodedString indexName, Map<String, IndexAliasDefinition> aliases) {
		return schemaAccessor.putAliases( indexName, aliases );
	}

	private CompletableFuture<?> doMigrateSettings(URLEncodedString indexName, IndexSettings settings) {
		return schemaAccessor.closeIndex( indexName )
				.thenCompose( ignored -> Futures.whenCompleteExecute(
						schemaAccessor.updateSettings( indexName, settings ),
						// Re-open the index after the settings have been successfully updated
						// ... or if the settings update fails.
						() -> schemaAccessor.openIndex( indexName )
				) );
	}

	private CompletableFuture<?> doMigrateMapping(URLEncodedString indexName, RootTypeMapping mapping) {
		// Elasticsearch itself takes care of the actual merging
		return schemaAccessor.putMapping( indexName, mapping );
	}

}
