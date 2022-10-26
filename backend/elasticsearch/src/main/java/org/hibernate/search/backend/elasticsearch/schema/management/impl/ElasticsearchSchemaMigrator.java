/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.schema.management.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.aliases.impl.IndexAliasDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.impl.IndexMetadata;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.IndexSettings;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.impl.Throwables;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * An object responsible for updating an existing index to match provided metadata.
 * @author Gunnar Morling
 */
final class ElasticsearchSchemaMigrator {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchSchemaAccessor schemaAccessor;
	private final ElasticsearchSchemaValidator schemaValidator;

	public ElasticsearchSchemaMigrator(ElasticsearchSchemaAccessor schemaAccessor,
			ElasticsearchSchemaValidator schemaValidator) {
		this.schemaAccessor = schemaAccessor;
		this.schemaValidator = schemaValidator;
	}

	/**
	 * Update the existing schema to match the given metadata: for each mapping,
	 * update the existing mappings and analyzer definitions to match the expected ones,
	 * throwing {@link SearchException} if an incompatible attribute is detected.
	 *
	 * <p>The index is expected to already exist.
	 *
	 * @param indexName The name of the index to migrate.
	 * @param expectedIndexMetadata The expected index metadata.
	 * @param actualIndexMetadata The actual index metadata.
	 * @param operationSubmitter How to handle request to submit operation when the queue is full.
	 * @return A future.
	 * @throws SearchException If an error occurs.
	 */
	public CompletableFuture<?> migrate(URLEncodedString indexName,
			IndexMetadata expectedIndexMetadata, IndexMetadata actualIndexMetadata,
			OperationSubmitter operationSubmitter) {
		/*
		 * We only update aliases if it's really necessary,
		 * because we might not even need aliases.
		 */
		CompletableFuture<?> aliasMigration;
		if ( schemaValidator.isAliasesValid( expectedIndexMetadata, actualIndexMetadata ) ) {
			aliasMigration = CompletableFuture.completedFuture( null );
		}
		else {
			aliasMigration = doMigrateAliases( indexName, expectedIndexMetadata.getAliases(), operationSubmitter );
		}

		/*
		 * We only update settings if it's really necessary, because closing the index,
		 * even for just a moment, may hurt if other clients are using the index.
		 */
		CompletableFuture<?> settingsMigration;
		if ( schemaValidator.isSettingsValid( expectedIndexMetadata, actualIndexMetadata ) ) {
			settingsMigration = aliasMigration;
		}
		else {
			settingsMigration = aliasMigration.thenCompose(
					ignored -> doMigrateSettings( indexName, expectedIndexMetadata.getSettings(),
							actualIndexMetadata.getSettings(), operationSubmitter
					) );
		}

		/*
		 * We only update mapping if it's really necessary,
		 * because migrating might erase some user-defined attributes on existing fields,
		 * even fields that we did not need to migrate.
		 * even for just a moment, may hurt if other clients are using the index.
		 */
		CompletableFuture<?> mappingMigration;
		if ( schemaValidator.isMappingValid( expectedIndexMetadata, actualIndexMetadata ) ) {
			mappingMigration = settingsMigration;
		}
		else {
			mappingMigration = settingsMigration
					.thenCompose( ignored -> doMigrateMapping( indexName, expectedIndexMetadata.getMapping(), operationSubmitter ) );
		}

		return mappingMigration.exceptionally( Futures.handler( e -> {
			throw log.schemaUpdateFailed(
					indexName, e.getMessage(),
					Throwables.expectException( e )
			);
		} ) );
	}

	private CompletableFuture<?> doMigrateAliases(URLEncodedString indexName, Map<String, IndexAliasDefinition> aliases,
			OperationSubmitter operationSubmitter) {
		return schemaAccessor.updateAliases( indexName, aliases, operationSubmitter );
	}

	private CompletableFuture<?> doMigrateSettings(URLEncodedString indexName, IndexSettings expectedSettings,
			IndexSettings actualSettings, OperationSubmitter operationSubmitter) {

		// remove all already present settings extra attributes
		IndexSettings indexSettings = expectedSettings.diff( actualSettings.getExtraAttributes() );

		return schemaAccessor.closeIndex( indexName, operationSubmitter )
				.thenCompose( ignored -> Futures.whenCompleteExecute(
						schemaAccessor.updateSettings( indexName, indexSettings, operationSubmitter ),
						// Re-open the index after the settings have been successfully updated
						// ... or if the settings update fails.
						() -> schemaAccessor.openIndex( indexName, operationSubmitter )
				) );
	}

	private CompletableFuture<?> doMigrateMapping(URLEncodedString indexName, RootTypeMapping mapping,
			OperationSubmitter operationSubmitter) {
		// Elasticsearch itself takes care of the actual merging
		return schemaAccessor.updateMapping( indexName, mapping, operationSubmitter );
	}

}
