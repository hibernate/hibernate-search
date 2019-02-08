/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.admin.impl;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletionException;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexStatus;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.index.settings.impl.esnative.IndexSettings;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.ElasticsearchWorkBuilderFactory;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.result.impl.CreateIndexResult;
import org.hibernate.search.util.impl.common.Futures;
import org.hibernate.search.util.impl.common.LoggerFactory;
import org.hibernate.search.util.impl.common.Throwables;

/**
 * A utility implementing primitives for the various {@code ElasticsearchSchema*Impl}.
 * @author Gunnar Morling
 * @author Yoann Rodiere
 */
public class ElasticsearchSchemaAccessor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchWorkBuilderFactory workFactory;

	private final ElasticsearchWorkOrchestrator orchestrator;

	public ElasticsearchSchemaAccessor(ElasticsearchWorkBuilderFactory workFactory,
			ElasticsearchWorkOrchestrator orchestrator) {
		this.workFactory = workFactory;
		this.orchestrator = orchestrator;
	}

	public void createIndex(URLEncodedString indexName, IndexSettings settings,
			RootTypeMapping mapping) {
		ElasticsearchWork<?> work = workFactory.createIndex( indexName )
				.settings( settings )
				.mapping( mapping )
				.build();
		execute( work );
	}

	/**
	 * @param indexName The name of the index
	 * @param settings The settings for the newly created index
	 * @return {@code true} if the index was actually created, {@code false} if it already existed.
	 */
	public boolean createIndexIfAbsent(URLEncodedString indexName, IndexSettings settings,
			RootTypeMapping mapping) {
		ElasticsearchWork<CreateIndexResult> work = workFactory.createIndex( indexName )
				.settings( settings )
				.mapping( mapping )
				.ignoreExisting()
				.build();
		CreateIndexResult result = execute( work );
		return CreateIndexResult.CREATED.equals( result );
	}

	public boolean indexExists(URLEncodedString indexName) {
		ElasticsearchWork<Boolean> work = workFactory.indexExists( indexName ).build();
		return execute( work );
	}

	public IndexMetadata getCurrentIndexMetadata(URLEncodedString indexName) {
		IndexMetadata indexMetadata = new IndexMetadata();
		indexMetadata.setName( indexName );

		ElasticsearchWork<RootTypeMapping> getMappingWork = workFactory.getIndexTypeMapping( indexName ).build();
		try {
			RootTypeMapping mapping = execute( getMappingWork );
			indexMetadata.setMapping( mapping );
		}
		catch (RuntimeException e) {
			throw log.elasticsearchMappingRetrievalForValidationFailed( e );
		}

		ElasticsearchWork<IndexSettings> getSettingsWork = workFactory.getIndexSettings( indexName ).build();
		try {
			IndexSettings indexSettings = execute( getSettingsWork );
			indexMetadata.setSettings( indexSettings );
		}
		catch (RuntimeException e) {
			throw log.elasticsearchIndexSettingsRetrievalForValidationFailed( e );
		}

		return indexMetadata;
	}

	public void updateSettings(URLEncodedString indexName, IndexSettings settings) {
		ElasticsearchWork<?> work = workFactory.putIndexSettings( indexName, settings ).build();

		try {
			execute( work );
		}
		catch (RuntimeException e) {
			throw log.elasticsearchSettingsUpdateFailed( indexName, e );
		}
	}

	public void putMapping(URLEncodedString indexName, RootTypeMapping mapping) {
		ElasticsearchWork<?> work = workFactory.putIndexTypeMapping( indexName, mapping ).build();

		try {
			execute( work );
		}
		catch (RuntimeException e) {
			throw log.elasticsearchMappingCreationFailed( indexName.original, e.getMessage(), e );
		}
	}

	public void waitForIndexStatus(final URLEncodedString indexName, ElasticsearchIndexLifecycleExecutionOptions executionOptions) {
		ElasticsearchIndexStatus requiredIndexStatus = executionOptions.getRequiredStatus();
		String timeoutAndUnit = executionOptions.getRequiredStatusTimeoutInMs() + "ms";

		ElasticsearchWork<?> work =
				workFactory.waitForIndexStatusWork( indexName, requiredIndexStatus, timeoutAndUnit )
				.build();

		try {
			execute( work );
		}
		catch (RuntimeException e) {
			throw log.unexpectedIndexStatus(
					indexName.original, requiredIndexStatus.getElasticsearchString(), timeoutAndUnit, e
			);
		}
	}

	public void dropIndexIfExisting(URLEncodedString indexName) {
		ElasticsearchWork<?> work = workFactory.dropIndex( indexName ).ignoreIndexNotFound().build();
		execute( work );
	}

	public void closeIndex(URLEncodedString indexName) {
		ElasticsearchWork<?> work = workFactory.closeIndex( indexName ).build();
		execute( work );
		log.closedIndex( indexName );
	}

	public void openIndex(URLEncodedString indexName) {
		try {
			ElasticsearchWork<?> work = workFactory.openIndex( indexName ).build();
			execute( work );
		}
		catch (RuntimeException e) {
			log.openedIndex( indexName );
			throw e;
		}
		log.openedIndex( indexName );
	}

	private <T> T execute(ElasticsearchWork<T> work) {
		try {
			return Futures.unwrappedExceptionJoin( orchestrator.submit( work ) );
		}
		catch (CompletionException e) {
			throw Throwables.expectRuntimeException( e );
		}
	}
}
