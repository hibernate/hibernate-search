/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.similarities.Similarity;
import org.hibernate.search.analyzer.impl.AnalyzerReference;
import org.hibernate.search.analyzer.impl.RemoteAnalyzer;
import org.hibernate.search.analyzer.impl.RemoteAnalyzerProvider;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.backend.FlushLuceneWork;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.cfg.IndexSchemaManagementStrategy;
import org.hibernate.search.elasticsearch.client.impl.BackendRequest;
import org.hibernate.search.elasticsearch.client.impl.BackendRequestProcessor;
import org.hibernate.search.elasticsearch.client.impl.JestClient;
import org.hibernate.search.elasticsearch.impl.FieldHelper.ExtendedFieldType;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.spi.ElasticsearchIndexManagerType;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.metadata.impl.BridgeDefinedField;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
import org.hibernate.search.engine.metadata.impl.EmbeddedTypeMetadata;
import org.hibernate.search.engine.metadata.impl.FacetMetadata;
import org.hibernate.search.engine.metadata.impl.PropertyMetadata;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.indexes.spi.IndexManagerType;
import org.hibernate.search.indexes.spi.ReaderProvider;
import org.hibernate.search.spatial.impl.SpatialHelper;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.searchbox.client.JestResult;
import io.searchbox.cluster.Health;
import io.searchbox.cluster.Health.Builder;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.DeleteIndex;
import io.searchbox.indices.IndicesExists;
import io.searchbox.indices.mapping.PutMapping;

/**
 * An {@link IndexManager} applying indexing work to an Elasticsearch server.
 *
 * @author Gunnar Morling
 */
public class ElasticsearchIndexManager implements IndexManager, RemoteAnalyzerProvider {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private static final String ANALYZED = "analyzed";
	private static final String NOT_ANALYZED = "not_analyzed";
	private static final String NOT_INDEXED = "no";

	private String indexName;
	private String actualIndexName;
	private boolean refreshAfterWrite;
	private IndexSchemaManagementStrategy indexManagementStrategy;
	private String indexManagementWaitTimeout;
	private boolean multitenancyEnabled;

	/**
	 * Status the index needs to be at least in, otherwise we'll fail starting up.
	 */
	private IndexStatus requiredIndexStatus;

	private Similarity similarity;

	private ExtendedSearchIntegrator searchIntegrator;
	private final Set<Class<?>> containedEntityTypes = new HashSet<>();

	private ServiceManager serviceManager;

	private GsonService gsonService;

	private ElasticsearchIndexWorkVisitor visitor;
	private JestClient jestClient;
	private BackendRequestProcessor requestProcessor;

	private enum IndexStatus {

		GREEN("green"),
		YELLOW("yellow"),
		RED("red");

		private final String elasticsearchString;

		private IndexStatus(String elasticsearchString) {
			this.elasticsearchString = elasticsearchString;
		}

		public String getElasticsearchString() {
			return elasticsearchString;
		}

		static IndexStatus fromString(String status) {
			for ( IndexStatus indexStatus : IndexStatus.values() ) {
				if ( indexStatus.getElasticsearchString().equalsIgnoreCase( status ) ) {
					return indexStatus;
				}
			}

			throw LOG.unexpectedIndexStatusString( status );
		}
	}

	// Lifecycle

	@Override
	public void initialize(String indexName, Properties properties, Similarity similarity, WorkerBuildContext context) {
		this.serviceManager = context.getServiceManager();

		this.indexName = getIndexName( indexName, properties );
		this.requiredIndexStatus = getRequiredIndexStatus( properties );
		this.indexManagementStrategy = getIndexManagementStrategy( properties );
		this.indexManagementWaitTimeout = getIndexManagementWaitTimeout( properties );
		this.actualIndexName = IndexNameNormalizer.getElasticsearchIndexName( this.indexName );
		this.refreshAfterWrite = getRefreshAfterWrite( properties );
		this.multitenancyEnabled = context.isMultitenancyEnabled();

		this.similarity = similarity;

		this.gsonService = serviceManager.requestService( GsonService.class );

		this.jestClient = serviceManager.requestService( JestClient.class );
		this.visitor = new ElasticsearchIndexWorkVisitor(
				this.actualIndexName,
				this.refreshAfterWrite,
				context.getUninitializedSearchIntegrator()
		);
		this.requestProcessor = context.getServiceManager().requestService( BackendRequestProcessor.class );
	}

