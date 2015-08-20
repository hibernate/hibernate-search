/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.impl;

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
import org.hibernate.search.analyzer.impl.RemoteAnalyzerReference;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.backend.BackendFactory;
import org.hibernate.search.backend.FlushLuceneWork;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.backend.elasticsearch.cfg.IndexManagementStrategy;
import org.hibernate.search.backend.elasticsearch.client.impl.BackendRequest;
import org.hibernate.search.backend.elasticsearch.client.impl.BackendRequestProcessor;
import org.hibernate.search.backend.elasticsearch.client.impl.JestClient;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.spi.ElasticsearchIndexManagerType;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.metadata.impl.BridgeDefinedField;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
import org.hibernate.search.engine.metadata.impl.FacetMetadata;
import org.hibernate.search.engine.metadata.impl.PropertyMetadata;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.serialization.impl.LuceneWorkSerializerImpl;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.indexes.serialization.spi.SerializationProvider;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.indexes.spi.IndexManagerType;
import org.hibernate.search.indexes.spi.ReaderProvider;
import org.hibernate.search.metadata.NumericFieldSettingsDescriptor.NumericEncodingType;
import org.hibernate.search.spatial.impl.SpatialHelper;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.logging.impl.LoggerFactory;

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

	private String indexName;
	private String actualIndexName;
	private IndexManagementStrategy indexManagementStrategy;
	private String indexManagementWaitTimeout;

	/**
	 * Status the index needs to be at least in, otherwise we'll fail starting up.
	 */
	private IndexStatus requiredIndexStatus;

	private Similarity similarity;

	private ExtendedSearchIntegrator searchIntegrator;
	private final Set<Class<?>> containedEntityTypes = new HashSet<>();

	private BackendQueueProcessor backend;

	private LuceneWorkSerializer serializer;
	private SerializationProvider serializationProvider;
	private ServiceManager serviceManager;

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

			throw new SearchException( "Unexpected index status: " + status + ". Specify one of 'green', 'yellow' or 'red'." );
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

		this.similarity = similarity;

		this.jestClient = serviceManager.requestService( JestClient.class );
		this.backend = BackendFactory.createBackend( this, context, properties );

		this.visitor = new ElasticsearchIndexWorkVisitor( this.actualIndexName, context.getUninitializedSearchIntegrator() );
		this.requestProcessor = context.getServiceManager().requestService( BackendRequestProcessor.class );
	}

	private String getIndexName(String indexName, Properties properties) {
		String name = properties.getProperty( Environment.INDEX_NAME_PROP_NAME );
		return name != null ? name : indexName;
	}

	private IndexManagementStrategy getIndexManagementStrategy(Properties properties) {
		String strategy = properties.getProperty( ElasticsearchEnvironment.INDEX_MANAGEMENT_STRATEGY );
		return strategy != null ? IndexManagementStrategy.valueOf( strategy ) : ElasticsearchEnvironment.Defaults.INDEX_MANAGEMENT_STRATEGY;
	}

	private String getIndexManagementWaitTimeout(Properties properties) {
		int timeout = ConfigurationParseHelper.getIntValue(
				properties,
				ElasticsearchEnvironment.INDEX_MANAGEMENT_WAIT_TIMEOUT,
				ElasticsearchEnvironment.Defaults.INDEX_MANAGEMENT_WAIT_TIMEOUT
		);

		if ( timeout < 0 ) {
			throw new SearchException( "Positive timeout value expected, but it was: " + timeout );
		}

		return timeout + "ms";
	}

	private IndexStatus getRequiredIndexStatus(Properties properties) {
		String status = ConfigurationParseHelper.getString(
				properties,
				ElasticsearchEnvironment.REQUIRED_INDEX_STATUS,
				ElasticsearchEnvironment.Defaults.REQUIRED_INDEX_STATUS
		);

		return IndexStatus.fromString( status );
	}

	@Override
	public void destroy() {
		if ( indexManagementStrategy == IndexManagementStrategy.CREATE_DELETE ) {
			deleteIndexIfExisting();
		}

		backend.close();

		searchIntegrator.getServiceManager().releaseService( JestClient.class );
	}

	@Override
	public void setSearchFactory(ExtendedSearchIntegrator boundSearchIntegrator) {
		this.searchIntegrator = boundSearchIntegrator;
		initializeIndex();
	}

	private void initializeIndex() {
		if ( indexManagementStrategy == IndexManagementStrategy.NONE ) {
			return;
		}
		else if ( indexManagementStrategy == IndexManagementStrategy.CREATE ||
				indexManagementStrategy == IndexManagementStrategy.CREATE_DELETE ) {

			deleteIndexIfExisting();
			createIndex();
			createIndexMappings();
		}
		else if ( indexManagementStrategy == IndexManagementStrategy.MERGE ) {
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
			throw new SearchException( "Index '" + actualIndexName + "' has status '" + status + "', but it is expected to be '"
					+ requiredIndexStatus.getElasticsearchString() + "'." );
		}
	}

	private void createIndexIfNotYetExisting() {
		if ( jestClient.executeRequest( new IndicesExists.Builder( actualIndexName ).build(), 404 ).getResponseCode() == 200 ) {
			return;
		}

		jestClient.executeRequest( new CreateIndex.Builder( actualIndexName ).build() );
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

			// Add field for tenant id though we don't know at this point if it's actually going to be needed.
			// TODO HSEARCH-2256 Should we make this configurable?
			JsonObject field = new JsonObject();
			field.addProperty( "type", "string" );
			field.addProperty( "index", NOT_ANALYZED );
			properties.add( DocumentBuilderIndexedEntity.TENANT_ID_FIELDNAME, field );

			// normal document fields
			for ( DocumentFieldMetadata fieldMetadata : descriptor.getDocumentBuilder().getTypeMetadata().getAllDocumentFieldMetadata() ) {
				if ( fieldMetadata.isId() || fieldMetadata.getFieldName().isEmpty() || fieldMetadata.getFieldName().endsWith( "." )
						|| fieldMetadata.isSpatial() ) {
					continue;
				}

				addFieldMapping( payload, descriptor, fieldMetadata );
			}

			// bridge-defined fields
			for ( BridgeDefinedField bridgeDefinedField : getAllBridgeDefinedFields( descriptor ) ) {
				addFieldMapping( payload, bridgeDefinedField );
			}

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
				throw new SearchException( "Could not create mapping for entity type " + entityType.getName(), e );
			}
		}
	}

	private String analyzerName(Class<?> entityType, DocumentFieldMetadata fieldMetadata) {
		AnalyzerReference analyzerReference = fieldMetadata.getAnalyzerReference();
		if ( analyzerReference.is( RemoteAnalyzerReference.class ) ) {
			return analyzerReference.unwrap( RemoteAnalyzerReference.class ).getAnalyzer().getName( fieldMetadata.getFieldName() );
		}
		LOG.analyzerIsNotRemote( entityType, fieldMetadata.getFieldName(), analyzerReference );
		return null;
	}

	/**
	 * Adds a type mapping for the given field to the given request payload.
	 */
	private void addFieldMapping(JsonObject payload, EntityIndexBinding descriptor, DocumentFieldMetadata fieldMetadata) {
		String simpleFieldName = FieldHelper.getEmbeddedFieldPropertyName( fieldMetadata.getName() );
		JsonObject field = new JsonObject();

		String fieldType = getFieldType( descriptor, fieldMetadata );
		if ( fieldType == null ) {
			LOG.debug( "Not adding a mapping for field " + fieldMetadata.getFieldName() + " as its type could not be determined" );
			return;
		}

		field.addProperty( "type", fieldType );
		field.addProperty( "store", fieldMetadata.getStore() == Store.NO ? false : true );

		String index = getIndex( descriptor, fieldMetadata );
		field.addProperty( "index", index );

		String analyzerName = getAnalyzerName( descriptor, fieldMetadata, index );
		if ( analyzerName != null ) {
			field.addProperty( "analyzer", analyzerName );
		}

		if ( fieldMetadata.getBoost() != null ) {
			field.addProperty( "boost", fieldMetadata.getBoost() );
		}

		if ( fieldMetadata.indexNullAs() != null ) {
			// TODO HSEARCH-2262 Validate the type; Supported types are converted transparently by ES
			field.addProperty( "null_value", fieldMetadata.indexNullAs() );
		}

		getOrCreateProperties( payload, fieldMetadata.getName() ).add( simpleFieldName, field );

		// Create facet fields if needed: if the facet has the same name as the field, we don't need to create an
		// extra field for it
		for ( FacetMetadata facetMetadata : fieldMetadata.getFacetMetadata() ) {
			if ( !facetMetadata.getFacetName().equals( fieldMetadata.getFieldName() ) ) {
				addFieldMapping( payload, facetMetadata );
			}
		}
	}

	private String getAnalyzerName(EntityIndexBinding descriptor, DocumentFieldMetadata fieldMetadata, String index) {
		if ( isAnalyzed( index ) && fieldMetadata.getAnalyzerReference() != null ) {
			return analyzerName( descriptor.getDocumentBuilder().getBeanClass(), fieldMetadata );
		}
		return null;
	}

	private boolean isAnalyzed(String index) {
		return ANALYZED.equals( index );
	}

	/**
	 * Adds a type mapping for the given field to the given request payload.
	 */
	private void addFieldMapping(JsonObject payload, BridgeDefinedField bridgeDefinedField) {
		String fieldName = bridgeDefinedField.getName();
		String simpleFieldName = FieldHelper.getEmbeddedFieldPropertyName( fieldName );
		if ( !SpatialHelper.isSpatialField( simpleFieldName ) ) {
			JsonObject field = new JsonObject();

			field.addProperty( "type", getFieldType( bridgeDefinedField ) );

			String index = getIndex( bridgeDefinedField );
			field.addProperty( "index", index );

			// we don't overwrite already defined fields. Typically, in the case of spatial, the geo_point field
			// is defined before the double field and we want to keep the geo_point one
			JsonObject parent = getOrCreateProperties( payload, fieldName );
			if ( !parent.has( simpleFieldName ) ) {
				parent.add( simpleFieldName, field );
			}
		}
		else {
			if ( SpatialHelper.isSpatialFieldLongitude( simpleFieldName ) ) {
				// we ignore the longitude field, we will create the geo_point mapping only once with the latitude field
				return;
			}
			else if ( SpatialHelper.isSpatialFieldLatitude( simpleFieldName ) ) {
				// we only add the geo_point for the latitude field
				JsonObject field = new JsonObject();

				field.addProperty( "type", "geo_point" );

				// in this case, the spatial field has precedence over an already defined field
				getOrCreateProperties( payload, fieldName ).add( SpatialHelper.getSpatialFieldRootName( simpleFieldName ), field );
			}
			else {
				// the fields potentially created for the spatial hash queries
				JsonObject field = new JsonObject();
				field.addProperty( "type", "string" );
				field.addProperty( "index", NOT_ANALYZED );

				getOrCreateProperties( payload, fieldName ).add( fieldName, field );
			}
		}
	}

	private JsonObject addFieldMapping(JsonObject payload, FacetMetadata facetMetadata) {
		String simpleFieldName = FieldHelper.getEmbeddedFieldPropertyName( facetMetadata.getFacetName() );
		String fullFieldName = facetMetadata.getFacetName();

		JsonObject field = new JsonObject();
		field.addProperty( "type", getFieldType( facetMetadata ) );
		field.addProperty( "store", false );
		field.addProperty( "index", NOT_ANALYZED );

		getOrCreateProperties( payload, fullFieldName).add( simpleFieldName, field );
		return field;
	}

	private String getIndex(EntityIndexBinding binding, DocumentFieldMetadata fieldMetadata) {
		// Never analyze boolean, date or numeric
		if ( FieldHelper.isBoolean( binding, fieldMetadata.getName() ) ||
				FieldHelper.isDate( binding, fieldMetadata.getName() ) ||
				FieldHelper.isCalendar( binding, fieldMetadata.getName() ) ||
				FieldHelper.isNumeric(fieldMetadata) ) {
			return NOT_ANALYZED;
		}

		return getIndexValue( fieldMetadata.getIndex() );
	}

	@SuppressWarnings("deprecation")
	private String getIndex(BridgeDefinedField bridgeDefinedField) {
		// Never analyze boolean
		if ( FieldHelper.isBoolean( bridgeDefinedField )
				|| FieldHelper.isNumeric( bridgeDefinedField )
				|| FieldHelper.isDate( bridgeDefinedField ) ) {
			return NOT_ANALYZED;
		}

		Field.Index index = bridgeDefinedField.getIndex();
		return getIndexValue( index );
	}

	private String getIndexValue(Field.Index index) {
		switch ( index ) {
			case ANALYZED:
			case ANALYZED_NO_NORMS:
				return ANALYZED;
			case NOT_ANALYZED:
			case NOT_ANALYZED_NO_NORMS:
				return NOT_ANALYZED;
			case NO:
				return "no";
			default:
				throw new IllegalArgumentException( "Unexpected index type: " + index );
		}
	}

	private String getFieldType(EntityIndexBinding descriptor, DocumentFieldMetadata fieldMetadata) {
		String type;

		if ( FieldHelper.isBoolean( descriptor, fieldMetadata.getName() ) ) {
			type = "boolean";
		}
		else if ( FieldHelper.isDate( descriptor, fieldMetadata.getName() ) ||
				FieldHelper.isCalendar( descriptor, fieldMetadata.getName() ) ) {
			type = "date";
		}
		else if ( FieldHelper.isNumeric( fieldMetadata ) ) {

			NumericEncodingType numericEncodingType = FieldHelper.getNumericEncodingType( descriptor, fieldMetadata );

			switch ( numericEncodingType ) {
				case INTEGER:
					type = "integer";
					break;
				case LONG:
					type = "long";
					break;
				case FLOAT:
					type = "float";
					break;
				case DOUBLE:
					type = "double";
					break;
				default:
					// Likely a custom field bridge which does not expose the type of the given field; either correctly
					// so (because the given name is the default field and this bridge does not wish to use that field
					// name as is) or incorrectly; The field will not be added to the mapping, causing an exception at
					// runtime if the bridge writes that field nevertheless
					type = null;
			}
		}
		else {
			type = "string";
		}

		return type;
	}

	private String getFieldType(BridgeDefinedField bridgeDefinedField) {
		switch ( bridgeDefinedField.getType() ) {
			case BOOLEAN:
				return "boolean";
			case DATE:
				return "date";
			case FLOAT:
				return "float";
			case DOUBLE:
				return "double";
			case INTEGER:
				return "integer";
			case LONG:
				return "long";
			case STRING:
				return "string";
			default:
				throw new SearchException( "Unexpected field type: " + bridgeDefinedField.getType() );
		}
	}

	private String getFieldType(FacetMetadata facetMetadata) {
		switch ( facetMetadata.getEncoding() ) {
			case DOUBLE:
				return "double";
			case LONG:
				return "long";
			case STRING:
				return "string";
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
	}

	private JsonObject getOrCreateProperties(JsonObject mapping, String fieldName) {
		if ( !FieldHelper.isEmbeddedField( fieldName ) ) {
			return mapping.getAsJsonObject( "properties" );
		}

		JsonObject parentProperties = mapping.getAsJsonObject( "properties" );


		String[] parts = fieldName.split( "\\." );
		for ( int i = 0; i < parts.length - 1; i++ ) {
			String part = parts[i];
			JsonObject property = parentProperties.getAsJsonObject( part );
			if ( property == null ) {
				property = new JsonObject();

				// TODO HSEARCH-2263 enable nested mapping as needed:
				// * only needed for embedded *-to-many with more than one field
				// * for these, the user should be able to opt out (nested would be the safe default mapping in this
				// case, but they could want to opt out when only ever querying on single fields of the embeddable)

//				property.addProperty( "type", "nested" );

				JsonObject properties = new JsonObject();
				property.add( "properties", properties );
				parentProperties.add( part, property );
				parentProperties = properties;
			}
			else {
				parentProperties = property.getAsJsonObject( "properties" );
			}
		}

		return parentProperties;
	}

	/**
	 * Recursively collects all the bridge-defined fields for the given type and its embeddables.
	 */
	private Set<BridgeDefinedField> getAllBridgeDefinedFields(EntityIndexBinding binding) {
		Set<BridgeDefinedField> bridgeDefinedFields = new HashSet<>();
		collectPropertyLevelBridgeDefinedFields( binding.getDocumentBuilder().getMetadata(), bridgeDefinedFields );
		return bridgeDefinedFields;
	}

	private void collectPropertyLevelBridgeDefinedFields(TypeMetadata type, Set<BridgeDefinedField> allBridgeDefinedFields) {
		allBridgeDefinedFields.addAll( type.getClassBridgeDefinedFields() );

		if ( type.getIdPropertyMetadata() != null ) {
			allBridgeDefinedFields.addAll( type.getIdPropertyMetadata().getBridgeDefinedFields().values() );
		}

		for ( PropertyMetadata property : type.getAllPropertyMetadata() ) {
			allBridgeDefinedFields.addAll( property.getBridgeDefinedFields().values() );
		}

		for ( TypeMetadata embeddedType : type.getEmbeddedTypeMetadata() ) {
			collectPropertyLevelBridgeDefinedFields( embeddedType, allBridgeDefinedFields );
		}
	}

	// Getters

	@Override
	public String getIndexName() {
		return indexName;
	}

	@Override
	public ReaderProvider getReaderProvider() {
		throw new UnsupportedOperationException( "No ReaderProvider / IndexReader with ES" );
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
		if ( serializer == null ) {
			serializationProvider = requestSerializationProvider();
			serializer = new LuceneWorkSerializerImpl( serializationProvider, searchIntegrator );
			LOG.indexManagerUsesSerializationService( this.indexName, this.serializer.describeSerializer() );
		}
		return serializer;
	}

	@Override
	public void flushAndReleaseResources() {
		// no-op
	}

	private SerializationProvider requestSerializationProvider() {
		try {
			return serviceManager.requestService( SerializationProvider.class );
		}
		catch (SearchException se) {
			throw LOG.serializationProviderNotFoundException( se );
		}
	}

	public String getActualIndexName() {
		return actualIndexName;
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
	public BackendQueueProcessor getBackendQueueProcessor() {
		return backend;
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
