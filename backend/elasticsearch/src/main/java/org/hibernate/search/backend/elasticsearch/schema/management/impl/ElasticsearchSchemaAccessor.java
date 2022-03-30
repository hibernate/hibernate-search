/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.schema.management.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.index.layout.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.index.IndexStatus;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.IndexSettings;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.aliases.impl.IndexAliasDefinition;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchParallelWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.ElasticsearchWorkBuilderFactory;
import org.hibernate.search.backend.elasticsearch.work.impl.NonBulkableWork;
import org.hibernate.search.backend.elasticsearch.work.result.impl.CreateIndexResult;
import org.hibernate.search.backend.elasticsearch.work.result.impl.ExistingIndexMetadata;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.impl.Throwables;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A utility implementing primitives for the various {@code ElasticsearchSchema*Impl}.
 * @author Gunnar Morling
 */
final class ElasticsearchSchemaAccessor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchWorkBuilderFactory workBuilderFactory;

	private final ElasticsearchParallelWorkOrchestrator orchestrator;

	public ElasticsearchSchemaAccessor(ElasticsearchWorkBuilderFactory workBuilderFactory,
			ElasticsearchParallelWorkOrchestrator orchestrator) {
		this.workBuilderFactory = workBuilderFactory;
		this.orchestrator = orchestrator;
	}

	public CompletableFuture<?> createIndexAssumeNonExisting(URLEncodedString primaryIndexName,
			Map<String, IndexAliasDefinition> aliases, IndexSettings settings, RootTypeMapping mapping) {
		NonBulkableWork<?> work = getWorkFactory().createIndex( primaryIndexName )
				.aliases( aliases )
				.settings( settings )
				.mapping( mapping )
				.build();
		return execute( work );
	}

	/**
	 * @param primaryIndexName The name of the created index.
	 * @param aliases The aliases for the newly created index.
	 * @param settings The settings for the newly created index.
	 * @param mapping The root mapping for the newly created index.
	 * @return A future holding {@code true} if the index was actually created, {@code false} if it already existed.
	 */
	public CompletableFuture<Boolean> createIndexIgnoreExisting(URLEncodedString primaryIndexName,
			Map<String, IndexAliasDefinition> aliases, IndexSettings settings,
			RootTypeMapping mapping) {
		NonBulkableWork<CreateIndexResult> work = getWorkFactory().createIndex( primaryIndexName )
				.aliases( aliases )
				.settings( settings )
				.mapping( mapping )
				.ignoreExisting()
				.build();
		return execute( work ).thenApply( CreateIndexResult.CREATED::equals );
	}

	public CompletableFuture<ExistingIndexMetadata> getCurrentIndexMetadata(IndexNames indexNames) {
		return getCurrentIndexMetadata( indexNames, false );
	}

	public CompletableFuture<ExistingIndexMetadata> getCurrentIndexMetadataOrNull(IndexNames indexNames) {
		return getCurrentIndexMetadata( indexNames, true );
	}

	private CompletableFuture<ExistingIndexMetadata> getCurrentIndexMetadata(IndexNames indexNames, boolean allowNull) {
		NonBulkableWork<List<ExistingIndexMetadata>> work = getWorkFactory().getIndexMetadata()
				.index( indexNames.write() )
				.index( indexNames.read() )
				.build();
		return execute( work )
				.exceptionally( Futures.handler( e -> {
					throw log.elasticsearchIndexMetadataRetrievalFailed( e.getMessage(),
							Throwables.expectException( e ) );
				} ) )
				.thenApply( list -> {
					if ( list.isEmpty() ) {
						if ( allowNull ) {
							return null;
						}
						else {
							throw log.indexMissing( indexNames.write(), indexNames.read() );
						}
					}
					if ( list.size() > 1 ) {
						throw log.elasticsearchIndexNameAndAliasesMatchMultipleIndexes(
								indexNames.write(), indexNames.read(),
								list.stream().map( ExistingIndexMetadata::getPrimaryName ).collect( Collectors.toSet() )
						);
					}
					return list.get( 0 );
				} );
	}

	public CompletableFuture<?> updateAliases(URLEncodedString indexName, Map<String, IndexAliasDefinition> aliases) {
		NonBulkableWork<?> work = getWorkFactory().putIndexAliases( indexName, aliases ).build();
		return execute( work )
				.exceptionally( Futures.handler( e -> {
					throw log.elasticsearchAliasUpdateFailed( indexName.original, e.getMessage(),
							Throwables.expectException( e ) );
				} ) );
	}

	public CompletableFuture<?> updateSettings(URLEncodedString indexName, IndexSettings settings) {
		NonBulkableWork<?> work = getWorkFactory().putIndexSettings( indexName, settings ).build();
		return execute( work )
				.exceptionally( Futures.handler( e -> {
					throw log.elasticsearchSettingsUpdateFailed( indexName.original, e.getMessage(),
							Throwables.expectException( e ) );
				} ) );
	}

	public CompletableFuture<?> updateMapping(URLEncodedString indexName, RootTypeMapping mapping) {
		NonBulkableWork<?> work = getWorkFactory().putIndexTypeMapping( indexName, mapping ).build();
		return execute( work )
				.exceptionally( Futures.handler( e -> {
					throw log.elasticsearchMappingUpdateFailed(
							indexName.original, e.getMessage(), Throwables.expectException( e )
					);
				} ) );
	}

	public CompletableFuture<?> waitForIndexStatus(IndexNames indexNames, ElasticsearchIndexLifecycleExecutionOptions executionOptions) {
		IndexStatus requiredIndexStatus = executionOptions.getRequiredStatus();
		int requiredStatusTimeoutInMs = executionOptions.getRequiredStatusTimeoutInMs();

		URLEncodedString name = indexNames.write();

		NonBulkableWork<?> work =
				getWorkFactory().waitForIndexStatusWork( name, requiredIndexStatus, requiredStatusTimeoutInMs )
						.build();
		return execute( work )
				.exceptionally( Futures.handler( e -> {
					throw log.unexpectedIndexStatus(
							name, requiredIndexStatus.externalRepresentation(), requiredStatusTimeoutInMs,
							Throwables.expectException( e )
					);
				} ) );
	}

	public CompletableFuture<?> dropIndexIfExisting(URLEncodedString indexName) {
		NonBulkableWork<?> work = getWorkFactory().dropIndex( indexName ).ignoreIndexNotFound().build();
		return execute( work );
	}

	public CompletableFuture<?> closeIndex(URLEncodedString indexName) {
		NonBulkableWork<?> work = getWorkFactory().closeIndex( indexName ).build();
		return execute( work )
				.thenRun( () -> log.closedIndex( indexName ) );
	}

	public CompletableFuture<?> openIndex(URLEncodedString indexName) {
		NonBulkableWork<?> work = getWorkFactory().openIndex( indexName ).build();
		return execute( work )
				.thenRun( () -> log.openedIndex( indexName ) );
	}

	private ElasticsearchWorkBuilderFactory getWorkFactory() {
		return workBuilderFactory;
	}

	private <T> CompletableFuture<T> execute(NonBulkableWork<T> work) {
		return orchestrator.submit( work );
	}
}