	private static String getIndexName(String indexName, Properties properties) {
		String name = properties.getProperty( Environment.INDEX_NAME_PROP_NAME );
		return name != null ? name : indexName;
	}

	private static IndexSchemaManagementStrategy getIndexManagementStrategy(Properties properties) {
		String strategy = properties.getProperty( ElasticsearchEnvironment.INDEX_SCHEMA_MANAGEMENT_STRATEGY );
		return strategy != null ? IndexSchemaManagementStrategy.valueOf( strategy ) : ElasticsearchEnvironment.Defaults.INDEX_SCHEMA_MANAGEMENT_STRATEGY;
	}

	private static String getIndexManagementWaitTimeout(Properties properties) {
		int timeout = ConfigurationParseHelper.getIntValue(
				properties,
				ElasticsearchEnvironment.INDEX_MANAGEMENT_WAIT_TIMEOUT,
				ElasticsearchEnvironment.Defaults.INDEX_MANAGEMENT_WAIT_TIMEOUT
		);

		if ( timeout < 0 ) {
			throw LOG.negativeTimeoutValue( timeout );
		}

		return timeout + "ms";
	}

	private static IndexStatus getRequiredIndexStatus(Properties properties) {
		String status = ConfigurationParseHelper.getString(
				properties,
				ElasticsearchEnvironment.REQUIRED_INDEX_STATUS,
				ElasticsearchEnvironment.Defaults.REQUIRED_INDEX_STATUS
		);

		return IndexStatus.fromString( status );
	}

	private static boolean getRefreshAfterWrite(Properties properties) {
		return ConfigurationParseHelper.getBooleanValue(
				properties,
				ElasticsearchEnvironment.REFRESH_AFTER_WRITE,
				ElasticsearchEnvironment.Defaults.REFRESH_AFTER_WRITE
		);
	}

	@Override
	public void destroy() {
		if ( indexManagementStrategy == IndexSchemaManagementStrategy.RECREATE_DELETE ) {
			deleteIndexIfExisting();
		}

		requestProcessor = null;
		serviceManager.releaseService( BackendRequestProcessor.class );

		serviceManager.releaseService( JestClient.class );
		jestClient = null;

		serviceManager.releaseService( GsonService.class );
		gsonService = null;
	}

	@Override
	public void setSearchFactory(ExtendedSearchIntegrator boundSearchIntegrator) {
		this.searchIntegrator = boundSearchIntegrator;
		initializeIndex();
	}

	private void initializeIndex() {
		if ( indexManagementStrategy == IndexSchemaManagementStrategy.NONE ) {
			return;
		}
		else if ( indexManagementStrategy == IndexSchemaManagementStrategy.CREATE ) {
			if ( createIndexIfNotYetExisting() ) {
				createIndexMappings();
			}
		}
		else if ( indexManagementStrategy == IndexSchemaManagementStrategy.RECREATE ||
				indexManagementStrategy == IndexSchemaManagementStrategy.RECREATE_DELETE ) {
			deleteIndexIfExisting();
			createIndex();
			createIndexMappings();
		}
		else if ( indexManagementStrategy == IndexSchemaManagementStrategy.MERGE ) {
			createIndexIfNotYetExisting();
			createIndexMappings();
		}
	}

	@Override
	public void addContainedEntity(Class<?> entity) {
		containedEntityTypes.add( entity );
	}

	private void createIndex() {
		CreateIndex createIndex = new CreateIndex.Builder( actualIndexName )
				.build();

		jestClient.executeRequest( createIndex );

		waitForIndexCreation();
	}

