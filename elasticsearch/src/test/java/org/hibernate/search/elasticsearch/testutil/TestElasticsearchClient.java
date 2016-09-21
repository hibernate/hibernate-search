/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.testutil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.impl.DefaultGsonService;
import org.hibernate.search.elasticsearch.impl.IndexNameNormalizer;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.junit.rules.ExternalResource;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.searchbox.action.AbstractAction;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.cluster.Health;
import io.searchbox.cluster.Health.Builder;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.DeleteIndex;
import io.searchbox.indices.mapping.GetMapping;
import io.searchbox.indices.mapping.PutMapping;

/**
 * @author Yoann Rodiere
 */
public class TestElasticsearchClient extends ExternalResource {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private JestClient client;

	private final List<String> createdIndicesNames = Lists.newArrayList();

	public void deleteAndCreateIndex(Class<?> rootClass) throws IOException {
		deleteAndCreateIndex( IndexNameNormalizer.getElasticsearchIndexName( rootClass.getName() ) );
	}

	public void putMapping(Class<?> mappedAndRootClass, String mappingJson) throws IOException {
		putMapping( mappedAndRootClass, mappedAndRootClass, mappingJson );
	}

	public void putMapping(Class<?> rootClass, Class<?> mappedClass, String mappingJson) throws IOException {
		putMapping( IndexNameNormalizer.getElasticsearchIndexName( rootClass.getName() ), mappedClass.getName(), mappingJson );
	}

	public String getMapping(Class<?> mappedAndRootClass) throws IOException {
		return getMapping( mappedAndRootClass, mappedAndRootClass );
	}

	public String getMapping(Class<?> rootClass, Class<?> mappedClass) throws IOException {
		return getMapping( IndexNameNormalizer.getElasticsearchIndexName( rootClass.getName() ), mappedClass.getName() );
	}

	public void deleteAndCreateIndex(String indexName) throws IOException {
		// Ignore the result: if the deletion fails, we don't care unless the creation just after also fails
		tryDeleteESIndex( indexName );

		JestResult result = client.execute( new CreateIndex.Builder( indexName ).build() );
		createdIndicesNames.add( indexName );
		if ( !result.isSucceeded() ) {
			throw new AssertionFailure( "Error while creating index '" + indexName + "' for tests:" + result.getErrorMessage() );
		}

		waitForIndexCreation( indexName );
	}

	private void waitForIndexCreation(final String indexNameToWaitFor) throws IOException {
		Builder healthBuilder = new Health.Builder()
				.setParameter( "wait_for_status", ElasticsearchEnvironment.Defaults.REQUIRED_INDEX_STATUS )
				.setParameter( "timeout", ElasticsearchEnvironment.Defaults.INDEX_MANAGEMENT_WAIT_TIMEOUT + "ms" );

		Health health = new Health( healthBuilder ) {
			@Override
			protected String buildURI() {
				try {
					return super.buildURI() + URLEncoder.encode(indexNameToWaitFor, AbstractAction.CHARSET);
				}
				catch (UnsupportedEncodingException e) {
					throw new AssertionFailure( "Unexpectedly unsupported charset", e );
				}
			}
		};

		JestResult result = client.execute( health );
		if ( !result.isSucceeded() ) {
			String status = result.getJsonObject().get( "status" ).getAsString();
			throw new AssertionFailure( "Error while waiting for creation of index '" + indexNameToWaitFor
					+ "' for tests (status was '" + status + "'):" + result.getErrorMessage() );
		}
	}

	public void putMapping(String indexName, String mappingName, String mappingJson) throws IOException {
		/*
		 * Convert to JsonElement first, so that some Elasticsearch peculiarities (such as the fact that
		 * single quotes are not accepted as a substitute for single quotes) can be worked around.
		 * In tests, single quotes are way easier to include in JSON strings, because we don't have to escape them.
		 */
		JsonElement mappingJsonElement = new JsonParser().parse( mappingJson );

		JestResult result = client.execute( new PutMapping.Builder( indexName, mappingName, mappingJsonElement ).build() );
		if ( !result.isSucceeded() ) {
			throw new AssertionFailure( "Error while putting mapping '" + mappingName
					+ "' on index '" + indexName + "' for tests:" + result.getErrorMessage() );
		}
	}

	public String getMapping(String indexName, String mappingName) throws IOException {
		JestResult result = client.execute( new GetMapping.Builder().addIndex( indexName ).addType( mappingName ).build() );
		if ( !result.isSucceeded() ) {
			throw new AssertionFailure( "Error while getting mapping '" + mappingName
					+ "' on index '" + indexName + "' for tests:" + result.getErrorMessage() );
		}
		JsonElement index = result.getJsonObject().get( indexName );
		if ( index == null ) {
			return new JsonObject().toString();
		}
		JsonElement mappings = index.getAsJsonObject().get( "mappings" );
		if ( mappings == null ) {
			return new JsonObject().toString();
		}
		JsonElement mapping = mappings.getAsJsonObject().get( mappingName );
		if ( mapping == null ) {
			return new JsonObject().toString();
		}
		return mapping.toString();
	}

	@Override
	protected void before() throws Throwable {
		JestClientFactory factory = new JestClientFactory();

		Gson gson = new DefaultGsonService().getGson();

		factory.setHttpClientConfig(
			new HttpClientConfig.Builder( ElasticsearchEnvironment.Defaults.SERVER_URI )
				.readTimeout( ElasticsearchEnvironment.Defaults.SERVER_READ_TIMEOUT )
				.connTimeout( ElasticsearchEnvironment.Defaults.SERVER_CONNECTION_TIMEOUT )
				.gson( gson )
				.build()
		);

		client = factory.getObject();
	}

	@Override
	protected void after() {
		for ( String indexName : createdIndicesNames ) {
			tryDeleteESIndex( indexName );
		}
		createdIndicesNames.clear();
		client.shutdownClient();
		client = null;
	}

	private void tryDeleteESIndex(String indexName) {
		try {
			JestResult result = client.execute( new DeleteIndex.Builder( indexName ).build() );
			if ( !result.isSucceeded() && result.getResponseCode() != 404 /* Index not found is ok */ ) {
				LOG.warnf( "Error while trying to delete index '%s' as part of test cleanup: %s", indexName, result.getErrorMessage() );
			}
		}
		catch (IOException | RuntimeException e) {
			LOG.warnf( e, "Error while trying to delete index '%s' as part of test cleanup", indexName );
		}
	}

}
