/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.testutil;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchIndexStatus;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.elasticsearch.client.impl.Paths;
import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.dialect.impl.DialectIndependentGsonProvider;
import org.hibernate.search.elasticsearch.impl.ElasticsearchIndexNameNormalizer;
import org.hibernate.search.elasticsearch.impl.JsonBuilder;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.util.impl.ElasticsearchClientUtils;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.testsupport.setup.TestDefaults;
import org.hibernate.search.util.impl.SearchThreadFactory;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.junit.rules.ExternalResource;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * @author Yoann Rodiere
 */
public class TestElasticsearchClient extends ExternalResource {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private RestClient client;

	private Gson gson;

	private final List<URLEncodedString> createdIndicesNames = Lists.newArrayList();

	private final List<String> createdTemplatesNames = Lists.newArrayList();

	public IndexClient index(Class<?> rootClass) {
		return new IndexClient( ElasticsearchIndexNameNormalizer.getElasticsearchIndexName( rootClass.getName() ) );
	}

	public IndexClient index(String indexName) {
		return new IndexClient( URLEncodedString.fromString( indexName ) );
	}

	public TypeClient type(Class<?> rootClass) {
		return index( rootClass ).type( rootClass );
	}

	public class IndexClient {

		private final URLEncodedString indexName;

		public IndexClient(URLEncodedString indexName) {
			this.indexName = indexName;
		}

		public void waitForRequiredIndexStatus() throws IOException {
			TestElasticsearchClient.this.waitForRequiredIndexStatus( indexName );
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

		public TypeClient type(Class<?> mappingClass) {
			return type( mappingClass.getName() );
		}

		public TypeClient type(String mappingName) {
			return new TypeClient( this, mappingName );
		}

		public SettingsClient settings() {
			return settings( "" );
		}

		public SettingsClient settings(String settingsPath) {
			return new SettingsClient( this, settingsPath );
		}
	}

	public class TypeClient {

		private final IndexClient indexClient;

		private final URLEncodedString typeName;

		public TypeClient(IndexClient indexClient, String mappingName) {
			this.indexClient = indexClient;
			this.typeName = URLEncodedString.fromString( mappingName );
		}

		public TypeClient putMapping(String mappingJson) throws IOException {
			TestElasticsearchClient.this.putMapping( indexClient.indexName, typeName, mappingJson );
			return this;
		}

		public String getMapping() throws IOException {
			return TestElasticsearchClient.this.getMapping( indexClient.indexName, typeName );
		}

		public TypeClient index(URLEncodedString id, String jsonDocument) throws IOException {
			URLEncodedString indexName = indexClient.indexName;
			TestElasticsearchClient.this.index( indexName, typeName, id, jsonDocument );
			return this;
		}

		public DocumentClient document(String id) {
			return new DocumentClient( this, id );
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
			URLEncodedString indexName = indexClient.indexName;
			return TestElasticsearchClient.this.getSettings( indexName, settingsPath );
		}

		public void put(String settings) throws IOException {
			URLEncodedString indexName = indexClient.indexName;
			TestElasticsearchClient.this.putSettings( indexName, settingsPath, settings );
		}
	}

	public class DocumentClient {

		private final TypeClient typeClient;

		private final URLEncodedString id;

		public DocumentClient(TypeClient typeClient, String id) {
			this.typeClient = typeClient;
			this.id = URLEncodedString.fromString( id );
		}

		public JsonObject getSource() throws IOException {
			return TestElasticsearchClient.this.getDocumentSource( typeClient.indexClient.indexName, typeClient.typeName, id );
		}