	private void waitForIndexCreation() {
		Builder healthBuilder = new Health.Builder()
				.setParameter( "wait_for_status", requiredIndexStatus.getElasticsearchString() )
				.setParameter( "timeout", indexManagementWaitTimeout );

		Health health = new Health( healthBuilder ) {
			@Override
			protected String buildURI() {
				return super.buildURI() + actualIndexName;
			}
		};

		JestResult result = jestClient.executeRequest( health, 408 );

		if ( !result.isSucceeded() ) {
			String status = result.getJsonObject().get( "status" ).getAsString();
			throw LOG.unexpectedIndexStatus( actualIndexName, requiredIndexStatus.getElasticsearchString(),
					status );
		}
	}

	/**
	 * @return {@code true} if the index was actually created, {@code false} if it already existed.
	 */
	private boolean createIndexIfNotYetExisting() {
		if ( jestClient.executeRequest( new IndicesExists.Builder( actualIndexName ).build(), 404 ).getResponseCode() == 200 ) {
			return false;
		}

		JestResult result = jestClient.executeRequest(
				new CreateIndex.Builder( actualIndexName ).build(),
				"index_already_exists_exception"
				);
		if ( !result.isSucceeded() ) {
			// The index was created just after we checked if it existed: just do as if it had been created when we checked.
			return false;
		}

		return true;
	}

	private void deleteIndexIfExisting() {
		// Not actually needed, but do it to avoid cluttering the ES log
		if ( jestClient.executeRequest( new IndicesExists.Builder( actualIndexName ).build(), 404 ).getResponseCode() == 404 ) {
			return;
		}

		try {
			jestClient.executeRequest( new DeleteIndex.Builder( actualIndexName ).build() );
		}
		catch (SearchException e) {
			// ignoring deletion of non-existing index
			if ( !e.getMessage().contains( "index_not_found_exception" ) ) {
				throw e;
			}
		}
	}

	private static class IncompleteDataException extends SearchException {
		public IncompleteDataException(String message) {
			super( message );
		}
	}

	// TODO HSEARCH-2260
	// What happens if mappings already exist? We need an option similar to hbm2ddl
	// What happens if several nodes in a cluster try to create the mappings?
	private void createIndexMappings() {
		for ( Class<?> entityType : containedEntityTypes ) {
			EntityIndexBinding descriptor = searchIntegrator.getIndexBinding( entityType );

			JsonObject payload = new JsonObject();
			payload.addProperty( "dynamic", "strict" );
			JsonObject properties = new JsonObject();
			payload.add( "properties", properties );

			if ( multitenancyEnabled ) {
				JsonObject field = new JsonObject();
				field.addProperty( "type", ElasticsearchFieldType.STRING.getElasticsearchString() );
				field.addProperty( "index", NOT_ANALYZED );
				properties.add( DocumentBuilderIndexedEntity.TENANT_ID_FIELDNAME, field );
			}

			addMappings( new ElasticsearchMappingBuilder( descriptor, payload ) );

			PutMapping putMapping = new PutMapping.Builder(
					actualIndexName,
					entityType.getName(),
					payload
			)
			.build();

			try {
				jestClient.executeRequest( putMapping );
			}
			catch (Exception e) {
				throw LOG.elasticsearchMappingCreationFailed( entityType.getName(), e );
			}
		}
	}

