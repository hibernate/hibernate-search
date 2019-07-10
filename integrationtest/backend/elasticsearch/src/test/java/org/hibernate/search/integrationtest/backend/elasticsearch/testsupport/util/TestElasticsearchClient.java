/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexStatus;
import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchClientFactoryImpl;
import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchClientUtils;
import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClientFactory;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClientImplementor;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.gson.impl.DefaultGsonProvider;
import org.hibernate.search.backend.elasticsearch.impl.ElasticsearchIndexNameNormalizer;
import org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchRequestFormatter;
import org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchResponseFormatter;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.dialect.ElasticsearchTestDialect;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class TestElasticsearchClient implements TestRule {

	private final ElasticsearchTestDialect dialect = ElasticsearchTestDialect.get();

	private final TestConfigurationProvider configurationProvider = new TestConfigurationProvider();

	private ElasticsearchClientImplementor client;

	private final List<URLEncodedString> createdIndicesNames = new ArrayList<>();

	private final List<String> createdTemplatesNames = new ArrayList<>();

	public ElasticsearchTestDialect getDialect() {
		return dialect;
	}

	public IndexClient index(String indexName) {
		return new IndexClient( URLEncodedString.fromString( ElasticsearchIndexNameNormalizer.normalize( indexName ) ) );
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

		public TypeClient type() {
			return new TypeClient( this );
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

		public TypeClient(IndexClient indexClient) {
			this.indexClient = indexClient;
		}

		public TypeClient putMapping(String mappingJson) {
			TestElasticsearchClient.this.putMapping( indexClient.indexName, mappingJson );
			return this;
		}

		public String getMapping() {
			return TestElasticsearchClient.this.getMapping( indexClient.indexName );
		}

		public TypeClient index(URLEncodedString id, String jsonDocument) {
			URLEncodedString indexName = indexClient.indexName;
			TestElasticsearchClient.this.index( indexName, id, jsonDocument );
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
			return TestElasticsearchClient.this.getDocumentSource( typeClient.indexClient.indexName, id );
		}

		public JsonElement getStoredField(String fieldName) {
			return TestElasticsearchClient.this.getDocumentField( typeClient.indexClient.indexName, id, fieldName );
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

		public TemplateClient create(String templateString, String settings) {
			return create( templateString, 0, settings );
		}

		public TemplateClient create(String templateString, int templateOrder, String settings) {
			return create( templateString, templateOrder, toJsonElement( settings ).getAsJsonObject() );
		}

		public TemplateClient create(String templateString, int templateOrder, JsonObject settings) {
			TestElasticsearchClient.this.createTemplate( templateName, templateString, templateOrder, settings );
			return this;
		}

		public TemplateClient registerForCleanup() {
			TestElasticsearchClient.this.registerTemplateForCleanup( templateName );
			return this;
		}
	}

	private void deleteAndCreateIndex(URLEncodedString indexName) {
		deleteAndCreateIndex( indexName, null );
	}

	private void deleteAndCreateIndex(URLEncodedString indexName, JsonObject settingsAsJsonObject) {
		ElasticsearchRequest.Builder builder = ElasticsearchRequest.put()
				.pathComponent( indexName );

		if ( settingsAsJsonObject != null ) {
			JsonObject payload = new JsonObject();
			payload.add( "settings", settingsAsJsonObject );
			builder.body( payload );
		}

		Boolean includeTypeName = dialect.getIncludeTypeNameParameterForMappingApi();
		if ( includeTypeName != null ) {
			builder.param( "include_type_name", includeTypeName );
		}

		doDeleteAndCreateIndex(
				indexName,
				builder.build()
		);
	}

	private void doDeleteAndCreateIndex(URLEncodedString indexName, ElasticsearchRequest createRequest) {
		// Ignore the result: if the deletion fails, we don't care unless the creation just after also fails
		tryDeleteESIndex( indexName );

		registerIndexForCleanup( indexName );
		performRequest( createRequest );

		waitForRequiredIndexStatus( indexName );
	}

	private void createTemplate(String templateName, String templateString, int templateOrder, JsonObject settings) {
		JsonObject source = new JsonObject();
		dialect.setTemplatePattern( source, templateString );
		source.addProperty( "order", templateOrder );
		source.add( "settings", settings );

		registerTemplateForCleanup( templateName );

		ElasticsearchRequest.Builder builder = ElasticsearchRequest.put()
				.pathComponent( Paths._TEMPLATE ).pathComponent( URLEncodedString.fromString( templateName ) )
				.body( source );

		Boolean includeTypeName = dialect.getIncludeTypeNameParameterForMappingApi();
		if ( includeTypeName != null ) {
			builder.param( "include_type_name", includeTypeName );
		}

		performRequest( builder.build() );
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
				.param( "timeout", ElasticsearchIndexSettings.Defaults.LIFECYCLE_MINIMAL_REQUIRED_STATUS_WAIT_TIMEOUT + "ms" )
				.build() );
	}

	private void putMapping(URLEncodedString indexName, String mappingJson) {
		JsonObject mappingJsonObject = toJsonElement( mappingJson ).getAsJsonObject();

		ElasticsearchRequest.Builder builder = ElasticsearchRequest.put()
				.pathComponent( indexName ).pathComponent( Paths._MAPPING );
		dialect.getTypeNameForMappingApi().ifPresent( builder::pathComponent );
		builder.body( mappingJsonObject );

		Boolean includeTypeName = dialect.getIncludeTypeNameParameterForMappingApi();
		if ( includeTypeName != null ) {
			builder.param( "include_type_name", includeTypeName );
		}

		performRequest( builder.build() );
	}

	private String getMapping(URLEncodedString indexName) {

		ElasticsearchRequest.Builder builder = ElasticsearchRequest.get()
				.pathComponent( indexName ).pathComponent( Paths._MAPPING );
		dialect.getTypeNameForMappingApi().ifPresent( builder::pathComponent );

		Boolean includeTypeName = dialect.getIncludeTypeNameParameterForMappingApi();
		if ( includeTypeName != null ) {
			builder.param( "include_type_name", includeTypeName );
		}

		/*
		 * Elasticsearch 5.5+ triggers a 404 error when mappings are missing,
		 * while 5.4 and below just return an empty mapping.
		 * In our case, an empty mapping is fine, so we'll just ignore 404.
		 */
		ElasticsearchResponse response = performRequestIgnore404( builder.build() );
		JsonObject result = response.getBody();
		JsonElement index = result.get( indexName.original );
		if ( index == null ) {
			return new JsonObject().toString();
		}
		JsonElement mappings = index.getAsJsonObject().get( "mappings" );
		if ( mappings == null ) {
			return new JsonObject().toString();
		}
		Optional<URLEncodedString> typeName = dialect.getTypeNameForMappingApi();
		if ( typeName.isPresent() ) {
			JsonElement mapping = mappings.getAsJsonObject().get( typeName.get().original );
			if ( mapping == null ) {
				return new JsonObject().toString();
			}
			return mapping.toString();
		}
		else {
			return mappings.toString();
		}
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
			JsonObject parent = new JsonObject();
			parent.add( property, settingsJsonElement );
			settingsJsonElement = parent;
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

	private void index(URLEncodedString indexName, URLEncodedString id, String jsonDocument) {
		JsonObject documentJsonObject = toJsonElement( jsonDocument ).getAsJsonObject();
		performRequest( ElasticsearchRequest.put()
				.pathComponent( indexName ).pathComponent( dialect.getTypeKeywordForNonMappingApi() ).pathComponent( id )
				.body( documentJsonObject )
				.param( "refresh", true )
				.build() );
	}

	private JsonObject getDocumentSource(URLEncodedString indexName, URLEncodedString id) {
		ElasticsearchResponse response = performRequest( ElasticsearchRequest.get()
				.pathComponent( indexName ).pathComponent( dialect.getTypeKeywordForNonMappingApi() ).pathComponent( id )
				.build() );
		JsonObject result = response.getBody();
		return result.get( "_source" ).getAsJsonObject();
	}

	protected JsonElement getDocumentField(URLEncodedString indexName, URLEncodedString id, String fieldName) {
		ElasticsearchResponse response = performRequest( ElasticsearchRequest.get()
				.pathComponent( indexName ).pathComponent( dialect.getTypeKeywordForNonMappingApi() ).pathComponent( id )
				.param( "stored_fields", fieldName )
				.build() );
		JsonObject result = response.getBody();
		return result.get( "fields" ).getAsJsonObject().get( fieldName );
	}

	@Override
	public Statement apply(Statement base, Description description) {
		Statement wrapped = new Statement() {
			@Override
			public void evaluate() throws Throwable {
				try ( Closer<IOException> closer = new Closer<>() ) {
					try {
						before();
						base.evaluate();
					}
					finally {
						after( closer );
					}
				}
			}
		};
		return configurationProvider.apply( wrapped, description );
	}

	private void before() {
		ConfigurationPropertySource backendProperties = ConfigurationPropertySource.fromMap(
				configurationProvider.getPropertiesFromFile( ElasticsearchTckBackendHelper.DEFAULT_BACKEND_PROPERTIES_PATH )
		);

		BeanResolver beanResolver = configurationProvider.createBeanResolverForTest();
		/*
		 * We use a {@link ElasticsearchClientFactoryImpl} to create our low-level client.
		 *
		 * The main advantage is that we ensure we connect to Elasticsearch exactly the same way
		 * as any test-created SearchFactory, allowing to support things like testing on AWS
		 * (using the hibernate-search-elasticsearch-aws module).
		 */
		try ( BeanHolder<ElasticsearchClientFactory> factoryHolder =
				beanResolver.resolve( ElasticsearchClientFactoryImpl.REFERENCE ) ) {
			client = factoryHolder.get().create(
					backendProperties, DefaultGsonProvider.create( GsonBuilder::new, true )
			);
		}
	}

	private void after(Closer<IOException> closer) {
		closer.pushAll( this::tryDeleteESIndex, createdIndicesNames );
		createdIndicesNames.clear();
		closer.pushAll( this::tryDeleteESTemplate, createdTemplatesNames );
		createdTemplatesNames.clear();
		closer.push( this::tryCloseClient, client );
		client = null;
	}

	private void tryDeleteESIndex(URLEncodedString indexName) {
		try {
			performRequestIgnore404( ElasticsearchRequest.delete()
					.pathComponent( indexName )
					.build() );
		}
		catch (RuntimeException e) {
			throw new AssertionFailure(
					String.format( Locale.ROOT, "Error while trying to delete index '%s' as part of test cleanup", indexName ),
					e
			);
		}
	}

	private void tryDeleteESTemplate(String templateName) {
		try {
			performRequestIgnore404( ElasticsearchRequest.delete()
					.pathComponent( Paths._TEMPLATE ).pathComponent( URLEncodedString.fromString( templateName ) )
					.build() );
		}
		catch (RuntimeException e) {
			throw new AssertionFailure(
					String.format( Locale.ROOT, "Error while trying to delete template '%s' as part of test cleanup", templateName ),
					e
			);
		}
	}

	private void tryCloseClient(ElasticsearchClientImplementor client) {
		try {
			client.close();
		}
		catch (RuntimeException | IOException e) {
			throw new AssertionFailure(
					"Unexpected exception when closing the ElasticsearchClient used in "
							+ TestElasticsearchClient.class.getSimpleName(),
					e
			);
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