		public JsonElement getStoredField(String fieldName) throws IOException {
			return TestElasticsearchClient.this.getDocumentField( typeClient.indexClient.indexName, typeClient.typeName, id, fieldName );
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

	private void deleteAndCreateIndex(URLEncodedString indexName) throws IOException {
		// Ignore the result: if the deletion fails, we don't care unless the creation just after also fails
		tryDeleteESIndex( indexName );

		registerIndexForCleanup( indexName );
		performRequest( ElasticsearchRequest.put().pathComponent( indexName ).build() );

		waitForRequiredIndexStatus( indexName );
	}

	private void createTemplate(String templateName, String templateString, JsonObject settings) throws IOException {
		JsonObject source = JsonBuilder.object()
				.addProperty( "template", templateString )
				.add( "settings", settings )
				.build();

		registerTemplateForCleanup( templateName );
		performRequest( ElasticsearchRequest.put()
				.pathComponent( Paths._TEMPLATE ).pathComponent( URLEncodedString.fromString( templateName ) )
				.body( source )
				.build() );
	}

	private void ensureIndexDoesNotExist(URLEncodedString indexName) throws IOException {
		try {
			performRequest( ElasticsearchRequest.delete()
					.pathComponent( indexName )
					.build() );
		}
		catch (ResponseException e) {
			if ( e.getResponse().getStatusLine().getStatusCode() != 404 /* Index not found is ok */ ) {
				throw e;
			}
		}
	}

	private void registerIndexForCleanup(URLEncodedString indexName) {
		createdIndicesNames.add( indexName );
	}

	private void registerTemplateForCleanup(String templateName) {
		createdTemplatesNames.add( templateName );
	}

	private void waitForRequiredIndexStatus(final URLEncodedString indexName) throws IOException {
		performRequest( ElasticsearchRequest.get()
				.pathComponent( Paths._CLUSTER ).pathComponent( Paths.HEALTH ).pathComponent( indexName )
				/*
				 * We only wait for YELLOW: it's perfectly fine, and some tests actually expect
				 * the indexes to never reach a green status
				 */
				.param( "wait_for_status", ElasticsearchIndexStatus.YELLOW.getElasticsearchString() )
				.param( "timeout", ElasticsearchEnvironment.Defaults.INDEX_MANAGEMENT_WAIT_TIMEOUT + "ms" )
				.build() );
	}

	private void putMapping(URLEncodedString indexName, URLEncodedString mappingName, String mappingJson) throws IOException {
		JsonObject mappingJsonObject = toJsonElement( mappingJson ).getAsJsonObject();

		performRequest( ElasticsearchRequest.put()
				.pathComponent( indexName ).pathComponent( Paths._MAPPING ).pathComponent( mappingName )
				.body( mappingJsonObject )
				.build() );
	}

	private String getMapping(URLEncodedString indexName, URLEncodedString mappingName) throws IOException {
		Response response = performRequest( ElasticsearchRequest.get()
				.pathComponent( indexName ).pathComponent( Paths._MAPPING ).pathComponent( mappingName )
				.build() );
		JsonObject result = toJsonObject( response );
		JsonElement index = result.get( indexName.original );
		if ( index == null ) {
			return new JsonObject().toString();
		}
		JsonElement mappings = index.getAsJsonObject().get( "mappings" );
		if ( mappings == null ) {
			return new JsonObject().toString();
		}
		JsonElement mapping = mappings.getAsJsonObject().get( mappingName.original );
		if ( mapping == null ) {
			return new JsonObject().toString();
		}
		return mapping.toString();
	}

	private void putSettings(URLEncodedString indexName, String settingsPath, String settings) throws IOException {
		JsonElement settingsJsonElement = toJsonElement( settings );

		for ( String property : Lists.reverse( Arrays.asList( settingsPath.split( "\\." ) ) ) ) {
			settingsJsonElement = JsonBuilder.object().add( property, settingsJsonElement ).build();
		}

		performRequest( ElasticsearchRequest.post()
				.pathComponent( indexName )
				.pathComponent( Paths._CLOSE )
				.build() );

		performRequest( ElasticsearchRequest.put()
				.pathComponent( indexName ).pathComponent( Paths._SETTINGS )
				.body( settingsJsonElement.getAsJsonObject() )
				.build() );

		performRequest( ElasticsearchRequest.post()
				.pathComponent( indexName )
				.pathComponent( Paths._OPEN )
				.build() );
	}

	private String getSettings(URLEncodedString indexName, String path) throws IOException {
		Response response = performRequest( ElasticsearchRequest.get()
				.pathComponent( indexName ).pathComponent( Paths._SETTINGS )
				.build() );
		JsonObject result = toJsonObject( response );
		JsonElement index = result.get( indexName.original );
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

	private void index(URLEncodedString indexName, URLEncodedString typeName, URLEncodedString id, String jsonDocument) throws IOException {
		JsonObject documentJsonObject = toJsonElement( jsonDocument ).getAsJsonObject();
		performRequest( ElasticsearchRequest.put()
				.pathComponent( indexName ).pathComponent( typeName ).pathComponent( id )
				.body( documentJsonObject )
				.param( "refresh", true )
				.build() );
	}

	private JsonObject getDocumentSource(URLEncodedString indexName, URLEncodedString typeName, URLEncodedString id) throws IOException {
		Response response = performRequest( ElasticsearchRequest.get()
				.pathComponent( indexName ).pathComponent( typeName ).pathComponent( id )
				.build() );
		JsonObject result = toJsonObject( response );
		return result.get( "_source" ).getAsJsonObject();
	}

	protected JsonElement getDocumentField(URLEncodedString indexName, URLEncodedString typeName, URLEncodedString id, String fieldName) throws IOException {
		Response response = performRequest( ElasticsearchRequest.get()
				.pathComponent( indexName ).pathComponent( typeName ).pathComponent( id )
				.param( "stored_fields", fieldName )
				.build() );
		JsonObject result = toJsonObject( response );
		return result.get( "fields" ).getAsJsonObject().get( fieldName );
	}

	@Override
	protected void before() throws Throwable {
		gson = DialectIndependentGsonProvider.INSTANCE.getGson();

		Properties properties = TestDefaults.getProperties();
		String username = properties.getProperty( "hibernate.search.default." + ElasticsearchEnvironment.SERVER_USERNAME );
		String password = properties.getProperty( "hibernate.search.default." + ElasticsearchEnvironment.SERVER_PASSWORD );

		this.client = RestClient.builder( HttpHost.create( ElasticsearchEnvironment.Defaults.SERVER_URI ) )
				/*
				 * Note: this timeout is not only used on retries,
				 * but also when executing requests synchronously.
				 * See https://github.com/elastic/elasticsearch/issues/21789#issuecomment-287399115
				 */
				.setMaxRetryTimeoutMillis( ElasticsearchEnvironment.Defaults.SERVER_REQUEST_TIMEOUT )
				.setHttpClientConfigCallback( (builder) -> {
					if ( username != null ) {
						BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
						credentialsProvider.setCredentials(
								new AuthScope( AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM, AuthScope.ANY_SCHEME ),
								new UsernamePasswordCredentials( username, password )
								);

						builder = builder.setDefaultCredentialsProvider( credentialsProvider );
					}
					return builder
							.setMaxConnTotal( ElasticsearchEnvironment.Defaults.MAX_TOTAL_CONNECTION )
							.setMaxConnPerRoute( ElasticsearchEnvironment.Defaults.MAX_TOTAL_CONNECTION_PER_ROUTE )
							.setThreadFactory( new SearchThreadFactory( "Test Elasticsearch client transport thread" ) );
				} )
				.setRequestConfigCallback( (builder) -> {
					return builder
							.setSocketTimeout( ElasticsearchEnvironment.Defaults.SERVER_READ_TIMEOUT )
							.setConnectTimeout( ElasticsearchEnvironment.Defaults.SERVER_CONNECTION_TIMEOUT );
				} )
				.build();
	}

	@Override
	protected void after() {
		for ( URLEncodedString indexName : createdIndicesNames ) {
			tryDeleteESIndex( indexName );
		}
		createdIndicesNames.clear();
		for ( String templateName : createdTemplatesNames ) {
			tryDeleteESTemplate( templateName );
		}
		createdTemplatesNames.clear();

		try {
			client.close();
			client = null;
		}
		catch (IOException e) {
			throw new AssertionFailure( "Unexpected exception when closing the RestClient", e );
		}
	}

	private void tryDeleteESIndex(URLEncodedString indexName) {
		try {
			performRequest( ElasticsearchRequest.delete()
					.pathComponent( indexName )
					.build() );
		}
		catch (ResponseException e) {
			if ( e.getResponse().getStatusLine().getStatusCode() != 404 /* Index not found is ok */ ) {
				LOG.warnf( e, "Error while trying to delete index '%s' as part of test cleanup", indexName );
			}
		}
		catch (IOException | RuntimeException e) {
			LOG.warnf( e, "Error while trying to delete index '%s' as part of test cleanup", indexName );
		}
	}

	private void tryDeleteESTemplate(String templateName) {
		try {
			performRequest( ElasticsearchRequest.delete()
					.pathComponent( Paths._TEMPLATE ).pathComponent( URLEncodedString.fromString( templateName ) )
					.build() );
		}
		catch (ResponseException e) {
			if ( e.getResponse().getStatusLine().getStatusCode() != 404 /* Template not found is ok */ ) {
				LOG.warnf( e, "Error while trying to delete template '%s' as part of test cleanup", templateName );
			}
		}
		catch (IOException | RuntimeException e) {
			LOG.warnf( e, "Error while trying to delete template '%s' as part of test cleanup", templateName );
		}
	}

	protected Response performRequest(ElasticsearchRequest request) throws IOException {
		return client.performRequest(
				request.getMethod(), request.getPath(), request.getParameters(),
				ElasticsearchClientUtils.toEntity( gson, request )
				);
	}

	protected JsonObject toJsonObject(Response response) throws IOException {
		HttpEntity entity = response.getEntity();
		if ( entity == null ) {
			return null;
		}

		ContentType contentType = ContentType.get( entity );
		try ( InputStream inputStream = entity.getContent();
				Reader reader = new InputStreamReader( inputStream, contentType.getCharset() ) ) {
			return gson.fromJson( reader, JsonObject.class );
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