	private void addMappings(ElasticsearchMappingBuilder mappingBuilder) {
		TypeMetadata typeMetadata = mappingBuilder.getMetadata();

		// normal document fields
		for ( DocumentFieldMetadata fieldMetadata : typeMetadata.getNonEmbeddedDocumentFieldMetadata() ) {
			if ( fieldMetadata.isId() || fieldMetadata.getFieldName().isEmpty() || fieldMetadata.getFieldName().endsWith( "." )
					|| fieldMetadata.isSpatial() ) {
				continue;
			}

			try {
				addFieldMapping( mappingBuilder, fieldMetadata );
			}
			catch (IncompleteDataException e) {
				LOG.debug( "Not adding a mapping for field " + fieldMetadata.getFieldName() + " because of incomplete data", e );
			}
		}

		// bridge-defined fields
		for ( BridgeDefinedField bridgeDefinedField : getNonEmbeddedBridgeDefinedFields( typeMetadata ) ) {
			try {
				addFieldMapping( mappingBuilder, bridgeDefinedField );
			}
			catch (IncompleteDataException e) {
				LOG.debug( "Not adding a mapping for field " + bridgeDefinedField.getName() + " because of incomplete data", e );
			}
		}

		// Recurse into embedded types
		for ( EmbeddedTypeMetadata embeddedTypeMetadata : typeMetadata.getEmbeddedTypeMetadata() ) {
			ElasticsearchMappingBuilder embeddedContext = mappingBuilder.createEmbedded( embeddedTypeMetadata );
			addMappings( embeddedContext );
		}
	}

	/**
	 * Adds a type mapping for the given field to the given request payload.
	 */
	private void addFieldMapping(ElasticsearchMappingBuilder mappingBuilder, DocumentFieldMetadata fieldMetadata) {
		String fieldPath = fieldMetadata.getName();
		JsonObject field = new JsonObject();

		ElasticsearchFieldType fieldType = addTypeOptions( field, fieldMetadata );

		field.addProperty( "store", fieldMetadata.getStore() == Store.NO ? false : true );

		addIndexOptions( field, mappingBuilder, fieldMetadata.getSourceProperty(), fieldMetadata.getFieldName(),
				fieldType, fieldMetadata.getIndex(), fieldMetadata.getAnalyzerReference() );

		field.addProperty( "boost", mappingBuilder.getBoost( fieldMetadata.getBoost() ) );

		if ( fieldMetadata.indexNullAs() != null ) {
			JsonElement nullValueJsonElement = getNullValue( fieldType, fieldMetadata );
			field.add( "null_value", nullValueJsonElement );
		}

		mappingBuilder.setPropertyAbsolute( fieldPath, field );

		// Create facet fields if needed: if the facet has the same name as the field, we don't need to create an
		// extra field for it
		for ( FacetMetadata facetMetadata : fieldMetadata.getFacetMetadata() ) {
			if ( !facetMetadata.getFacetName().equals( fieldMetadata.getFieldName() ) ) {
				try {
					addFieldMapping( mappingBuilder, facetMetadata );
				}
				catch (IncompleteDataException e) {
					LOG.debug( "Not adding a mapping for facet " + facetMetadata.getFacetName() + " because of incomplete data", e );
				}
			}
		}
	}

	/**
	 * Adds a type mapping for the given field to the given request payload.
	 */
	private void addFieldMapping(ElasticsearchMappingBuilder mappingBuilder, BridgeDefinedField bridgeDefinedField) {
		String fieldPath = bridgeDefinedField.getName();
		if ( !SpatialHelper.isSpatialField( fieldPath ) ) {
			JsonObject field = new JsonObject();

			ElasticsearchFieldType fieldType = addTypeOptions( field, bridgeDefinedField );

			addIndexOptions( field, mappingBuilder, bridgeDefinedField.getSourceField().getSourceProperty(),
					fieldPath, fieldType, bridgeDefinedField.getIndex(), null );

			// we don't overwrite already defined fields. Typically, in the case of spatial, the geo_point field
			// is defined before the double field and we want to keep the geo_point one
			if ( !mappingBuilder.hasPropertyAbsolute( fieldPath ) ) {
				mappingBuilder.setPropertyAbsolute( fieldPath, field );
			}
		}
		else {
			if ( SpatialHelper.isSpatialFieldLongitude( fieldPath ) ) {
				// we ignore the longitude field, we will create the geo_point mapping only once with the latitude field
				return;
			}
			else if ( SpatialHelper.isSpatialFieldLatitude( fieldPath ) ) {
				// we only add the geo_point for the latitude field
				JsonObject field = new JsonObject();

				field.addProperty( "type", ElasticsearchFieldType.GEO_POINT.getElasticsearchString() );

				// in this case, the spatial field has precedence over an already defined field
				mappingBuilder.setPropertyAbsolute( SpatialHelper.getSpatialFieldRootName( fieldPath ), field );
			}
			else {
				// the fields potentially created for the spatial hash queries
				JsonObject field = new JsonObject();
				field.addProperty( "type", ElasticsearchFieldType.STRING.getElasticsearchString() );
				field.addProperty( "index", NOT_ANALYZED );

				mappingBuilder.setPropertyAbsolute( fieldPath, field );
			}
		}
	}

