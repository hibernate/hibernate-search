package org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.extension;

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

import org.hibernate.search.backend.elasticsearch.ElasticsearchVersion;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonClientImplementor;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonRequest;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonResponse;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchClientFactory;
import org.hibernate.search.backend.elasticsearch.client.common.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchClientUtils;
import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.gson.GsonJsonHandlers;
import org.hibernate.search.backend.elasticsearch.index.IndexStatus;
import org.hibernate.search.backend.elasticsearch.index.layout.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonHandlerBridge;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonScalarConversions;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonProviderHelper;
import org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchRequestFormatter;
import org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchResponseFormatter;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.AllAwareConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.common.execution.spi.DelegatingSimpleScheduledExecutor;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.thread.impl.EmbeddedThreadProvider;
import org.hibernate.search.engine.environment.thread.impl.ThreadPoolProviderImpl;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchTestHostConnectionConfiguration;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendConfiguration;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.google.gson.JsonParser;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public class TestElasticsearchClient implements BeforeEachCallback, AfterEachCallback, Closeable {

	private static final ConfigurationProperty<BeanReference<? extends ElasticsearchClientFactory>> CLIENT_FACTORY =
			ConfigurationProperty.forKey( ElasticsearchBackendSettings.CLIENT_FACTORY )
					.asBeanReference( ElasticsearchClientFactory.class ).withDefault( BeanReference
							.of( ElasticsearchClientFactory.class, ElasticsearchBackendSettings.Defaults.CLIENT_FACTORY ) )
					.build();

	private final ElasticsearchTestDialect dialect = ElasticsearchTestDialect.get();

	private TestConfigurationProvider configurationProvider = new TestConfigurationProvider();

	private ThreadPoolProviderImpl threadPoolProvider;

	private ScheduledExecutorService timeoutExecutorService;

	private ElasticsearchJsonClientImplementor client;

	private final List<URLEncodedString> indexesToCleanUp = new ArrayList<>();

	private TestElasticsearchClient() {
	}

	public static TestElasticsearchClient create() {
		return new TestElasticsearchClient();
	}

	public ElasticsearchTestDialect getDialect() {
		return dialect;
	}

	public IndexClient index(String hibernateSearchIndexName) {
		return index( defaultPrimaryName( hibernateSearchIndexName ), defaultWriteAlias( hibernateSearchIndexName ),
				defaultReadAlias( hibernateSearchIndexName ) );
	}

	public IndexClient indexNoAlias(String hibernateSearchIndexName) {
		return index( IndexNames.encodeName( hibernateSearchIndexName ), null, null );
	}

	public IndexClient index(URLEncodedString primaryIndexName, URLEncodedString writeAlias, URLEncodedString readAlias) {
		return new IndexClient( primaryIndexName, writeAlias, readAlias );
	}

	public String getActualVersion() {
		ElasticsearchJsonResponse response = performRequestIgnore404( ElasticsearchJsonRequest.get().build() );
		return JsonScalarConversions.asString( ( (ObjectNode) response.bodyJson().get( "version" ) ).get( "number" ) );
	}

	public class IndexClient {

		private final URLEncodedString primaryIndexName;

		private final URLEncodedString writeAlias;

		private final URLEncodedString readAlias;

		public IndexClient(URLEncodedString primaryIndexName, URLEncodedString writeAlias, URLEncodedString readAlias) {
			this.primaryIndexName = primaryIndexName;
			this.writeAlias = writeAlias;
			this.readAlias = readAlias;
			// Always delete indexes manipulated through test client after the test;
			// leaving indexes on the cluster could cause problems
			// with Amazon OpenSearch Serverless in particular.
			TestElasticsearchClient.this.registerIndexForCleanup( primaryIndexName );
		}

		public boolean exists() {
			return TestElasticsearchClient.this.exists( primaryIndexName );
		}

		public IndexClient deleteAndCreate() {
			TestElasticsearchClient.this.deleteAndCreateIndex( primaryIndexName, writeAlias, readAlias );
			return this;
		}

		public IndexClient deleteAndCreate(String settingsPath, String settings) {
			ObjectNode settingsAsJsonObject = buildStructuredSettings( settingsPath, settings );
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
			ObjectNode mappingJsonObject = ( (ObjectNode) toJsonElement( mappingJson ) );
			return putMapping( mappingJsonObject );
		}

		public TypeClient putMapping(ObjectNode mapping) {
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
			ObjectNode settingsAsJsonObject = buildStructuredSettings( settingsPath, settings );
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
			return put( alias, (ObjectNode) null );
		}

		public IndexAliasesClient put(String alias, String aliasAttributes) {
			ObjectNode aliasAttributesAsJsonObject =
					aliasAttributes == null ? null : ( (ObjectNode) toJsonElement( aliasAttributes ) );
			return put( alias, aliasAttributesAsJsonObject );
		}

		public IndexAliasesClient put(String alias, ObjectNode aliasAttributes) {
			String indexPrimaryName = indexClient.primaryIndexName.original;
			TestElasticsearchClient.this.updateAliases(
					TestElasticsearchClient.this.createAddAliasAction( indexPrimaryName, alias, aliasAttributes ) );
			return this;
		}

		public void move(String alias, String newIndexPrimaryName, ObjectNode aliasAttributes) {
			String oldIndexPrimaryName = indexClient.primaryIndexName.original;
			TestElasticsearchClient.this.updateAliases(
					TestElasticsearchClient.this.createRemoveAliasAction( oldIndexPrimaryName, alias ),
					TestElasticsearchClient.this.createAddAliasAction( newIndexPrimaryName, alias, aliasAttributes ) );
		}
	}

	private void deleteAndCreateIndex(URLEncodedString primaryIndexName, URLEncodedString writeAlias,
			URLEncodedString readAlias) {
		deleteAndCreateIndex( primaryIndexName, writeAlias, readAlias, null );
	}

	private void deleteAndCreateIndex(URLEncodedString primaryIndexName, URLEncodedString writeAlias,
			URLEncodedString readAlias, ObjectNode settingsAsJsonObject) {
		ElasticsearchJsonRequest.Builder builder = ElasticsearchJsonRequest.put().pathComponent( primaryIndexName );
		ObjectNode payload = new ObjectNode( JsonNodeFactory.instance );
		ObjectNode aliases = aliasDefinitions( writeAlias != null ? writeAlias.original : null,
				readAlias != null ? readAlias.original : null );
		payload.set( "aliases", aliases );
		if ( settingsAsJsonObject != null ) {
			payload.set( "settings", settingsAsJsonObject );
		}
		builder.bodyJson( payload );
		doDeleteAndCreateIndex( primaryIndexName, builder.build() );
	}

	private void doDeleteAndCreateIndex(URLEncodedString indexName, ElasticsearchJsonRequest createRequest) {
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
		performRequestIgnore404( ElasticsearchJsonRequest.delete().pathComponent( indexName ).build() );
	}

	private ObjectNode createAddAliasAction(String indexName, String alias, ObjectNode aliasAttributes) {
		ObjectNode action = new ObjectNode( JsonNodeFactory.instance );
		ObjectNode aliasDefinition = new ObjectNode( JsonNodeFactory.instance );
		action.set( "add", aliasDefinition );
		aliasDefinition.put( "index", indexName );
		aliasDefinition.put( "alias", alias );
		if ( aliasAttributes != null ) {
			for ( Map.Entry<String, JsonNode> entry : aliasAttributes.properties() ) {
				aliasDefinition.set( entry.getKey(), entry.getValue() );
			}
		}
		return action;
	}

	private ObjectNode createRemoveAliasAction(String indexName, String alias) {
		ObjectNode action = new ObjectNode( JsonNodeFactory.instance );
		ObjectNode aliasDefinition = new ObjectNode( JsonNodeFactory.instance );
		action.set( "remove", aliasDefinition );
		aliasDefinition.put( "index", indexName );
		aliasDefinition.put( "alias", alias );
		return action;
	}

	private void updateAliases(ObjectNode... actions) {
		ElasticsearchJsonRequest.Builder builder = ElasticsearchJsonRequest.post().pathComponent( Paths._ALIASES );
		ObjectNode payload = new ObjectNode( JsonNodeFactory.instance );
		ArrayNode actionArray = new ArrayNode( JsonNodeFactory.instance );
		payload.set( "actions", actionArray );
		for ( ObjectNode action : actions ) {
			actionArray.add( action );
		}
		builder.bodyJson( payload );
		performRequest( builder.build() );
	}

	private void registerIndexForCleanup(URLEncodedString indexName) {
		indexesToCleanUp.add( indexName );
	}

	private boolean exists(final URLEncodedString indexName) {
		ElasticsearchJsonResponse response =
				performRequestIgnore404( ElasticsearchJsonRequest.get().pathComponent( indexName ).build() );
		int code = response.statusCode();
		return 200 <= code && code < 300;
	}

	private void waitForRequiredIndexStatus(final URLEncodedString indexName) {
		performRequest( ElasticsearchJsonRequest.get().pathComponent( Paths._CLUSTER ).pathComponent( Paths.HEALTH )
				.pathComponent( indexName )./*
											* We only wait for YELLOW: it's perfectly fine, and some tests actually expect
											* the indexes to never reach a green status
											*/
				param( "wait_for_status", IndexStatus.YELLOW.externalRepresentation() )
				.param( "timeout",
						ElasticsearchIndexSettings.Defaults.SCHEMA_MANAGEMENT_MINIMAL_REQUIRED_STATUS_WAIT_TIMEOUT + "ms" )
				.build() );
	}

	private void putMapping(URLEncodedString indexName, ObjectNode mappingJsonObject) {
		ElasticsearchJsonRequest.Builder builder =
				ElasticsearchJsonRequest.put().pathComponent( indexName ).pathComponent( Paths._MAPPING );
		builder.bodyJson( mappingJsonObject );
		performRequest( builder.build() );
	}

	private String getMapping(URLEncodedString indexName) {
		ElasticsearchJsonRequest.Builder builder =
				ElasticsearchJsonRequest.get().pathComponent( indexName ).pathComponent( Paths._MAPPING );
		/*
		 * Elasticsearch 5.5+ triggers a 404 error when mappings are missing,
		 * while 5.4 and below just return an empty mapping.
		 * In our case, an empty mapping is fine, so we'll just ignore 404.
		 */
		ElasticsearchJsonResponse response = performRequestIgnore404( builder.build() );
		ObjectNode result = response.bodyJson();
		JsonNode index = result.get( indexName.original );
		if ( index == null ) {
			return new ObjectNode( JsonNodeFactory.instance ).toString();
		}
		JsonNode mappings = ( (ObjectNode) index ).get( "mappings" );
		if ( mappings == null ) {
			return new ObjectNode( JsonNodeFactory.instance ).toString();
		}
		return mappings.toString();
	}

	private void putIndexSettingsDynamic(URLEncodedString indexName, ObjectNode settingsJsonObject) {
		performRequest( ElasticsearchJsonRequest.put().pathComponent( indexName ).pathComponent( Paths._SETTINGS )
				.bodyJson( settingsJsonObject ).build() );
	}

	private ObjectNode buildStructuredSettings(String settingsPath, String settings) {
		JsonNode settingsJsonElement = toJsonElement( settings );
		if ( settingsPath != null ) {
			List<String> components = Arrays.asList( settingsPath.split( "\\." ) );
			Collections.reverse( components );
			for ( String property : components ) {
				ObjectNode parent = new ObjectNode( JsonNodeFactory.instance );
				parent.set( property, settingsJsonElement );
				settingsJsonElement = parent;
			}
		}
		return ( (ObjectNode) settingsJsonElement );
	}

	private String getSettings(URLEncodedString indexName, String path) {
		ElasticsearchJsonResponse response = performRequest(
				ElasticsearchJsonRequest.get().pathComponent( indexName ).pathComponent( Paths._SETTINGS ).build() );
		ObjectNode result = response.bodyJson();
		JsonNode index = result.get( indexName.original );
		if ( index == null ) {
			return new ObjectNode( JsonNodeFactory.instance ).toString();
		}
		JsonNode settings = ( (ObjectNode) index ).get( "settings" );
		if ( path != null ) {
			for ( String property : path.split( "\\." ) ) {
				if ( settings == null ) {
					break;
				}
				settings = ( (ObjectNode) settings ).get( property );
			}
		}
		if ( settings == null ) {
			return new ObjectNode( JsonNodeFactory.instance ).toString();
		}
		return settings.toString();
	}

	private String getAliases(URLEncodedString indexName) {
		ElasticsearchJsonResponse response = performRequest( ElasticsearchJsonRequest.get().pathComponent( indexName )
				.pathComponent( URLEncodedString.fromString( "_alias" ) ).build() );
		ObjectNode result = response.bodyJson();
		JsonNode index = result.get( indexName.original );
		if ( index == null ) {
			index = new ObjectNode( JsonNodeFactory.instance );
		}
		JsonNode aliases = ( (ObjectNode) index ).get( "aliases" );
		if ( aliases == null ) {
			aliases = new ObjectNode( JsonNodeFactory.instance );
		}
		return aliases.toString();
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		configurationProvider.beforeEach( context );
		open( configurationProvider );
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		close();
		configurationProvider.afterEach( context );
	}

	public void open(TestConfigurationProvider configurationProvider) {
		open( configurationProvider, Optional.of( ElasticsearchTestDialect.getActualVersion() ) );
	}

	public void open(TestConfigurationProvider configurationProvider, Optional<ElasticsearchVersion> elasticsearchVersion) {
		Map<String, Object> map = new LinkedHashMap<>();
		ElasticsearchTestHostConnectionConfiguration.get().addToBackendProperties( map );
		ConfigurationPropertySource backendProperties = AllAwareConfigurationPropertySource.fromMap( map );
		threadPoolProvider =
				new ThreadPoolProviderImpl( BeanHolder.of( new EmbeddedThreadProvider( "Test Elasticsearch client: " ) ) );
		timeoutExecutorService = threadPoolProvider.newScheduledExecutor( 1, "Timeout - " );
		BeanResolver beanResolver = configurationProvider.createBeanResolverForTest();
		/*
		 * We use a {@link ElasticsearchClientFactory} to create our low-level client.
		 *
		 * The main advantage is that we ensure we connect to Elasticsearch exactly the same way
		 * as any test-created SearchFactory, enabling support for things like testing on AWS
		 * (using the hibernate-search-elasticsearch-aws module).
		 */
		client = new org.hibernate.search.backend.elasticsearch.client.common.impl.JsonTransportClient(
				getDiscoveredClientFactory( beanResolver ).get().create( beanResolver, backendProperties,
						threadPoolProvider.threadProvider(), "Client",
						new DelegatingSimpleScheduledExecutor( timeoutExecutorService,
								threadPoolProvider.isScheduledExecutorBlocking() ) ),
				JsonProviderHelper.create( true ) );
	}

	@Override
	public void close() throws IOException {
		try ( Closer<IOException> closer = new Closer<>() ) {
			close( closer );
		}
	}

	private void close(Closer<IOException> closer) {
		closer.pushAll( this::tryDeleteESIndex, indexesToCleanUp );
		indexesToCleanUp.clear();
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
			performRequestIgnore404( ElasticsearchJsonRequest.delete().pathComponent( indexName ).build() );
		}
		catch (RuntimeException e) {
			throw new AssertionFailure(
					String.format( Locale.ROOT, "Error while trying to delete index '%s' as part of test cleanup", indexName ),
					e );
		}
	}

	private void tryCloseClient(ElasticsearchJsonClientImplementor client) {
		try {
			client.close();
		}
		catch (RuntimeException e) {
			throw new AssertionFailure( "Unexpected exception when closing the ElasticsearchClient used in "
					+ TestElasticsearchClient.class.getSimpleName(), e );
		}
	}

	protected ElasticsearchJsonResponse performRequest(ElasticsearchJsonRequest request) {
		ElasticsearchJsonResponse response;
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

	protected ElasticsearchJsonResponse performRequestIgnore404(ElasticsearchJsonRequest request) {
		ElasticsearchJsonResponse response;
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

	private AssertionFailure requestFailed(ElasticsearchJsonRequest request, Exception e) {
		return new AssertionFailure( "Elasticsearch request in TestElasticsearchClient failed:" + "Request:\n" + "========\n"
				+ new ElasticsearchRequestFormatter( request ), e );
	}

	private AssertionFailure requestFailed(ElasticsearchJsonRequest request, ElasticsearchJsonResponse response) {
		return new AssertionFailure( "Elasticsearch request in TestElasticsearchClient failed:\n" + "Request:\n" + "========\n"
				+ new ElasticsearchRequestFormatter( request ) + "\nResponse:\n" + "========\n"
				+ new ElasticsearchResponseFormatter( response ) );
	}

	/*
	 * Historical fixture Strings accept Gson's lenient syntax, including single quotes.
	 * Convert once at this test-only legacy-input boundary. Request/response processing
	 * uses Jackson nodes and does not use this compatibility path.
	 */
	@SuppressWarnings("removal")
	private JsonNode toJsonElement(String jsonAsString) {
		return JsonHandlerBridge.importValue( GsonJsonHandlers.value(), JsonParser.parseString( jsonAsString ),
				"test fixture import" );
	}

	public static BeanHolder<? extends ElasticsearchClientFactory> getDiscoveredClientFactory(BeanResolver beanResolver) {
		return beanResolver.resolve( BeanReference.of( ElasticsearchClientFactory.class,
				BackendConfiguration.ELASTICSEARCH_BACKEND_CLIENT_TYPE != null
						? BackendConfiguration.ELASTICSEARCH_BACKEND_CLIENT_TYPE
						: BackendConfiguration.IDE_ELASTICSEARCH_BACKEND_CLIENT_TYPE ) );
	}
}
