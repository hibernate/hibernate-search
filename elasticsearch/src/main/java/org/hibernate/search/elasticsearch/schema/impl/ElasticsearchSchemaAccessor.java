/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.schema.impl;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Properties;

import org.hibernate.search.elasticsearch.impl.GsonService;
import org.hibernate.search.elasticsearch.impl.JestAPIFormatter;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.processor.impl.ElasticsearchWorkProcessor;
import org.hibernate.search.elasticsearch.schema.impl.model.IndexMetadata;
import org.hibernate.search.elasticsearch.schema.impl.model.TypeMapping;
import org.hibernate.search.elasticsearch.settings.impl.model.IndexSettings;
import org.hibernate.search.elasticsearch.work.impl.DefaultElasticsearchRequestResultAssessor;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchRequestResultAssessor;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.elasticsearch.work.impl.NoopElasticsearchWorkSuccessReporter;
import org.hibernate.search.elasticsearch.work.impl.SimpleElasticsearchWork;
import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.engine.service.spi.Stoppable;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import io.searchbox.action.AbstractAction;
import io.searchbox.client.JestResult;
import io.searchbox.cluster.Health;
import io.searchbox.cluster.Health.Builder;
import io.searchbox.indices.CloseIndex;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.DeleteIndex;
import io.searchbox.indices.IndicesExists;
import io.searchbox.indices.OpenIndex;
import io.searchbox.indices.mapping.GetMapping;
import io.searchbox.indices.mapping.PutMapping;
import io.searchbox.indices.settings.GetSettings;
import io.searchbox.indices.settings.UpdateSettings;

/**
 * A utility implementing primitives for the various {@code DefaultElasticsearchSchema*}.
 * @author Gunnar Morling
 * @author Yoann Rodiere
 */
public class ElasticsearchSchemaAccessor implements Service, Startable, Stoppable {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private static final TypeToken<Map<String, TypeMapping>> STRING_TO_TYPE_MAPPING_MAP_TYPE_TOKEN =
			new TypeToken<Map<String, TypeMapping>>() {
				// Create a new class to capture generic parameters
			};

	private ServiceManager serviceManager;

	private ElasticsearchWorkProcessor workProcessor;

	private GsonService gsonService;

	private JestAPIFormatter jestApiFormatter;

	private ElasticsearchRequestResultAssessor<JestResult> defaultResultAssessor;
	private ElasticsearchRequestResultAssessor<JestResult> createIndexIfAbsentResultAssessor;
	private ElasticsearchRequestResultAssessor<JestResult> indexExistsResultAssessor;
	private ElasticsearchRequestResultAssessor<JestResult> healthResultAssessor;

	@Override
	public void start(Properties properties, BuildContext context) {
		serviceManager = context.getServiceManager();
		workProcessor = serviceManager.requestService( ElasticsearchWorkProcessor.class );
		gsonService = serviceManager.requestService( GsonService.class );
		jestApiFormatter = serviceManager.requestService( JestAPIFormatter.class );
		defaultResultAssessor = DefaultElasticsearchRequestResultAssessor.builder( jestApiFormatter ).build();
		createIndexIfAbsentResultAssessor = DefaultElasticsearchRequestResultAssessor.builder( jestApiFormatter )
				.ignoreErrorTypes( "index_already_exists_exception" )
				.build();
		indexExistsResultAssessor = DefaultElasticsearchRequestResultAssessor.builder( jestApiFormatter )
				.ignoreErrorStatuses( 404 )
				.build();
		healthResultAssessor = DefaultElasticsearchRequestResultAssessor.builder( jestApiFormatter )
				.ignoreErrorStatuses( 408 )
				.build();
	}

	@Override
	public void stop() {
		defaultResultAssessor = null;
		createIndexIfAbsentResultAssessor = null;
		indexExistsResultAssessor = null;
		healthResultAssessor = null;

		jestApiFormatter = null;
		serviceManager.releaseService( JestAPIFormatter.class );
		gsonService = null;
		serviceManager.releaseService( GsonService.class );
		workProcessor = null;
		serviceManager.releaseService( ElasticsearchWorkProcessor.class );
		serviceManager = null;
	}

	/*
	 * Serializing nulls is really not a good idea here, it triggers NPEs in Elasticsearch
	 * We better not include the null fields.
	 */
	private String serializeAsJsonWithoutNulls(Object object) {
		Gson gson = gsonService.getGsonNoSerializeNulls();
		return gson.toJson( object );
	}