	private JsonObject addFieldMapping(ElasticsearchMappingBuilder mappingBuilder, FacetMetadata facetMetadata) {
		String fullFieldName = facetMetadata.getFacetName();

		JsonObject field = new JsonObject();
		addTypeOptions( field, facetMetadata );
		field.addProperty( "store", false );
		field.addProperty( "index", NOT_ANALYZED );

		mappingBuilder.setPropertyAbsolute( fullFieldName, field );
		return field;
	}

	/**
	 * Adds the main indexing-related options to the given field: "index", "doc_values", "analyzer", ...
	 */
	private void addIndexOptions(JsonObject field, ElasticsearchMappingBuilder mappingBuilder, PropertyMetadata sourceProperty,
			String fieldName, ElasticsearchFieldType fieldType, Field.Index index, AnalyzerReference analyzerReference) {
		String elasticsearchIndex;
		switch ( index ) {
			case ANALYZED:
			case ANALYZED_NO_NORMS:
				elasticsearchIndex = canTypeBeAnalyzed( fieldType ) ? ANALYZED : NOT_ANALYZED;
				break;
			case NOT_ANALYZED:
			case NOT_ANALYZED_NO_NORMS:
				elasticsearchIndex = NOT_ANALYZED;
				break;
			case NO:
				elasticsearchIndex = NOT_INDEXED;
				break;
			default:
				throw new AssertionFailure( "Unexpected index type: " + index );
		}
		field.addProperty( "index", elasticsearchIndex );

		if ( NOT_INDEXED.equals( elasticsearchIndex ) && FieldHelper.isSortableField( mappingBuilder.getMetadata(), sourceProperty, fieldName ) ) {
			// We must use doc values in order to enable sorting on non-indexed fields
			field.addProperty( "doc_values", true );
		}

		if ( ANALYZED.equals( elasticsearchIndex ) && analyzerReference != null ) {
			String analyzerName = mappingBuilder.getAnalyzerName( analyzerReference, fieldName );
			if ( analyzerName != null ) {
				field.addProperty( "analyzer", analyzerName );
			}
		}
	}

	private boolean canTypeBeAnalyzed(ElasticsearchFieldType fieldType) {
		// Only strings can be analyzed
		return ElasticsearchFieldType.STRING.equals( fieldType );
	}

	private ElasticsearchFieldType addTypeOptions(JsonObject field, DocumentFieldMetadata fieldMetadata) {
		return addTypeOptions( fieldMetadata.getFieldName(), field, FieldHelper.getType( fieldMetadata ) );
	}

	private ElasticsearchFieldType addTypeOptions(JsonObject field, BridgeDefinedField bridgeDefinedField) {
		ExtendedFieldType type = FieldHelper.getType( bridgeDefinedField );

		if ( ExtendedFieldType.UNKNOWN.equals( type ) ) {
			throw LOG.unexpectedFieldType( bridgeDefinedField.getType().name(), bridgeDefinedField.getName() );
		}

		return addTypeOptions( bridgeDefinedField.getName(), field, FieldHelper.getType( bridgeDefinedField ) );
	}

