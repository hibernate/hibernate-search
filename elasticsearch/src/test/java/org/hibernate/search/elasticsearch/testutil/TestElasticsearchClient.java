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

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchIndexStatus;
import org.hibernate.search.elasticsearch.impl.DefaultGsonService;
import org.hibernate.search.elasticsearch.impl.ElasticsearchIndexNameNormalizer;
import org.hibernate.search.elasticsearch.impl.JsonBuilder;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.util.impl.ElasticsearchClientUtils;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchRequest;
import org.hibernate.search.exception.AssertionFailure;
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

	public TypeClient type(Class<?> rootClass) {
		return index( rootClass ).type( rootClass );
	}

	public class IndexClient {

		private final String indexName;

		public IndexClient(String indexName) {
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

		private final String typeName;

		public TypeClient(IndexClient indexClient, String mappingName) {
			this.indexClient = indexClient;
			this.typeName = mappingName;
		}

		public TypeClient putMapping(String mappingJson) throws IOException {
			TestElasticsearchClient.this.putMapping( indexClient.indexName, typeName, mappingJson );
			return this;
		}

		public String getMapping() throws IOException {
			return TestElasticsearchClient.this.getMapping( indexClient.indexName, typeName );
		}

		public TypeClient index(String id, String jsonDocument) throws IOException {
			TestElasticsearchClient.this.index( indexClient.indexName, typeName, id, jsonDocument );
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
			return TestElasticsearchClient.this.getSettings( indexClient.indexName, settingsPath );
		}

		public void put(String settings) throws IOException {
			TestElasticsearchClient.this.putSettings( indexClient.indexName, settingsPath, settings );
		}
	}

	public class DocumentClient {

		private final TypeClient typeClient;

		private final String id;

		public DocumentClient(TypeClient typeClient, String id) {
			this.typeClient = typeClient;
			this.id = id;
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

	private void deleteAndCreateIndex(String indexName) throws IOException {
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
				.pathComponent( "_template" ).pathComponent( templateName )
				.body( source )
				.build() );
	}

	private void ensureIndexDoesNotExist(String indexName) throws IOException {
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

	private void registerIndexForCleanup(String indexName) {
		createdIndicesNames.add( indexName );
	}

	private void registerTemplateForCleanup(String templateName) {
		createdTemplatesNames.add( templateName );
	}

	private void waitForRequiredIndexStatus(final String indexName) throws IOException {
		performRequest( ElasticsearchRequest.get()
				.pathComponent( "_cluster" ).pathComponent( "health" ).pathComponent( indexName )
				.param( "wait_for_status", requiredIndexStatus.getElasticsearchString() )
				.param( "timeout", ElasticsearchEnvironment.Defaults.INDEX_MANAGEMENT_WAIT_TIMEOUT + "ms" )
				.build() );
	}

	private void putMapping(String indexName, String mappingName, String mappingJson) throws IOException {
		JsonObject mappingJsonObject = toJsonElement( mappingJson ).getAsJsonObject();

		performRequest( ElasticsearchRequest.put()
				.pathComponent( indexName ).pathComponent( "_mapping" ).pathComponent( mappingName )
				.body( mappingJsonObject )
				.build() );
	}

	private String getMapping(String indexName, String mappingName) throws IOException {
		Response response = performRequest( ElasticsearchRequest.get()
				.pathComponent( indexName ).pathComponent( "_mapping" ).pathComponent( mappingName )
				.build() );
		JsonObject result = toJsonObject( response );
		JsonElement index = result.get( indexName );
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

		performRequest( ElasticsearchRequest.post()
				.pathComponent( indexName )
				.pathComponent( "_close" )
				.build() );

		performRequest( ElasticsearchRequest.put()
				.pathComponent( indexName ).pathComponent( "_settings" )
				.body( settingsJsonElement.getAsJsonObject() )
				.build() );

		performRequest( ElasticsearchRequest.post()
				.pathComponent( indexName )
				.pathComponent( "_open" )
				.build() );
	}

	private String getSettings(String indexName, String path) throws IOException {
		Response response = performRequest( ElasticsearchRequest.get()
				.pathComponent( indexName ).pathComponent( "_settings" )
				.build() );
		JsonObject result = toJsonObject( response );
		JsonElement index = result.get( indexName );
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
		JsonObject documentJsonObject = toJsonElement( jsonDocument ).getAsJsonObject();
		performRequest( ElasticsearchRequest.put()
				.pathComponent( indexName ).pathComponent( typeName ).pathComponent( id )
				.body( documentJsonObject )
				.param( "refresh", true )
				.build() );
	}

	private JsonObject getDocumentSource(String indexName, String typeName, String id) throws IOException {
		Response response = performRequest( ElasticsearchRequest.get()
				.pathComponent( indexName ).pathComponent( typeName ).pathComponent( id )
				.build() );
		JsonObject result = toJsonObject( response );
		return result.get( "_source" ).getAsJsonObject();
	}

	private JsonElement getDocumentField(String indexName, String typeName, String id, String fieldName) throws IOException {
		Response response = performRequest( ElasticsearchRequest.get()
				.pathComponent( indexName ).pathComponent( typeName ).pathComponent( id )
				.param( "fields", fieldName )
				.build() );
		JsonObject result = toJsonObject( response );
		return result.get( "fields" ).getAsJsonObject().get( fieldName );
	}

	@Override
	protected void before() throws Throwable {
		gson = new DefaultGsonService().getGson();

		this.client = RestClient.builder( HttpHost.create( ElasticsearchEnvironment.Defaults.SERVER_URI ) )
				.setRequestConfigCallback( (builder) -> {
					return builder
							.setSocketTimeout( ElasticsearchEnvironment.Defaults.SERVER_READ_TIMEOUT )
							.setConnectTimeout( ElasticsearchEnvironment.Defaults.SERVER_CONNECTION_TIMEOUT );
				} )
				.build();
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

		try {
			client.close();
			client = null;
		}
		catch (IOException e) {
			throw new AssertionFailure( "Unexpected exception when closing the RestClient", e );
		}
	}

	private void tryDeleteESIndex(String indexName) {
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
					.pathComponent( "_template" ).pathComponent( templateName )
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

	private Response performRequest(ElasticsearchRequest request) throws IOException {
		return client.performRequest(
				request.getMethod(), request.getPath(), request.getParameters(),
				ElasticsearchClientUtils.toEntity( gson, request )
				);
	}

	private JsonObject toJsonObject(Response response) throws IOException {
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
