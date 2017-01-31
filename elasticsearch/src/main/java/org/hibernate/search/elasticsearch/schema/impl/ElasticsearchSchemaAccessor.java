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

import org.hibernate.search.elasticsearch.client.impl.JestClient;
import org.hibernate.search.elasticsearch.impl.GsonService;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.schema.impl.model.IndexMetadata;
import org.hibernate.search.elasticsearch.schema.impl.model.TypeMapping;
import org.hibernate.search.elasticsearch.settings.impl.model.IndexSettings;
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

	private JestClient jestClient;

	private GsonService gsonService;

	@Override
	public void start(Properties properties, BuildContext context) {
		serviceManager = context.getServiceManager();
		jestClient = serviceManager.requestService( JestClient.class );
		gsonService = serviceManager.requestService( GsonService.class );
	}

	@Override
	public void stop() {
		jestClient = null;
		serviceManager.releaseService( JestClient.class );
		gsonService = null;
		serviceManager.releaseService( GsonService.class );
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

		jestClient.executeRequest( createIndex );
	}

	/**
	 * @return {@code true} if the index was actually created, {@code false} if it already existed.
	 */
	public boolean createIndexIfAbsent(String indexName, IndexSettings settings, ExecutionOptions executionOptions) {
		String settingsAsJson = settings == null ? null : serializeAsJsonWithoutNulls( settings );

		CreateIndex createIndex = new CreateIndex.Builder( indexName )
				.settings( settingsAsJson )
				.build();

		JestResult result = jestClient.executeRequest( createIndex, "index_already_exists_exception" );
		if ( !result.isSucceeded() ) {
			// The index was created just after we checked if it existed: just do as if it had been created when we checked.
			return false;
		}

		return true;
	}

	public boolean indexExists(String indexName) {
		JestResult peekResult = jestClient.executeRequest( new IndicesExists.Builder( indexName ).build(), 404 );
		return peekResult.getResponseCode() == 200;
	}

	public IndexMetadata getCurrentIndexMetadata(String indexName) {
		IndexMetadata indexMetadata = new IndexMetadata();
		indexMetadata.setName( indexName );

		GetMapping getMapping = new GetMapping.Builder()
				.addIndex( indexName )
				.build();
		try {
			JestResult result = jestClient.executeRequest( getMapping );
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
		try {
			JestResult result = jestClient.executeRequest( getSettings );
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

		try {
			jestClient.executeRequest( putSettings );
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

		try {
			jestClient.executeRequest( putMapping );
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

		JestResult result = jestClient.executeRequest( health, 408 );

		if ( !result.isSucceeded() ) {
			String status = result.getJsonObject().get( "status" ).getAsString();
			throw LOG.unexpectedIndexStatus( theIndexName, requiredIndexStatusString, status, timeoutAndUnit );
		}
	}

	public void dropIndex(String indexName, ExecutionOptions executionOptions) {
		jestClient.executeRequest( new DeleteIndex.Builder( indexName ).build() );
	}

	public void closeIndex(String indexName) {
		jestClient.executeRequest( new CloseIndex.Builder( indexName ).build() );
		LOG.closedIndex( indexName );
	}

	public void openIndex(String indexName) {
		try {
			jestClient.executeRequest( new OpenIndex.Builder( indexName ).build() );
		}
		catch (RuntimeException e) {
			LOG.openedIndex( indexName );
			throw e;
		}
		LOG.openedIndex( indexName );
	}
}