	private ElasticsearchFieldType addTypeOptions(JsonObject field, FacetMetadata facetMetadata) {
		ExtendedFieldType type;

		switch ( facetMetadata.getEncoding() ) {
			case DOUBLE:
				type = ExtendedFieldType.DOUBLE;
				break;
			case LONG:
				type = ExtendedFieldType.LONG;
				break;
			case STRING:
				type = ExtendedFieldType.STRING;
				break;
			case AUTO:
				throw new AssertionFailure( "The facet type should have been resolved during bootstrapping" );
			default: {
				throw new AssertionFailure(
						"Unexpected facet encoding type '"
								+ facetMetadata.getEncoding()
								+ "' Has the enum been modified?"
				);
			}
		}

		return addTypeOptions( facetMetadata.getFacetName(), field, type );
	}

	private ElasticsearchFieldType addTypeOptions(String fieldName, JsonObject field, ExtendedFieldType extendedType) {
		ElasticsearchFieldType elasticsearchType;
		List<String> formats = new ArrayList<>();

		/* Note: for date formats, we use a 4-digit year format as the first format
		 * (which is the output format), so that Elasticsearch outputs are more
		 * human-readable.
		 */
		switch ( extendedType ) {
			case BOOLEAN:
				elasticsearchType = ElasticsearchFieldType.BOOLEAN;
				break;
			case CALENDAR:
			case DATE:
			case INSTANT:
				elasticsearchType = ElasticsearchFieldType.DATE;
				// Use default formats ("strict_date_optional_time||epoch_millis")
				break;
			case LOCAL_DATE:
				elasticsearchType = ElasticsearchFieldType.DATE;
				formats.add( "strict_date" );
				formats.add( "yyyyyyyyy-MM-dd" );
				break;
			case LOCAL_DATE_TIME:
				elasticsearchType = ElasticsearchFieldType.DATE;
				formats.add( "strict_date_hour_minute_second_fraction" );
				formats.add( "yyyyyyyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS" );
				break;
			case LOCAL_TIME:
				elasticsearchType = ElasticsearchFieldType.DATE;
				formats.add( "strict_hour_minute_second_fraction" );
				break;
			case OFFSET_DATE_TIME:
				elasticsearchType = ElasticsearchFieldType.DATE;
				formats.add( "strict_date_time" );
				formats.add( "yyyyyyyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSZ" );
				break;
			case OFFSET_TIME:
				elasticsearchType = ElasticsearchFieldType.DATE;
				formats.add( "strict_time" );
				break;
			case ZONED_DATE_TIME:
				elasticsearchType = ElasticsearchFieldType.DATE;
				formats.add( "yyyy-MM-dd'T'HH:mm:ss.SSSZZ'['ZZZ']'" );
				formats.add( "yyyyyyyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSZZ'['ZZZ']'" );
				break;
			case YEAR:
				elasticsearchType = ElasticsearchFieldType.DATE;
				formats.add( "strict_year" );
				formats.add( "yyyyyyyyy" );
				break;
			case YEAR_MONTH:
				elasticsearchType = ElasticsearchFieldType.DATE;
				formats.add( "strict_year_month" );
				formats.add( "yyyyyyyyy-MM" );
				break;
			case MONTH_DAY:
				elasticsearchType = ElasticsearchFieldType.DATE;
				/*
				 * This seems to be the ISO-8601 format for dates without year.
				 * It's also the default format for Java's MonthDay, see MonthDay.PARSER.
				 */
				formats.add( "--MM-dd" );
				break;
			case INTEGER:
				elasticsearchType = ElasticsearchFieldType.INTEGER;
				break;
			case LONG:
				elasticsearchType = ElasticsearchFieldType.LONG;
				break;
			case FLOAT:
				elasticsearchType = ElasticsearchFieldType.FLOAT;
				break;
			case DOUBLE:
				elasticsearchType = ElasticsearchFieldType.DOUBLE;
				break;
			case UNKNOWN_NUMERIC:
				// Likely a custom field bridge which does not expose the type of the given field; either correctly
				// so (because the given name is the default field and this bridge does not wish to use that field
				// name as is) or incorrectly; The field will not be added to the mapping, causing an exception at
				// runtime if the bridge writes that field nevertheless
				elasticsearchType = null;
				break;
			case STRING:
			case UNKNOWN:
			default:
				elasticsearchType = ElasticsearchFieldType.STRING;
				break;
		}

		if ( elasticsearchType == null ) {
			throw new IncompleteDataException( "Field type could not be determined" );
		}

		field.addProperty( "type", elasticsearchType.getElasticsearchString() );

		if ( ! formats.isEmpty() ) {
			field.addProperty( "format", StringHelper.join( formats, "||" ) );
		}

		return elasticsearchType;
	}

