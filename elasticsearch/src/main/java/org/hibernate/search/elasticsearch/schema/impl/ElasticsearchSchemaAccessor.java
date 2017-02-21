/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.schema.impl;

import java.util.Map;
import java.util.Properties;

import org.hibernate.search.elasticsearch.cfg.ElasticsearchIndexStatus;
import org.hibernate.search.elasticsearch.dialect.impl.ElasticsearchDialectProvider;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.processor.impl.ElasticsearchWorkProcessor;
import org.hibernate.search.elasticsearch.schema.impl.model.IndexMetadata;
import org.hibernate.search.elasticsearch.schema.impl.model.TypeMapping;
import org.hibernate.search.elasticsearch.settings.impl.model.IndexSettings;
import org.hibernate.search.elasticsearch.work.impl.CreateIndexResult;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.elasticsearch.work.impl.factory.ElasticsearchWorkFactory;
import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.engine.service.spi.Stoppable;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A utility implementing primitives for the various {@code DefaultElasticsearchSchema*}.
 * @author Gunnar Morling
 * @author Yoann Rodiere
 */
public class ElasticsearchSchemaAccessor implements Service, Startable, Stoppable {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private ServiceManager serviceManager;

	private ElasticsearchDialectProvider dialectProvider;

	private ElasticsearchWorkFactory workFactory;

	private ElasticsearchWorkProcessor workProcessor;

	@Override
	public void start(Properties properties, BuildContext context) {
		serviceManager = context.getServiceManager();
		dialectProvider = serviceManager.requestService( ElasticsearchDialectProvider.class );
		workFactory = dialectProvider.getDialect().getWorkFactory();
		workProcessor = serviceManager.requestService( ElasticsearchWorkProcessor.class );
	}

	@Override
	public void stop() {
		workProcessor = null;
		serviceManager.releaseService( ElasticsearchWorkProcessor.class );
		workFactory = null;
		dialectProvider = null;
		serviceManager.releaseService( ElasticsearchDialectProvider.class );
		serviceManager = null;
	}

	public void createIndex(String indexName, IndexSettings settings, ExecutionOptions executionOptions) {
		ElasticsearchWork<?> work = workFactory.createIndex( indexName ).settings( settings ).build();
		workProcessor.executeSyncUnsafe( work );
	}

	/**
	 * @return {@code true} if the index was actually created, {@code false} if it already existed.
	 */
	public boolean createIndexIfAbsent(String indexName, IndexSettings settings, ExecutionOptions executionOptions) {
		ElasticsearchWork<CreateIndexResult> work = workFactory.createIndex( indexName )
				.settings( settings )
				.ignoreExisting()
				.build();
		CreateIndexResult result = workProcessor.executeSyncUnsafe( work );
		return CreateIndexResult.CREATED.equals( result );
	}

	public boolean indexExists(String indexName) {
		ElasticsearchWork<Boolean> work = workFactory.indexExists( indexName ).build();
		return workProcessor.executeSyncUnsafe( work );
	}

	public IndexMetadata getCurrentIndexMetadata(String indexName) {
		IndexMetadata indexMetadata = new IndexMetadata();
		indexMetadata.setName( indexName );

		ElasticsearchWork<Map<String, TypeMapping>> getMappingWork = workFactory.getIndexTypeMappings( indexName ).build();
		try {
			Map<String, TypeMapping> mappings = workProcessor.executeSyncUnsafe( getMappingWork );
			indexMetadata.setMappings( mappings );
		}
		catch (RuntimeException e) {
			throw LOG.elasticsearchMappingRetrievalForValidationFailed( e );
		}

		ElasticsearchWork<IndexSettings> getSettingsWork = workFactory.getIndexSettings( indexName ).build();
		try {
			IndexSettings indexSettings = workProcessor.executeSyncUnsafe( getSettingsWork );
			indexMetadata.setSettings( indexSettings );
		}
		catch (RuntimeException e) {
			throw LOG.elasticsearchIndexSettingsRetrievalForValidationFailed( e );
		}

		return indexMetadata;
	}

	public void updateSettings(String indexName, IndexSettings settings) {
		ElasticsearchWork<?> work = workFactory.putIndexSettings( indexName, settings ).build();

		try {
			workProcessor.executeSyncUnsafe( work );
		}
		catch (RuntimeException e) {
			throw LOG.elasticsearchSettingsUpdateFailed( indexName, e );
		}
	}

	public void putMapping(String indexName, String mappingName, TypeMapping mapping) {
		ElasticsearchWork<?> work = workFactory.putIndexTypeMapping( indexName, mappingName, mapping ).build();

		try {
			workProcessor.executeSyncUnsafe( work );
		}
		catch (RuntimeException e) {
			throw LOG.elasticsearchMappingCreationFailed( mappingName, e );
		}
	}

	public void waitForIndexStatus(final String indexName, ExecutionOptions executionOptions) {
		ElasticsearchIndexStatus requiredIndexStatus = executionOptions.getRequiredIndexStatus();
		String timeoutAndUnit = executionOptions.getIndexManagementTimeoutInMs() + "ms";

		ElasticsearchWork<?> work =
				workFactory.waitForIndexStatusWork( indexName, requiredIndexStatus, timeoutAndUnit )
				.build();

		workProcessor.executeSyncUnsafe( work );
	}

	public void dropIndex(String indexName, ExecutionOptions executionOptions) {
		ElasticsearchWork<?> work = workFactory.dropIndex( indexName ).build();
		workProcessor.executeSyncUnsafe( work );
	}

	public void closeIndex(String indexName) {
		ElasticsearchWork<?> work = workFactory.closeIndex( indexName ).build();
		workProcessor.executeSyncUnsafe( work );
		LOG.closedIndex( indexName );
	}

	public void openIndex(String indexName) {
		try {
			ElasticsearchWork<?> work = workFactory.openIndex( indexName ).build();
			workProcessor.executeSyncUnsafe( work );
		}
		catch (RuntimeException e) {
			LOG.openedIndex( indexName );
			throw e;
		}
		LOG.openedIndex( indexName );
	}
}
