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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchIndexStatus;
import org.hibernate.search.elasticsearch.impl.DefaultGsonService;
import org.hibernate.search.elasticsearch.impl.ElasticsearchIndexNameNormalizer;
import org.hibernate.search.elasticsearch.impl.JsonBuilder;
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
import io.searchbox.core.Index;
import io.searchbox.indices.CloseIndex;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.DeleteIndex;
import io.searchbox.indices.OpenIndex;
import io.searchbox.indices.mapping.GetMapping;
import io.searchbox.indices.mapping.PutMapping;
import io.searchbox.indices.settings.GetSettings;
import io.searchbox.indices.settings.UpdateSettings;
import io.searchbox.indices.template.DeleteTemplate;
import io.searchbox.indices.template.PutTemplate;

/**
 * @author Yoann Rodiere
 */
public class TestElasticsearchClient extends ExternalResource {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private JestClient client;

	private final List<String> createdIndicesNames = Lists.newArrayList();

	private final List<String> createdTemplatesNames = Lists.newArrayList();

	private ElasticsearchIndexStatus requiredIndexStatus = ElasticsearchEnvironment.Defaults.REQUIRED_INDEX_STATUS;

	public TestElasticsearchClient requiredIndexStatus(ElasticsearchIndexStatus requiredIndexStatus) {
		this.requiredIndexStatus = requiredIndexStatus;
		return this;
	}

	public IndexClient index(Class<?> rootClass) {
		return new IndexClient( ElasticsearchIndexNameNormalizer.getElasticsearchIndexName( rootClass.getName() ) );
	}

	public IndexClient index(String indexName) {
		return new IndexClient( indexName );
	}

	public MappingClient mapping(Class<?> rootClass) {
		return index( rootClass ).mapping( rootClass );
	}

	public class IndexClient {

		private final String indexName;

		public IndexClient(String indexName) {
			this.indexName = indexName;
		}

		public IndexClient deleteAndCreate() throws IOException {
			TestElasticsearchClient.this.deleteAndCreateIndex( indexName );
			return this;
		}

		public IndexClient ensureDoesNotExist() throws IOException {
			TestElasticsearchClient.this.ensureIndexDoesNotExist( indexName );
			return this;
		}

		public IndexClient registerForCleanup() {
			TestElasticsearchClient.this.registerIndexForCleanup( indexName );
			return this;
		}

		public MappingClient mapping(Class<?> mappingClass) {
			return mapping( mappingClass.getName() );
		}

		public MappingClient mapping(String mappingName) {
			return new MappingClient( this, mappingName );
		}

		public SettingsClient settings() {
			return settings( "" );
		}

		public SettingsClient settings(String settingsPath) {
			return new SettingsClient( this, settingsPath );
		}
	}

	public class MappingClient {

		private final IndexClient indexClient;

		private final String mappingName;

		public MappingClient(IndexClient indexClient, String mappingName) {
			this.indexClient = indexClient;
			this.mappingName = mappingName;
		}

		public MappingClient put(String mappingJson) throws IOException {
			TestElasticsearchClient.this.putMapping( indexClient.indexName, mappingName, mappingJson );
			return this;
		}

		public String get() throws IOException {
			return TestElasticsearchClient.this.getMapping( indexClient.indexName, mappingName );
		}

		public MappingClient index(String id, String jsonDocument) throws IOException {
			TestElasticsearchClient.this.index( indexClient.indexName, mappingName, id, jsonDocument );
			return this;
		}
	}

	public class SettingsClient {

		private final IndexClient indexClient;

		private final String settingsPath;

		public SettingsClient(IndexClient indexClient, String settingsPath) {
			this.indexClient = indexClient;
			this.settingsPath = settingsPath;
		}

		public String get() throws IOException {
			return TestElasticsearchClient.this.getSettings( indexClient.indexName, settingsPath );
		}

		public void put(String settings) throws IOException {
			TestElasticsearchClient.this.putSettings( indexClient.indexName, settingsPath, settings );
		}
	}

	public TemplateClient template(String templateName) {
		return new TemplateClient( templateName );
	}

	public class TemplateClient {

		private final String templateName;

		public TemplateClient(String templateName) {
			this.templateName = templateName;
		}

		public TemplateClient create(String templateString, JsonObject settings) throws IOException {
			TestElasticsearchClient.this.createTemplate( templateName, templateString, settings );
			return this;
		}

		public TemplateClient registerForCleanup() {
			TestElasticsearchClient.this.registerTemplateForCleanup( templateName );
			return this;
		}
	}

	private void deleteAndCreateIndex(String indexName) throws IOException {
		// Ignore the result: if the deletion fails, we don't care unless the creation just after also fails
		tryDeleteESIndex( indexName );

		JestResult result = client.execute( new CreateIndex.Builder( indexName ).build() );
		registerIndexForCleanup( indexName );
		if ( !result.isSucceeded() ) {
			throw new AssertionFailure( "Error while creating index '" + indexName + "' for tests:" + result.getErrorMessage() );
		}

		waitForIndexCreation( indexName );
	}

	private void createTemplate(String templateName, String templateString, JsonObject settings) throws IOException {
		JsonObject source = JsonBuilder.object()
				.addProperty( "template", templateString )
				.add( "settings", settings )
				.build();
		JestResult result = client.execute( new PutTemplate.Builder( templateName, source ).build() );
		registerTemplateForCleanup( templateName );
		if ( !result.isSucceeded() ) {
			throw new AssertionFailure( "Error while creating template '" + templateName + "' for tests:" + result.getErrorMessage() );
		}
	}