	public void createIndex(String indexName, IndexSettings settings, ExecutionOptions executionOptions) {
		String settingsAsJson = settings == null ? null : serializeAsJsonWithoutNulls( settings );

		CreateIndex createIndex = new CreateIndex.Builder( indexName )
				.settings( settingsAsJson )
				.build();

		ElasticsearchWork<?> work = new SimpleElasticsearchWork<>( createIndex, null, indexName,
				defaultResultAssessor, null, NoopElasticsearchWorkSuccessReporter.INSTANCE, false );
		workProcessor.executeSyncUnsafe( work );
	}

	/**
	 * @return {@code true} if the index was actually created, {@code false} if it already existed.
	 */
	public boolean createIndexIfAbsent(String indexName, IndexSettings settings, ExecutionOptions executionOptions) {
		String settingsAsJson = settings == null ? null : serializeAsJsonWithoutNulls( settings );

		CreateIndex createIndex = new CreateIndex.Builder( indexName )
				.settings( settingsAsJson )
				.build();

		ElasticsearchWork<JestResult> work = new SimpleElasticsearchWork<>( createIndex, null, indexName,
				createIndexIfAbsentResultAssessor, null, NoopElasticsearchWorkSuccessReporter.INSTANCE, false );
		JestResult result = workProcessor.executeSyncUnsafe( work );
		if ( !result.isSucceeded() ) {
			// The index was created just after we checked if it existed: just do as if it had been created when we checked.
			return false;
		}

		return true;
	}

	public boolean indexExists(String indexName) {
		IndicesExists indicesExists = new IndicesExists.Builder( indexName ).build();
		ElasticsearchWork<JestResult> work = new SimpleElasticsearchWork<>( indicesExists, null, indexName,
				indexExistsResultAssessor, null, NoopElasticsearchWorkSuccessReporter.INSTANCE, false );

		JestResult peekResult = workProcessor.executeSyncUnsafe( work );
		return peekResult.getResponseCode() == 200;
	}

	public IndexMetadata getCurrentIndexMetadata(String indexName) {
		IndexMetadata indexMetadata = new IndexMetadata();
		indexMetadata.setName( indexName );

		GetMapping getMapping = new GetMapping.Builder()
				.addIndex( indexName )
				.build();
		ElasticsearchWork<JestResult> getMappingWork = new SimpleElasticsearchWork<>( getMapping, null, indexName,
				defaultResultAssessor, null, NoopElasticsearchWorkSuccessReporter.INSTANCE, false );

		try {
			JestResult result = workProcessor.executeSyncUnsafe( getMappingWork );
			JsonObject resultJson = result.getJsonObject();
			JsonElement index = result.getJsonObject().get( indexName );
			if ( index == null || !index.isJsonObject() ) {
				throw new AssertionFailure( "Elasticsearch API call succeeded, but the requested index wasn't mentioned in the result: " + resultJson );
			}
			JsonElement mappings = index.getAsJsonObject().get( "mappings" );

			if ( mappings != null ) {
				Type mapType = STRING_TO_TYPE_MAPPING_MAP_TYPE_TOKEN.getType();
				indexMetadata.setMappings( gsonService.getGson().<Map<String, TypeMapping>>fromJson( mappings, mapType ) );
			}
		}
		catch (RuntimeException e) {
			throw LOG.elasticsearchMappingRetrievalForValidationFailed( e );
		}

		GetSettings getSettings = new GetSettings.Builder()
				.addIndex( indexName )
				.build();
		ElasticsearchWork<JestResult> getSettingsWork = new SimpleElasticsearchWork<>( getSettings, null, indexName,
				defaultResultAssessor, null, NoopElasticsearchWorkSuccessReporter.INSTANCE, false );

		try {
			JestResult result = workProcessor.executeSyncUnsafe( getSettingsWork );
			JsonObject resultJson = result.getJsonObject();
			JsonElement index = result.getJsonObject().get( indexName );
			if ( index == null || !index.isJsonObject() ) {
				throw new AssertionFailure( "Elasticsearch API call succeeded, but the requested index wasn't mentioned in the result: " + resultJson );
			}

			JsonElement settings = index.getAsJsonObject().get( "settings" );
			if ( settings == null || !settings.isJsonObject() ) {
				throw new AssertionFailure( "Elasticsearch API call succeeded, but the requested settings weren't mentioned in the result: " + resultJson );
			}

			JsonElement indexSettings = settings.getAsJsonObject().get( "index" );
			if ( indexSettings != null ) {
				indexMetadata.setSettings( gsonService.getGson().fromJson( indexSettings, IndexSettings.class ) );
			}
			else {
				// Empty settings
				indexMetadata.setSettings( new IndexSettings() );
			}
		}
		catch (RuntimeException e) {
			throw LOG.elasticsearchIndexSettingsRetrievalForValidationFailed( e );
		}

		return indexMetadata;
	}

