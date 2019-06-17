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

	public void createIndex(URLEncodedString indexName, IndexSettings settings,
			RootTypeMapping mapping) {
		ElasticsearchWork<?> work = getWorkFactory().createIndex( indexName )
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
		ElasticsearchWork<CreateIndexResult> work = getWorkFactory().createIndex( indexName )
				.settings( settings )
				.mapping( mapping )
				.ignoreExisting()
				.build();
		CreateIndexResult result = execute( work );
		return CreateIndexResult.CREATED.equals( result );
	}

	public boolean indexExists(URLEncodedString indexName) {
		ElasticsearchWork<Boolean> work = getWorkFactory().indexExists( indexName ).build();
		return execute( work );
	}

	public IndexMetadata getCurrentIndexMetadata(URLEncodedString indexName) {
		IndexMetadata indexMetadata = new IndexMetadata();
		indexMetadata.setName( indexName );

		ElasticsearchWork<RootTypeMapping> getMappingWork = getWorkFactory().getIndexTypeMapping( indexName ).build();
		try {
			RootTypeMapping mapping = execute( getMappingWork );
			indexMetadata.setMapping( mapping );
		}
		catch (RuntimeException e) {
			throw log.elasticsearchMappingRetrievalForValidationFailed( e );
		}

		ElasticsearchWork<IndexSettings> getSettingsWork = getWorkFactory().getIndexSettings( indexName ).build();
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
		ElasticsearchWork<?> work = getWorkFactory().putIndexSettings( indexName, settings ).build();

		try {
			execute( work );
		}
		catch (RuntimeException e) {
			throw log.elasticsearchSettingsUpdateFailed( indexName, e );
		}
	}

	public void putMapping(URLEncodedString indexName, RootTypeMapping mapping) {
		ElasticsearchWork<?> work = getWorkFactory().putIndexTypeMapping( indexName, mapping ).build();

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
				getWorkFactory().waitForIndexStatusWork( indexName, requiredIndexStatus, timeoutAndUnit )
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
		ElasticsearchWork<?> work = getWorkFactory().dropIndex( indexName ).ignoreIndexNotFound().build();
		execute( work );
	}

	public void closeIndex(URLEncodedString indexName) {
		ElasticsearchWork<?> work = getWorkFactory().closeIndex( indexName ).build();
		execute( work );
		log.closedIndex( indexName );
	}

	public void openIndex(URLEncodedString indexName) {
		try {
			ElasticsearchWork<?> work = getWorkFactory().openIndex( indexName ).build();
			execute( work );
		}
		catch (RuntimeException e) {
			log.openedIndex( indexName );
			throw e;
		}
		log.openedIndex( indexName );
	}

	private ElasticsearchWorkBuilderFactory getWorkFactory() {
		return link.getWorkBuilderFactory();
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