	private void ensureIndexDoesNotExist(String indexName) throws IOException {
		JestResult result = client.execute( new DeleteIndex.Builder( indexName ).build() );
		if ( !result.isSucceeded() && result.getResponseCode() != 404 /* Index not found is ok */ ) {
			throw new AssertionFailure( String.format(
					Locale.ENGLISH,
					"Error while trying to delete index '%s' as part of test initialization: %s",
					indexName, result.getErrorMessage()
					) );
		}
	}

	private void registerIndexForCleanup(String indexName) {
		createdIndicesNames.add( indexName );
	}

	private void registerTemplateForCleanup(String templateName) {
		createdTemplatesNames.add( templateName );
	}

	private void waitForIndexCreation(final String indexNameToWaitFor) throws IOException {
		Builder healthBuilder = new Health.Builder()
				.setParameter( "wait_for_status", requiredIndexStatus.getElasticsearchString() )
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

	private void putMapping(String indexName, String mappingName, String mappingJson) throws IOException {
		JsonElement mappingJsonElement = toJsonElement( mappingJson );

		JestResult result = client.execute( new PutMapping.Builder( indexName, mappingName, mappingJsonElement ).build() );
		if ( !result.isSucceeded() ) {
			throw new AssertionFailure( "Error while putting mapping '" + mappingName
					+ "' on index '" + indexName + "' for tests:" + result.getErrorMessage() );
		}
	}

	private String getMapping(String indexName, String mappingName) throws IOException {
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

	private void putSettings(String indexName, String settingsPath, String settings) throws IOException {
		JsonElement settingsJsonElement = toJsonElement( settings );

		for ( String property : Lists.reverse( Arrays.asList( settingsPath.split( "\\." ) ) ) ) {
			settingsJsonElement = JsonBuilder.object().add( property, settingsJsonElement ).build();
		}

		JestResult result = client.execute( new CloseIndex.Builder( indexName ).build() );
		if ( !result.isSucceeded() ) {
			throw new AssertionFailure( "Error while closing index '" + indexName
					+ "' for tests:" + result.getErrorMessage() );
		}
		result = client.execute( new UpdateSettings.Builder( settingsJsonElement ).addIndex( indexName ).build() );
		if ( !result.isSucceeded() ) {
			throw new AssertionFailure( "Error while putting settings on index '" + indexName
					+ "' for tests:" + result.getErrorMessage() );
		}
		result = client.execute( new OpenIndex.Builder( indexName ).build() );
		if ( !result.isSucceeded() ) {
			throw new AssertionFailure( "Error while re-opening index '" + indexName
					+ "' for tests:" + result.getErrorMessage() );
		}
	}

	private String getSettings(String indexName, String path) throws IOException {
		JestResult result = client.execute( new GetSettings.Builder().addIndex( indexName ).build() );
		if ( !result.isSucceeded() ) {
			throw new AssertionFailure( "Error while getting settings on index '" + indexName +
					"' for tests:" + result.getErrorMessage() );
		}
		JsonElement index = result.getJsonObject().get( indexName );
		if ( index == null ) {
			return new JsonObject().toString();
		}
		JsonElement settings = index.getAsJsonObject().get( "settings" );
		for ( String property : path.split( "\\." ) ) {
			if ( settings == null ) {
				break;
			}
			settings = settings.getAsJsonObject().get( property );
		}
		if ( settings == null ) {
			return new JsonObject().toString();
		}
		return settings.toString();
	}

	private void index(String indexName, String typeName, String id, String jsonDocument) throws IOException {
		JsonElement documentJsonElement = toJsonElement( jsonDocument );

		JestResult result = client.execute( new Index.Builder( documentJsonElement )
				.index( indexName )
				.type( typeName )
				.id( id )
				.refresh( true )
				.build() );
		if ( !result.isSucceeded() ) {
			throw new AssertionFailure( "Error while indexing '" + jsonDocument
					+ "' on index '" + indexName + "' for tests:" + result.getErrorMessage() );
		}
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
		for ( String templateName : createdTemplatesNames ) {
			tryDeleteESTemplate( templateName );
		}
		createdTemplatesNames.clear();
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

	private void tryDeleteESTemplate(String templateName) {
		try {
			JestResult result = client.execute( new DeleteTemplate.Builder( templateName ).build() );
			if ( !result.isSucceeded() && result.getResponseCode() != 404 /* Index not found is ok */ ) {
				LOG.warnf( "Error while trying to delete template '%s' as part of test cleanup: %s", templateName, result.getErrorMessage() );
			}
		}
		catch (IOException | RuntimeException e) {
			LOG.warnf( e, "Error while trying to delete template '%s' as part of test cleanup", templateName );
		}
	}

	/*
	 * Convert provided JSON to JsonElement, so that some Elasticsearch peculiarities (such as the fact that
	 * single quotes are not accepted as a substitute for single quotes) can be worked around.
	 * In tests, single quotes are way easier to include in JSON strings, because we don't have to escape them.
	 */
	private JsonElement toJsonElement(String jsonAsString) {
		return new JsonParser().parse( jsonAsString );
	}

}