	private JsonElement getNullValue(ElasticsearchFieldType dataType, DocumentFieldMetadata fieldMetadata) {
		Gson gson = gsonService.getGson();
		Object convertedValue = ElasticSearchIndexNullAsHelper.getNullValue(
				fieldMetadata.getName(), dataType, fieldMetadata.indexNullAs()
				);
		return gson.toJsonTree( convertedValue );
	}

	/**
	 * Collects all the bridge-defined fields for the given type, excluding its embedded types.
	 */
	private Set<BridgeDefinedField> getNonEmbeddedBridgeDefinedFields(TypeMetadata type) {
		Set<BridgeDefinedField> bridgeDefinedFields = new HashSet<>();

		bridgeDefinedFields.addAll( type.getClassBridgeDefinedFields() );

		if ( type.getIdPropertyMetadata() != null ) {
			bridgeDefinedFields.addAll( type.getIdPropertyMetadata().getBridgeDefinedFields().values() );
		}

		for ( PropertyMetadata property : type.getAllPropertyMetadata() ) {
			bridgeDefinedFields.addAll( property.getBridgeDefinedFields().values() );
		}

		return bridgeDefinedFields;
	}

	// Getters

	@Override
	public String getIndexName() {
		return indexName;
	}

	@Override
	public ReaderProvider getReaderProvider() {
		throw LOG.indexManagerReaderProviderUnsupported();
	}

	@Override
	public Set<Class<?>> getContainedTypes() {
		return containedEntityTypes;
	}

	@Override
	public Similarity getSimilarity() {
		return similarity;
	}

	@Override
	public Analyzer getAnalyzer(String name) {
		return searchIntegrator.getAnalyzer( name );
	}

	@Override
	public LuceneWorkSerializer getSerializer() {
		return null;
	}

	@Override
	public void flushAndReleaseResources() {
		// no-op
	}

	public String getActualIndexName() {
		return actualIndexName;
	}

	public boolean needsRefreshAfterWrite() {
		return refreshAfterWrite;
	}

	// Runtime ops

	@Override
	public void performOperations(List<LuceneWork> workList, IndexingMonitor monitor) {
		List<BackendRequest<?>> requests = new ArrayList<>( workList.size() );
		for ( LuceneWork luceneWork : workList ) {
			requests.add( luceneWork.acceptIndexWorkVisitor( visitor, null ) );
		}

		requestProcessor.executeSync( requests );
	}

	@Override
	public void performStreamOperation(LuceneWork singleOperation, IndexingMonitor monitor, boolean forceAsync) {
		if ( singleOperation == FlushLuceneWork.INSTANCE ) {
			requestProcessor.awaitAsyncProcessingCompletion();
		}
		else {
			BackendRequest<?> request = singleOperation.acceptIndexWorkVisitor( visitor, null );

			if ( request != null ) {
				requestProcessor.executeAsync( request );
			}
		}
	}

	@Override
	public void optimize() {
		performStreamOperation( OptimizeLuceneWork.INSTANCE, null, false );
	}

	@Override
	public String toString() {
		return "ElasticsearchIndexManager [actualIndexName=" + actualIndexName + "]";
	}

	@Override
	public RemoteAnalyzer getRemoteAnalyzer(String name) {
		return new RemoteAnalyzer( name );
	}

	@Override
	public IndexManagerType getIndexManagerType() {
		return ElasticsearchIndexManagerType.INSTANCE;
	}

}
