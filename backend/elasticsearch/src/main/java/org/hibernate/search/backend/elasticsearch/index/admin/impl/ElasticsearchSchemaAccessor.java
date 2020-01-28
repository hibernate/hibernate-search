/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.admin.impl;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.index.IndexStatus;
import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.index.settings.esnative.impl.IndexSettings;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.link.impl.ElasticsearchLink;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.ElasticsearchWorkBuilderFactory;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.result.impl.CreateIndexResult;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.impl.Throwables;

/**
 * A utility implementing primitives for the various {@code ElasticsearchSchema*Impl}.
 * @author Gunnar Morling
 */
public class ElasticsearchSchemaAccessor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchLink link;

	private final ElasticsearchWorkOrchestrator orchestrator;

	public ElasticsearchSchemaAccessor(ElasticsearchLink link,
			ElasticsearchWorkOrchestrator orchestrator) {
		this.link = link;
		this.orchestrator = orchestrator;
	}

	public CompletableFuture<?> createIndexAssumeNonExisting(URLEncodedString indexName, IndexSettings settings,
			RootTypeMapping mapping) {
		ElasticsearchWork<?> work = getWorkFactory().createIndex( indexName )
				.settings( settings )
				.mapping( mapping )
				.build();
		return execute( work );
	}

	/**
	 * @param indexName The name of the index
	 * @param settings The settings for the newly created index
	 * @return {@code true} if the index was actually created, {@code false} if it already existed.
	 */
	public CompletableFuture<Boolean> createIndexIgnoreExisting(URLEncodedString indexName, IndexSettings settings,
			RootTypeMapping mapping) {
		ElasticsearchWork<CreateIndexResult> work = getWorkFactory().createIndex( indexName )
				.settings( settings )
				.mapping( mapping )
				.ignoreExisting()
				.build();
		return execute( work ).thenApply( CreateIndexResult.CREATED::equals );
	}

	public CompletableFuture<Boolean> indexExists(URLEncodedString indexName) {
		ElasticsearchWork<Boolean> work = getWorkFactory().indexExists( indexName ).build();
		return execute( work );
	}

	public CompletableFuture<IndexMetadata> getCurrentIndexMetadata(URLEncodedString indexName) {
		ElasticsearchWork<IndexMetadata> work = getWorkFactory().getIndexMetadata( indexName ).build();
		return execute( work )
				.exceptionally( Futures.handler( e -> {
					throw log.elasticsearchIndexMetadataRetrievalForValidationFailed(
							Throwables.expectException( e )
					);
				} ) );
	}

	public CompletableFuture<?> updateSettings(URLEncodedString indexName, IndexSettings settings) {
		ElasticsearchWork<?> work = getWorkFactory().putIndexSettings( indexName, settings ).build();
		return execute( work )
				.exceptionally( Futures.handler( e -> {
					throw log.elasticsearchSettingsUpdateFailed(
							indexName.original, Throwables.expectException( e )
					);
				} ) );
	}

	public CompletableFuture<?> putMapping(URLEncodedString indexName, RootTypeMapping mapping) {
		ElasticsearchWork<?> work = getWorkFactory().putIndexTypeMapping( indexName, mapping ).build();
		return execute( work )
				.exceptionally( Futures.handler( e -> {
					throw log.elasticsearchMappingCreationFailed(
							indexName.original, e.getMessage(), Throwables.expectException( e )
					);
				} ) );
	}

	public CompletableFuture<?> waitForIndexStatus(final URLEncodedString indexName, ElasticsearchIndexLifecycleExecutionOptions executionOptions) {
		IndexStatus requiredIndexStatus = executionOptions.getRequiredStatus();
		String timeoutAndUnit = executionOptions.getRequiredStatusTimeoutInMs() + "ms";

		ElasticsearchWork<?> work =
				getWorkFactory().waitForIndexStatusWork( indexName, requiredIndexStatus, timeoutAndUnit )
				.build();
		return execute( work )
				.exceptionally( Futures.handler( e -> {
					throw log.unexpectedIndexStatus(
							indexName.original, requiredIndexStatus.getElasticsearchString(), timeoutAndUnit,
							Throwables.expectException( e )
					);
				} ) );
	}

	public CompletableFuture<?> dropIndexIfExisting(URLEncodedString indexName) {
		ElasticsearchWork<?> work = getWorkFactory().dropIndex( indexName ).ignoreIndexNotFound().build();
		return execute( work );
	}

	public CompletableFuture<?> closeIndex(URLEncodedString indexName) {
		ElasticsearchWork<?> work = getWorkFactory().closeIndex( indexName ).build();
		return execute( work )
				.thenRun( () -> log.closedIndex( indexName ) );
	}

	public CompletableFuture<?> openIndex(URLEncodedString indexName) {
		ElasticsearchWork<?> work = getWorkFactory().openIndex( indexName ).build();
		return execute( work )
				.thenRun( () -> log.openedIndex( indexName ) );
	}

	private ElasticsearchWorkBuilderFactory getWorkFactory() {
		return link.getWorkBuilderFactory();
	}

	private <T> CompletableFuture<T> execute(ElasticsearchWork<T> work) {
		return orchestrator.submit( work );
	}
}
