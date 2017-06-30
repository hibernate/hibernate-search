/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.testutil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchIndexStatus;
import org.hibernate.search.elasticsearch.client.impl.DefaultElasticsearchClientFactory;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchClient;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchResponse;
import org.hibernate.search.elasticsearch.client.impl.Paths;
import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.impl.ElasticsearchIndexNameNormalizer;
import org.hibernate.search.elasticsearch.impl.JsonBuilder;
import org.hibernate.search.elasticsearch.logging.impl.ElasticsearchRequestFormatter;
import org.hibernate.search.elasticsearch.logging.impl.ElasticsearchResponseFormatter;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.util.impl.ElasticsearchClientUtils;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.testsupport.setup.BuildContextForTest;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.hibernate.search.util.configuration.impl.MaskedProperty;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.junit.rules.ExternalResource;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * @author Yoann Rodiere
 */
public class TestElasticsearchClient extends ExternalResource {

	private static final Log LOG = LoggerFactory.make( Log.class );

	/**
	 * We use a {@link DefaultElasticsearchClientFactory} to create our low-level client.
	 *
	 * The main advantage is that we ensure we connect to Elasticsearch exactly the same way
	 * as any test-created SearchFactory, allowing to support things like testing on AWS
	 * (using the hibernate-search-elasticsearch-aws module).
	 */
	private final DefaultElasticsearchClientFactory clientFactory = new DefaultElasticsearchClientFactory();

	private ElasticsearchClient client;

	private final List<URLEncodedString> createdIndicesNames = new ArrayList<>();

	private final List<String> createdTemplatesNames = new ArrayList<>();

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

		public void waitForRequiredIndexStatus() {
			TestElasticsearchClient.this.waitForRequiredIndexStatus( indexName );
		}

		public IndexClient deleteAndCreate() {
			TestElasticsearchClient.this.deleteAndCreateIndex( indexName );
			return this;
		}

		public IndexClient deleteAndCreate(String settingsPath, String settings) {
			JsonObject settingsAsJsonObject = buildSettings( settingsPath, settings );
			TestElasticsearchClient.this.deleteAndCreateIndex( indexName, settingsAsJsonObject );
			return this;
		}