	public void updateSettings(String indexName, IndexSettings settings) {
		String settingsAsJson = serializeAsJsonWithoutNulls( settings );

		UpdateSettings putSettings = new UpdateSettings.Builder( settingsAsJson )
				.addIndex( indexName )
				.build();
		ElasticsearchWork<JestResult> work = new SimpleElasticsearchWork<>( putSettings, null, indexName,
				defaultResultAssessor, null, NoopElasticsearchWorkSuccessReporter.INSTANCE, false );

		try {
			workProcessor.executeSyncUnsafe( work );
		}
		catch (RuntimeException e) {
			throw LOG.elasticsearchSettingsUpdateFailed( indexName, e );
		}
	}

	public void putMapping(String indexName, String mappingName, TypeMapping mapping) {
		String mappingAsJson = serializeAsJsonWithoutNulls( mapping );

		PutMapping putMapping = new PutMapping.Builder(
				indexName,
				mappingName,
				mappingAsJson
		)
		.build();
		ElasticsearchWork<JestResult> work = new SimpleElasticsearchWork<>( putMapping, null, indexName,
				defaultResultAssessor, null, NoopElasticsearchWorkSuccessReporter.INSTANCE, false );

		try {
			workProcessor.executeSyncUnsafe( work );
		}
		catch (RuntimeException e) {
			throw LOG.elasticsearchMappingCreationFailed( mappingName, e );
		}
	}

	public void waitForIndexStatus(final String theIndexName, ExecutionOptions executionOptions) {
		String requiredIndexStatusString = executionOptions.getRequiredIndexStatus().getElasticsearchString();
		String timeoutAndUnit = executionOptions.getIndexManagementTimeoutInMs() + "ms";
		Builder healthBuilder = new Health.Builder()
				.setParameter( "wait_for_status", requiredIndexStatusString )
				.setParameter( "timeout", timeoutAndUnit );

		Health health = new Health( healthBuilder ) {
			@Override
			protected String buildURI() {
				try {
					// Use a different variable name than "indexName", because there is an "indexName" attribute in a superclass
					return super.buildURI() + URLEncoder.encode(theIndexName, AbstractAction.CHARSET);
				}
				catch (UnsupportedEncodingException e) {
					throw new AssertionFailure( "Unexpectedly unsupported charset", e );
				}
			}
		};
		ElasticsearchWork<JestResult> work = new SimpleElasticsearchWork<>( health, null, theIndexName,
				healthResultAssessor, null, NoopElasticsearchWorkSuccessReporter.INSTANCE, false );

		JestResult result = workProcessor.executeSyncUnsafe( work );

		if ( !result.isSucceeded() ) {
			String status = result.getJsonObject().get( "status" ).getAsString();
			throw LOG.unexpectedIndexStatus( theIndexName, requiredIndexStatusString, status, timeoutAndUnit );
		}
	}

	public void dropIndex(String indexName, ExecutionOptions executionOptions) {
		DeleteIndex action = new DeleteIndex.Builder( indexName ).build();
		ElasticsearchWork<JestResult> work = new SimpleElasticsearchWork<>( action, null, indexName,
				defaultResultAssessor, null, NoopElasticsearchWorkSuccessReporter.INSTANCE, false );

		workProcessor.executeSyncUnsafe( work );
	}

	public void closeIndex(String indexName) {
		CloseIndex action = new CloseIndex.Builder( indexName ).build();
		ElasticsearchWork<JestResult> work = new SimpleElasticsearchWork<>( action, null, indexName,
				defaultResultAssessor, null, NoopElasticsearchWorkSuccessReporter.INSTANCE, false );

		workProcessor.executeSyncUnsafe( work );
		LOG.closedIndex( indexName );
	}

	public void openIndex(String indexName) {
		try {
			OpenIndex action = new OpenIndex.Builder( indexName ).build();
			ElasticsearchWork<JestResult> work = new SimpleElasticsearchWork<>( action, null, indexName,
					defaultResultAssessor, null, NoopElasticsearchWorkSuccessReporter.INSTANCE, false );

			workProcessor.executeSyncUnsafe( work );
		}
		catch (RuntimeException e) {
			LOG.openedIndex( indexName );
			throw e;
		}
		LOG.openedIndex( indexName );
	}
}
