/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule;

import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.aliasDefinitions;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultPrimaryName;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultReadAlias;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultWriteAlias;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchClientFactoryImpl;
import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchClientUtils;
import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClientImplementor;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.index.IndexStatus;
import org.hibernate.search.backend.elasticsearch.index.layout.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchRequestFormatter;
import org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchResponseFormatter;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.AllAwareConfigurationPropertySource;
import org.hibernate.search.engine.common.execution.spi.DelegatingSimpleScheduledExecutor;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.thread.impl.EmbeddedThreadProvider;
import org.hibernate.search.engine.environment.thread.impl.ThreadPoolProviderImpl;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchTestHostConnectionConfiguration;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class TestElasticsearchClient implements TestRule, Closeable {

	private final ElasticsearchTestDialect dialect = ElasticsearchTestDialect.get();

	private ThreadPoolProviderImpl threadPoolProvider;
	private ScheduledExecutorService timeoutExecutorService;
	private ElasticsearchClientImplementor client;

	private final List<URLEncodedString> createdIndicesNames = new ArrayList<>();

	public ElasticsearchTestDialect getDialect() {
		return dialect;
	}

	public IndexClient index(String hibernateSearchIndexName) {
		return index(
				defaultPrimaryName( hibernateSearchIndexName ),
				defaultWriteAlias( hibernateSearchIndexName ),
				defaultReadAlias( hibernateSearchIndexName )
		);
	}

	public IndexClient indexNoAlias(String hibernateSearchIndexName) {
		return index( IndexNames.encodeName( hibernateSearchIndexName ), null, null );
	}

	public IndexClient index(URLEncodedString primaryIndexName, URLEncodedString writeAlias, URLEncodedString readAlias) {
		return new IndexClient( primaryIndexName, writeAlias, readAlias );
	}

	public class IndexClient {

		private final URLEncodedString primaryIndexName;
		private final URLEncodedString writeAlias;
		private final URLEncodedString readAlias;

		public IndexClient(URLEncodedString primaryIndexName, URLEncodedString writeAlias, URLEncodedString readAlias) {
			this.primaryIndexName = primaryIndexName;
			this.writeAlias = writeAlias;
			this.readAlias = readAlias;
		}

		public boolean exists() {
			return TestElasticsearchClient.this.exists( primaryIndexName );
		}

		public IndexClient deleteAndCreate() {
			TestElasticsearchClient.this.deleteAndCreateIndex( primaryIndexName, writeAlias, readAlias );
			return this;
		}

		public IndexClient deleteAndCreate(String settingsPath, String settings) {
			JsonObject settingsAsJsonObject = buildStructuredSettings( settingsPath, settings );
			TestElasticsearchClient.this.deleteAndCreateIndex( primaryIndexName, writeAlias, readAlias, settingsAsJsonObject );
			return this;
		}

		public IndexClient delete() {
			TestElasticsearchClient.this.deleteIndex( primaryIndexName );
			return this;
		}

		public IndexClient ensureDoesNotExist() {
			TestElasticsearchClient.this.ensureIndexDoesNotExist( primaryIndexName );
			return this;
		}

		public IndexClient registerForCleanup() {
			TestElasticsearchClient.this.registerIndexForCleanup( primaryIndexName );
			return this;
		}

		public TypeClient type() {
			return new TypeClient( this );
		}

		public IndexSettingsClient settings() {
			return settings( null );
		}

		public IndexSettingsClient settings(String settingsPath) {
			return new IndexSettingsClient( this, settingsPath );
		}

		public IndexAliasesClient aliases() {
			return new IndexAliasesClient( this );
		}
	}

	public class TypeClient {

		private final IndexClient indexClient;

		public TypeClient(IndexClient indexClient) {
			this.indexClient = indexClient;
		}

		public TypeClient putMapping(String mappingJson) {
			JsonObject mappingJsonObject = toJsonElement( mappingJson ).getAsJsonObject();
			return putMapping( mappingJsonObject );
		}

		public TypeClient putMapping(JsonObject mapping) {
			TestElasticsearchClient.this.putMapping( indexClient.primaryIndexName, mapping );
			return this;
		}

		public String getMapping() {
			return TestElasticsearchClient.this.getMapping( indexClient.primaryIndexName );
		}
	}

	public class IndexSettingsClient {

		private final IndexClient indexClient;

		private final String settingsPath;

		public IndexSettingsClient(IndexClient indexClient, String settingsPath) {
			this.indexClient = indexClient;
			this.settingsPath = settingsPath;
		}

		public String get() {
			URLEncodedString indexName = indexClient.primaryIndexName;
			return TestElasticsearchClient.this.getSettings( indexName, settingsPath );
		}

		/**
		 * Put settings without closing the index first.
		 *
		 * @param settings The settings value to put
		 */
		public void putDynamic(String settings) {
			URLEncodedString indexName = indexClient.primaryIndexName;
			JsonObject settingsAsJsonObject = buildStructuredSettings( settingsPath, settings );
			TestElasticsearchClient.this.putIndexSettingsDynamic( indexName, settingsAsJsonObject );
		}
	}

	public class IndexAliasesClient {

		private final IndexClient indexClient;

		public IndexAliasesClient(IndexClient indexClient) {
			this.indexClient = indexClient;
		}

		public String get() {
			URLEncodedString indexPrimaryName = indexClient.primaryIndexName;
			return TestElasticsearchClient.this.getAliases( indexPrimaryName );
		}

		public IndexAliasesClient put(String alias) {
			return put( alias, (JsonObject) null );
		}

		public IndexAliasesClient put(String alias, String aliasAttributes) {
			JsonObject aliasAttributesAsJsonObject =
					aliasAttributes == null ? null : toJsonElement( aliasAttributes ).getAsJsonObject();
			return put( alias, aliasAttributesAsJsonObject );
		}

		public IndexAliasesClient put(String alias, JsonObject aliasAttributes) {
			String indexPrimaryName = indexClient.primaryIndexName.original;
			TestElasticsearchClient.this.updateAliases(
					TestElasticsearchClient.this.createAddAliasAction( indexPrimaryName, alias, aliasAttributes )
			);
			return this;
		}

		public void move(String alias, String newIndexPrimaryName, JsonObject aliasAttributes) {
			String oldIndexPrimaryName = indexClient.primaryIndexName.original;
			TestElasticsearchClient.this.updateAliases(
					TestElasticsearchClient.this.createRemoveAliasAction( oldIndexPrimaryName, alias ),
					TestElasticsearchClient.this.createAddAliasAction( newIndexPrimaryName, alias, aliasAttributes )
			);
		}
	}

	private void deleteAndCreateIndex(URLEncodedString primaryIndexName,
			URLEncodedString writeAlias, URLEncodedString readAlias) {
		deleteAndCreateIndex( primaryIndexName, writeAlias, readAlias, null );
	}

	private void deleteAndCreateIndex(URLEncodedString primaryIndexName,
			URLEncodedString writeAlias, URLEncodedString readAlias,
			JsonObject settingsAsJsonObject) {
		ElasticsearchRequest.Builder builder = ElasticsearchRequest.put()
				.pathComponent( primaryIndexName );

		JsonObject payload = new JsonObject();

		JsonObject aliases = aliasDefinitions(
				writeAlias != null ? writeAlias.original : null,
				readAlias != null ? readAlias.original : null
		);
		payload.add( "aliases", aliases );

		if ( settingsAsJsonObject != null ) {
			payload.add( "settings", settingsAsJsonObject );
		}

		builder.body( payload );

		Boolean includeTypeName = dialect.getIncludeTypeNameParameterForMappingApi();
		if ( includeTypeName != null ) {
			builder.param( "include_type_name", includeTypeName );
		}

		doDeleteAndCreateIndex(
				primaryIndexName,
				builder.build()
		);
	}

	private void doDeleteAndCreateIndex(URLEncodedString indexName, ElasticsearchRequest createRequest) {
		// We're okay with deletion failing if it's just because the index doesn't exist yet
		tryDeleteESIndex( indexName );

		registerIndexForCleanup( indexName );
		performRequest( createRequest );

		waitForRequiredIndexStatus( indexName );
	}

	private void deleteIndex(URLEncodedString indexName) {
		// We're okay with deletion failing if it's just because the index doesn't exist yet
		tryDeleteESIndex( indexName );
	}

	private void ensureIndexDoesNotExist(URLEncodedString indexName) {
		performRequestIgnore404( ElasticsearchRequest.delete()
				.pathComponent( indexName )
				.build() );
	}

	private JsonObject createAddAliasAction(String indexName, String alias, JsonObject aliasAttributes) {
		JsonObject action = new JsonObject();
		JsonObject aliasDefinition = new JsonObject();
		action.add( "add", aliasDefinition );

		aliasDefinition.addProperty( "index", indexName );
		aliasDefinition.addProperty( "alias", alias );

		if ( aliasAttributes != null ) {
			for ( Map.Entry<String, JsonElement> entry : aliasAttributes.entrySet() ) {
				aliasDefinition.add( entry.getKey(), entry.getValue() );
			}
		}

		return action;
	}

	private JsonObject createRemoveAliasAction(String indexName, String alias) {
		JsonObject action = new JsonObject();
		JsonObject aliasDefinition = new JsonObject();
		action.add( "remove", aliasDefinition );

		aliasDefinition.addProperty( "index", indexName );
		aliasDefinition.addProperty( "alias", alias );

		return action;
	}

	private void updateAliases(JsonObject ... actions) {
		ElasticsearchRequest.Builder builder = ElasticsearchRequest.post()
				.pathComponent( Paths._ALIASES );

		JsonObject payload = new JsonObject();
		JsonArray actionArray = new JsonArray();
		payload.add( "actions", actionArray );
		for ( JsonObject action : actions ) {
			actionArray.add( action );
		}

		builder.body( payload );

		performRequest( builder.build() );
	}

	private void registerIndexForCleanup(URLEncodedString indexName) {
		createdIndicesNames.add( indexName );
	}

	private boolean exists(final URLEncodedString indexName) {
		ElasticsearchResponse response = performRequestIgnore404( ElasticsearchRequest.get()
				.pathComponent( indexName )
				.build() );
		int code = response.statusCode();
		return 200 <= code && code < 300;
	}

	private void waitForRequiredIndexStatus(final URLEncodedString indexName) {
		performRequest( ElasticsearchRequest.get()
				.pathComponent( Paths._CLUSTER ).pathComponent( Paths.HEALTH ).pathComponent( indexName )
				/*
				 * We only wait for YELLOW: it's perfectly fine, and some tests actually expect
				 * the indexes to never reach a green status
				 */
				.param( "wait_for_status", IndexStatus.YELLOW.externalRepresentation() )
				.param( "timeout", ElasticsearchIndexSettings.Defaults.SCHEMA_MANAGEMENT_MINIMAL_REQUIRED_STATUS_WAIT_TIMEOUT + "ms" )
				.build() );
	}

	private void putMapping(URLEncodedString indexName, JsonObject mappingJsonObject) {
		ElasticsearchRequest.Builder builder = ElasticsearchRequest.put()
				.pathComponent( indexName ).pathComponent( Paths._MAPPING );
		dialect.getTypeNameForMappingAndBulkApi().ifPresent( builder::pathComponent );
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
		dialect.getTypeNameForMappingAndBulkApi().ifPresent( builder::pathComponent );

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
		JsonObject result = response.body();
		JsonElement index = result.get( indexName.original );
		if ( index == null ) {
			return new JsonObject().toString();
		}
		JsonElement mappings = index.getAsJsonObject().get( "mappings" );
		if ( mappings == null ) {
			return new JsonObject().toString();
		}
		Optional<URLEncodedString> typeName = dialect.getTypeNameForMappingAndBulkApi();
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

	private void putIndexSettingsDynamic(URLEncodedString indexName, JsonObject settingsJsonObject) {
		performRequest( ElasticsearchRequest.put()
				.pathComponent( indexName ).pathComponent( Paths._SETTINGS )
				.body( settingsJsonObject )
				.build() );
	}

	private JsonObject buildStructuredSettings(String settingsPath, String settings) {
		JsonElement settingsJsonElement = toJsonElement( settings );

		if ( settingsPath != null ) {
			List<String> components = Arrays.asList( settingsPath.split( "\\." ) );
			Collections.reverse( components );
			for ( String property : components ) {
				JsonObject parent = new JsonObject();
				parent.add( property, settingsJsonElement );
				settingsJsonElement = parent;
			}
		}

		return settingsJsonElement.getAsJsonObject();
	}

	private String getSettings(URLEncodedString indexName, String path) {
		ElasticsearchResponse response = performRequest( ElasticsearchRequest.get()
				.pathComponent( indexName ).pathComponent( Paths._SETTINGS )
				.build() );
		JsonObject result = response.body();
		JsonElement index = result.get( indexName.original );
		if ( index == null ) {
			return new JsonObject().toString();
		}
		JsonElement settings = index.getAsJsonObject().get( "settings" );
		if ( path != null ) {
			for ( String property : path.split( "\\." ) ) {
				if ( settings == null ) {
					break;
				}
				settings = settings.getAsJsonObject().get( property );
			}
		}
		if ( settings == null ) {
			return new JsonObject().toString();
		}
		return settings.toString();
	}

	private String getAliases(URLEncodedString indexName) {
		ElasticsearchResponse response = performRequest( ElasticsearchRequest.get()
				.pathComponent( indexName ).pathComponent( URLEncodedString.fromString( "_alias" ) )
				.build() );
		JsonObject result = response.body();
		JsonElement index = result.get( indexName.original );
		if ( index == null ) {
			index = new JsonObject();
		}
		JsonElement aliases = index.getAsJsonObject().get( "aliases" );
		if ( aliases == null ) {
			aliases = new JsonObject();
		}
		return aliases.toString();
	}

	@Override
	public Statement apply(Statement base, Description description) {
		TestConfigurationProvider configurationProvider = new TestConfigurationProvider();
		Statement wrapped = new Statement() {
			@Override
			public void evaluate() throws Throwable {
				// Using the closer like this allows to suppress exceptions thrown by the 'finally' block.
				try ( Closer<IOException> closer = new Closer<>() ) {
					try {
						open( configurationProvider );
						base.evaluate();
					}
					finally {
						close( closer );
					}
				}
			}
		};
		return configurationProvider.apply( wrapped, description );
	}

	public void open(TestConfigurationProvider configurationProvider) {
		Map<String, Object> map = new LinkedHashMap<>();
		ElasticsearchTestHostConnectionConfiguration.get().addToBackendProperties( map );
		ConfigurationPropertySource backendProperties = AllAwareConfigurationPropertySource.fromMap( map );
		threadPoolProvider = new ThreadPoolProviderImpl(
				BeanHolder.of( new EmbeddedThreadProvider( "Test Elasticsearch client: " ) )
		);
		timeoutExecutorService = threadPoolProvider.newScheduledExecutor( 1, "Timeout - " );

		BeanResolver beanResolver = configurationProvider.createBeanResolverForTest();
		/*
		 * We use a {@link ElasticsearchClientFactoryImpl} to create our low-level client.
		 *
		 * The main advantage is that we ensure we connect to Elasticsearch exactly the same way
		 * as any test-created SearchFactory, enabling support for things like testing on AWS
		 * (using the hibernate-search-elasticsearch-aws module).
		 */
		client = new ElasticsearchClientFactoryImpl().create( beanResolver, backendProperties,
				threadPoolProvider.threadProvider(), "Client",
				new DelegatingSimpleScheduledExecutor(
						timeoutExecutorService,
						threadPoolProvider.isScheduledExecutorBlocking()
				),
				GsonProvider.create( GsonBuilder::new, true )
		);
	}

	@Override
	public void close() throws IOException {
		try ( Closer<IOException> closer = new Closer<>() ) {
			close( closer );
		}
	}

	private void close(Closer<IOException> closer) {
		closer.pushAll( this::tryDeleteESIndex, createdIndicesNames );
		createdIndicesNames.clear();
		closer.push( this::tryCloseClient, client );
		client = null;
		closer.push( ThreadPoolProviderImpl::close, threadPoolProvider );
		threadPoolProvider = null;
		closer.push( ScheduledExecutorService::shutdownNow, timeoutExecutorService );
		timeoutExecutorService = null;
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

	private void tryCloseClient(ElasticsearchClientImplementor client) {
		try {
			client.close();
		}
		catch (RuntimeException e) {
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
		int statusCode = response.statusCode();
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
		int statusCode = response.statusCode();
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
		return JsonParser.parseString( jsonAsString );
	}

}