		public IndexClient ensureDoesNotExist() {
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

		public TypeClient putMapping(String mappingJson) {
			TestElasticsearchClient.this.putMapping( indexClient.indexName, typeName, mappingJson );
			return this;
		}

		public String getMapping() {
			return TestElasticsearchClient.this.getMapping( indexClient.indexName, typeName );
		}

		public TypeClient index(URLEncodedString id, String jsonDocument) {
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

		public String get() {
			URLEncodedString indexName = indexClient.indexName;
			return TestElasticsearchClient.this.getSettings( indexName, settingsPath );
		}

		/**
		 * Put settings without closing the index first.
		 *
		 * @param settings The settings value to put
		 * @throws IOException
		 */
		public void putDynamic(String settings) {
			URLEncodedString indexName = indexClient.indexName;
			JsonObject settingsAsJsonObject = buildSettings( settingsPath, settings );
			TestElasticsearchClient.this.putDynamicSettings( indexName, settingsAsJsonObject );
		}

		/**
		 * Put settings, closing the index first and reopening the index afterwards.
		 *
		 * @param settings The settings value to put
		 * @throws IOException
		 */
		public void putNonDynamic(String settings) {
			URLEncodedString indexName = indexClient.indexName;
			JsonObject settingsAsJsonObject = buildSettings( settingsPath, settings );
			TestElasticsearchClient.this.putNonDynamicSettings( indexName, settingsAsJsonObject );
		}
	}

	public class DocumentClient {

		private final TypeClient typeClient;

		private final URLEncodedString id;

		public DocumentClient(TypeClient typeClient, String id) {
			this.typeClient = typeClient;
			this.id = URLEncodedString.fromString( id );
		}

		public JsonObject getSource() {
			return TestElasticsearchClient.this.getDocumentSource( typeClient.indexClient.indexName, typeClient.typeName, id );
		}

		public JsonElement getStoredField(String fieldName) {
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

		public TemplateClient create(String templateString, JsonObject settings) {
			TestElasticsearchClient.this.createTemplate( templateName, templateString, settings );
			return this;
		}

		public TemplateClient registerForCleanup() {
			TestElasticsearchClient.this.registerTemplateForCleanup( templateName );
			return this;
		}
	}

	private void deleteAndCreateIndex(URLEncodedString indexName) {
		doDeleteAndCreateIndex(
				indexName,
				ElasticsearchRequest.put().pathComponent( indexName ).build()
				);
	}

	private void deleteAndCreateIndex(URLEncodedString indexName, JsonObject settingsAsJsonObject) {
		doDeleteAndCreateIndex(
				indexName,
				ElasticsearchRequest.put().pathComponent( indexName ).body( settingsAsJsonObject ).build()
				);
	}

	private void doDeleteAndCreateIndex(URLEncodedString indexName, ElasticsearchRequest createRequest) {
		// Ignore the result: if the deletion fails, we don't care unless the creation just after also fails
		tryDeleteESIndex( indexName );

		registerIndexForCleanup( indexName );
		performRequest( createRequest );

		waitForRequiredIndexStatus( indexName );
	}

	private void createTemplate(String templateName, String templateString, JsonObject settings) {
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

	private void ensureIndexDoesNotExist(URLEncodedString indexName) {
		performRequestIgnore404( ElasticsearchRequest.delete()
				.pathComponent( indexName )
				.build() );
	}

	private void registerIndexForCleanup(URLEncodedString indexName) {
		createdIndicesNames.add( indexName );
	}

	private void registerTemplateForCleanup(String templateName) {
		createdTemplatesNames.add( templateName );
	}

	private void waitForRequiredIndexStatus(final URLEncodedString indexName) {
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

	private void putMapping(URLEncodedString indexName, URLEncodedString mappingName, String mappingJson) {
		JsonObject mappingJsonObject = toJsonElement( mappingJson ).getAsJsonObject();

		performRequest( ElasticsearchRequest.put()
				.pathComponent( indexName ).pathComponent( Paths._MAPPING ).pathComponent( mappingName )
				.body( mappingJsonObject )
				.build() );
	}

	private String getMapping(URLEncodedString indexName, URLEncodedString mappingName) {
		/*
		 * Elasticsearch 5.5+ triggers a 404 error when mappings are missing,
		 * while 5.4 and below just return an empty mapping.
		 * In our case, an empty mapping is fine, so we'll just ignore 404.
		 */
		ElasticsearchResponse response = performRequestIgnore404( ElasticsearchRequest.get()
				.pathComponent( indexName ).pathComponent( Paths._MAPPING ).pathComponent( mappingName )
				.build() );
		JsonObject result = response.getBody();
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

	private void putDynamicSettings(URLEncodedString indexName, JsonObject settingsJsonObject) {
		performRequest( ElasticsearchRequest.put()
				.pathComponent( indexName ).pathComponent( Paths._SETTINGS )
				.body( settingsJsonObject )
				.build() );
	}

	private void putNonDynamicSettings(URLEncodedString indexName, JsonObject settingsJsonObject) {
		performRequest( ElasticsearchRequest.post()
				.pathComponent( indexName )
				.pathComponent( Paths._CLOSE )
				.build() );

		performRequest( ElasticsearchRequest.put()
				.pathComponent( indexName ).pathComponent( Paths._SETTINGS )
				.body( settingsJsonObject )
				.build() );

		performRequest( ElasticsearchRequest.post()
				.pathComponent( indexName )
				.pathComponent( Paths._OPEN )
				.build() );
	}

	private JsonObject buildSettings(String settingsPath, String settings) {
		JsonElement settingsJsonElement = toJsonElement( settings );

		List<String> components = Arrays.asList( settingsPath.split( "\\." ) );
		Collections.reverse( components );
		for ( String property : components ) {
			settingsJsonElement = JsonBuilder.object().add( property, settingsJsonElement ).build();
		}

		return settingsJsonElement.getAsJsonObject();
	}

	private String getSettings(URLEncodedString indexName, String path) {
		ElasticsearchResponse response = performRequest( ElasticsearchRequest.get()
				.pathComponent( indexName ).pathComponent( Paths._SETTINGS )
				.build() );
		JsonObject result = response.getBody();
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

	private void index(URLEncodedString indexName, URLEncodedString typeName, URLEncodedString id, String jsonDocument) {
		JsonObject documentJsonObject = toJsonElement( jsonDocument ).getAsJsonObject();
		performRequest( ElasticsearchRequest.put()
				.pathComponent( indexName ).pathComponent( typeName ).pathComponent( id )
				.body( documentJsonObject )
				.param( "refresh", true )
				.build() );
	}

	private JsonObject getDocumentSource(URLEncodedString indexName, URLEncodedString typeName, URLEncodedString id) {
		ElasticsearchResponse response = performRequest( ElasticsearchRequest.get()
				.pathComponent( indexName ).pathComponent( typeName ).pathComponent( id )
				.build() );
		JsonObject result = response.getBody();
		return result.get( "_source" ).getAsJsonObject();
	}

	protected JsonElement getDocumentField(URLEncodedString indexName, URLEncodedString typeName, URLEncodedString id, String fieldName) {
		ElasticsearchResponse response = performRequest( ElasticsearchRequest.get()
				.pathComponent( indexName ).pathComponent( typeName ).pathComponent( id )
				.param( "stored_fields", fieldName )
				.build() );
		JsonObject result = response.getBody();
		return result.get( "fields" ).getAsJsonObject().get( fieldName );
	}

	@Override
	protected void before() throws Throwable {
		SearchConfiguration configuration = new SearchConfigurationForTest(); // Includes default test properties
		Properties unkmaskedProperties = configuration.getProperties();
		clientFactory.start( unkmaskedProperties, new BuildContextForTest( configuration ) );
		Properties rootCfg = new MaskedProperty( unkmaskedProperties, "hibernate.search" );
		// Use root as a fallback to support query options in particular
		Properties properties = new MaskedProperty( rootCfg, "default", rootCfg );
		client = clientFactory.create( properties );
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
			throw new AssertionFailure( "Unexpected exception when closing the ElasticsearchClient", e );
		}
	}

	private void tryDeleteESIndex(URLEncodedString indexName) {
		try {
			performRequestIgnore404( ElasticsearchRequest.delete()
					.pathComponent( indexName )
					.build() );
		}
		catch (RuntimeException e) {
			LOG.warnf( e, "Error while trying to delete index '%s' as part of test cleanup", indexName );
		}
	}

	private void tryDeleteESTemplate(String templateName) {
		try {
			performRequestIgnore404( ElasticsearchRequest.delete()
					.pathComponent( Paths._TEMPLATE ).pathComponent( URLEncodedString.fromString( templateName ) )
					.build() );
		}
		catch (RuntimeException e) {
			LOG.warnf( e, "Error while trying to delete template '%s' as part of test cleanup", templateName );
		}
	}

	protected ElasticsearchResponse performRequest(ElasticsearchRequest request) {
		ElasticsearchResponse response;
		try {
			response = client.submit( request ).join();
		}
		catch (Exception e) {
			throw requestFailed( request, e );
		}
		int statusCode = response.getStatusCode();
		if ( !ElasticsearchClientUtils.isSuccessCode( statusCode ) ) {
			throw requestFailed( request, response );
		}
		return response;
	}

	protected ElasticsearchResponse performRequestIgnore404(ElasticsearchRequest request) {
		ElasticsearchResponse response;
		try {
			response = client.submit( request ).join();
		}
		catch (Exception e) {
			throw requestFailed( request, e );
		}
		int statusCode = response.getStatusCode();
		if ( !ElasticsearchClientUtils.isSuccessCode( statusCode ) && 404 != statusCode ) {
			throw requestFailed( request, response );
		}
		return response;
	}

	private AssertionFailure requestFailed(ElasticsearchRequest request, Exception e) {
		return new AssertionFailure( "Elasticsearch request in TestElasticsearchClient failed:"
				+ "Request:\n"
				+ "========\n"
				+ new ElasticsearchRequestFormatter( request ),
				e );
	}

	private AssertionFailure requestFailed(ElasticsearchRequest request, ElasticsearchResponse response) {
		return new AssertionFailure( "Elasticsearch request in TestElasticsearchClient failed:\n"
				+ "Request:\n"
				+ "========\n"
				+ new ElasticsearchRequestFormatter( request )
				+ "\nResponse:\n"
				+ "========\n"
				+ new ElasticsearchResponseFormatter( response )
				);
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
