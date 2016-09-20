/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.schema.impl.DefaultElasticsearchSchemaValidator;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaValidationException;
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaValidator;
import org.hibernate.search.elasticsearch.schema.impl.model.DataType;
import org.hibernate.search.elasticsearch.schema.impl.model.DynamicType;
import org.hibernate.search.elasticsearch.schema.impl.model.IndexMetadata;
import org.hibernate.search.elasticsearch.schema.impl.model.IndexType;
import org.hibernate.search.elasticsearch.schema.impl.model.PropertyMapping;
import org.hibernate.search.elasticsearch.schema.impl.model.TypeMapping;
import org.hibernate.search.elasticsearch.spi.ElasticsearchIndexManagerType;
import org.hibernate.search.elasticsearch.util.impl.FieldHelper;
import org.hibernate.search.elasticsearch.util.impl.FieldHelper.ExtendedFieldType;
import org.hibernate.search.engine.BoostStrategy;
import org.hibernate.search.engine.impl.DefaultBoostStrategy;
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
import org.hibernate.search.util.configuration.impl.ConfigurationParseHelper;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import io.searchbox.action.AbstractAction;
import io.searchbox.client.JestResult;
import io.searchbox.cluster.Health;
import io.searchbox.cluster.Health.Builder;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.DeleteIndex;
import io.searchbox.indices.IndicesExists;
import io.searchbox.indices.mapping.GetMapping;
import io.searchbox.indices.mapping.PutMapping;

/**
 * An {@link IndexManager} applying indexing work to an Elasticsearch server.
 *
 * @author Gunnar Morling
 */
public class ElasticsearchIndexManager implements IndexManager, RemoteAnalyzerProvider {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private static final TypeToken<Map<String, TypeMapping>> STRING_TO_TYPE_MAPPING_MAP_TYPE_TOKEN =
			new TypeToken<Map<String, TypeMapping>>() {
				// Create a new class to capture generic parameters
			};

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
		jestClient = null;
		serviceManager.releaseService( JestClient.class );
		gsonService = null;
		serviceManager.releaseService( GsonService.class );
	}

	@Override
	public void setSearchFactory(ExtendedSearchIntegrator boundSearchIntegrator) {
		this.searchIntegrator = boundSearchIntegrator;
		initializeIndex();
	}

	private void initializeIndex() {
		switch (indexManagementStrategy) {
			case NONE:
				break;
			case CREATE:
				if ( createIndexIfNotYetExisting() ) {
					putIndexMappings();
				}
				break;
			case RECREATE:
			case RECREATE_DELETE:
				deleteIndexIfExisting();
				createIndex();
				putIndexMappings();
				break;
			case MERGE:
				createIndexIfNotYetExisting();
				putIndexMappings();
				break;
			case VALIDATE:
				validateIndexMappings();
				break;
			default:
				break;
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
				try {
					return super.buildURI() + URLEncoder.encode(actualIndexName, AbstractAction.CHARSET);
				}
				catch (UnsupportedEncodingException e) {
					throw new AssertionFailure("Unexpectedly unsupported charset", e);
				}
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

	// TODO What happens if several nodes in a cluster try to create the mappings?
	private void putIndexMappings() {
		IndexMetadata indexMetadata = createIndexMetadata();
		for ( Map.Entry<String, TypeMapping> entry : indexMetadata.getMappings().entrySet() ) {
			String mappingName = entry.getKey();
			TypeMapping mapping = entry.getValue();

			/*
			 * Serializing nulls is really not a good idea here, it triggers NPEs in ElasticSearch
			 * We better not include the null fields.
			 */
			Gson gson = gsonService.getGsonNoSerializeNulls();
			String mappingAsJson = gson.toJson( mapping );

			PutMapping putMapping = new PutMapping.Builder(
					indexMetadata.getName(),
					mappingName,
					mappingAsJson
			)
			.build();

			try {
				jestClient.executeRequest( putMapping );
			}
			catch (RuntimeException e) {
				throw LOG.elasticsearchMappingCreationFailed( mappingName, e );
			}
		}
	}

	private void validateIndexMappings() {
		ElasticsearchSchemaValidator validator = new DefaultElasticsearchSchemaValidator();
		try {
			validator.validate( createIndexMetadata(), getCurrentIndexMetadata() );
		}
		catch (ElasticsearchSchemaValidationException e) {
			throw LOG.schemaValidationFailed( actualIndexName, e );
		}
	}

	private IndexMetadata getCurrentIndexMetadata() {
		GetMapping getMapping = new GetMapping.Builder()
				.build();

		try {
			JestResult result = jestClient.executeRequest( getMapping );
			JsonElement index = result.getJsonObject().get( actualIndexName );
			if ( index == null || !index.isJsonObject() ) {
				throw LOG.mappingsMissing( actualIndexName );
			}
			JsonElement mappings = index.getAsJsonObject().get( "mappings" );
			if ( mappings == null ) {
				throw LOG.mappingsMissing( actualIndexName );
			}
			Type mapType = STRING_TO_TYPE_MAPPING_MAP_TYPE_TOKEN.getType();
			IndexMetadata indexMetadata = new IndexMetadata();
			indexMetadata.setName( actualIndexName );
			indexMetadata.setMappings( gsonService.getGson().<Map<String, TypeMapping>>fromJson( mappings, mapType ) );
			return indexMetadata;
		}
		catch (RuntimeException e) {
			throw LOG.elasticsearchMappingRetrievalForValidationFailed( e );
		}
	}

	private IndexMetadata createIndexMetadata() {
		IndexMetadata index = new IndexMetadata();
		index.setName( actualIndexName );
		for ( Class<?> entityType : containedEntityTypes ) {
			index.putMapping( entityType.getName(), createTypeMapping( entityType ) );
		}
		return index;
	}

	private TypeMapping createTypeMapping(Class<?> entityType) {
		EntityIndexBinding descriptor = searchIntegrator.getIndexBinding( entityType );
		TypeMapping root = new TypeMapping();

		root.setDynamic( DynamicType.STRICT );

		if ( multitenancyEnabled ) {
			PropertyMapping tenantId = new PropertyMapping();
			tenantId.setType( DataType.STRING );
			tenantId.setIndex( IndexType.NOT_ANALYZED );
			root.addProperty( DocumentBuilderIndexedEntity.TENANT_ID_FIELDNAME, tenantId );
		}

		addMappings( new ElasticsearchMappingBuilder( descriptor, root ) );

		return root;
	}

	private void addMappings(ElasticsearchMappingBuilder mappingBuilder) {
		TypeMetadata typeMetadata = mappingBuilder.getMetadata();

		// normal document fields
		for ( DocumentFieldMetadata fieldMetadata : typeMetadata.getNonEmbeddedDocumentFieldMetadata() ) {
			try {
				addPropertyMapping( mappingBuilder, fieldMetadata );
			}
			catch (IncompleteDataException e) {
				LOG.debug( "Not adding a mapping for field " + fieldMetadata.getFieldName() + " because of incomplete data", e );
			}
		}

		// bridge-defined fields
		for ( BridgeDefinedField bridgeDefinedField : getNonEmbeddedBridgeDefinedFields( typeMetadata ) ) {
			try {
				addPropertyMapping( mappingBuilder, bridgeDefinedField );
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
	 * Adds a property mapping for the given field to the given type mapping.
	 */
	private void addPropertyMapping(ElasticsearchMappingBuilder mappingBuilder, DocumentFieldMetadata fieldMetadata) {
		if ( fieldMetadata.getFieldName().isEmpty() || fieldMetadata.getFieldName().endsWith( "." )
				|| fieldMetadata.isSpatial() ) {
			return;
		}

		String propertyPath = fieldMetadata.getName();

		PropertyMapping propertyMapping = new PropertyMapping();

		addTypeOptions( propertyMapping, fieldMetadata );

		propertyMapping.setStore( fieldMetadata.getStore() == Store.NO ? false : true );

		addIndexOptions( propertyMapping, mappingBuilder, fieldMetadata.getSourceProperty(), fieldMetadata.getFieldName(),
				fieldMetadata.getIndex(), fieldMetadata.getAnalyzerReference() );

		propertyMapping.setBoost( mappingBuilder.getBoost( fieldMetadata.getBoost() ) );

		logDynamicBoostWarning( mappingBuilder, fieldMetadata.getSourceType().getDynamicBoost(), propertyPath );
		PropertyMetadata sourceProperty = fieldMetadata.getSourceProperty();
		if ( sourceProperty != null ) {
			logDynamicBoostWarning( mappingBuilder, sourceProperty.getDynamicBoostStrategy(), propertyPath );
		}

		addNullValue( propertyMapping, fieldMetadata );

		// Create facet fields if needed: if the facet has the same name as the field, we don't need to create an
		// extra field for it
		for ( FacetMetadata facetMetadata : fieldMetadata.getFacetMetadata() ) {
			if ( !facetMetadata.getFacetName().equals( fieldMetadata.getFieldName() ) ) {
				try {
					addPropertyMapping( mappingBuilder, facetMetadata );
				}
				catch (IncompleteDataException e) {
					LOG.debug( "Not adding a mapping for facet " + facetMetadata.getFacetName() + " because of incomplete data", e );
				}
			}
		}

		// Do this last, when we're sure no exception will be thrown for this mapping
		mappingBuilder.setPropertyAbsolute( propertyPath, propertyMapping );
	}

	private void logDynamicBoostWarning(ElasticsearchMappingBuilder mappingBuilder, BoostStrategy dynamicBoostStrategy, String fieldPath) {
		if ( dynamicBoostStrategy != null && !DefaultBoostStrategy.INSTANCE.equals( dynamicBoostStrategy ) ) {
			LOG.unsupportedDynamicBoost( dynamicBoostStrategy.getClass(), mappingBuilder.getBeanClass(), fieldPath );
		}
	}

	/**
	 * Adds a type mapping for the given field to the given request payload.
	 */
	private void addPropertyMapping(ElasticsearchMappingBuilder mappingBuilder, BridgeDefinedField bridgeDefinedField) {
		String propertyPath = bridgeDefinedField.getName();

		if ( !SpatialHelper.isSpatialField( propertyPath ) ) {
			PropertyMapping propertyMapping = new PropertyMapping();
			addTypeOptions( propertyMapping, bridgeDefinedField );
			addIndexOptions( propertyMapping, mappingBuilder, bridgeDefinedField.getSourceField().getSourceProperty(),
					propertyPath, bridgeDefinedField.getIndex(), null );

			// we don't overwrite already defined fields. Typically, in the case of spatial, the geo_point field
			// is defined before the double field and we want to keep the geo_point one
			if ( !mappingBuilder.hasPropertyAbsolute( propertyPath ) ) {
				mappingBuilder.setPropertyAbsolute( propertyPath, propertyMapping );
			}
		}
		else {
			if ( SpatialHelper.isSpatialFieldLongitude( propertyPath ) ) {
				// we ignore the longitude field, we will create the geo_point mapping only once with the latitude field
				return;
			}
			else if ( SpatialHelper.isSpatialFieldLatitude( propertyPath ) ) {
				// we only add the geo_point for the latitude field
				PropertyMapping propertyMapping = new PropertyMapping();

				propertyMapping.setType( DataType.GEO_POINT );

				// in this case, the spatial field has precedence over an already defined field
				mappingBuilder.setPropertyAbsolute( SpatialHelper.stripSpatialFieldSuffix( propertyPath ), propertyMapping );
			}
			else {
				// the fields potentially created for the spatial hash queries
				PropertyMapping propertyMapping = new PropertyMapping();
				propertyMapping.setType( DataType.STRING );
				propertyMapping.setIndex( IndexType.NOT_ANALYZED );

				mappingBuilder.setPropertyAbsolute( propertyPath, propertyMapping );
			}
		}
	}

	private void addPropertyMapping(ElasticsearchMappingBuilder mappingBuilder, FacetMetadata facetMetadata) {
		String propertyPath = facetMetadata.getFacetName();

		PropertyMapping propertyMapping = new PropertyMapping();

		addTypeOptions( propertyMapping, facetMetadata );
		propertyMapping.setStore( false );
		propertyMapping.setIndex( IndexType.NOT_ANALYZED );

		// Do this last, when we're sure no exception will be thrown for this mapping
		mappingBuilder.setPropertyAbsolute( propertyPath, propertyMapping );
	}

	/**
	 * Adds the main indexing-related options to the given field: "index", "doc_values", "analyzer", ...
	 */
	private void addIndexOptions(PropertyMapping propertyMapping, ElasticsearchMappingBuilder mappingBuilder, PropertyMetadata sourceProperty,
			String propertyPath, Field.Index index, AnalyzerReference analyzerReference) {
		IndexType elasticsearchIndex;
		switch ( index ) {
			case ANALYZED:
			case ANALYZED_NO_NORMS:
				elasticsearchIndex = canTypeBeAnalyzed( propertyMapping.getType() ) ? IndexType.ANALYZED : IndexType.NOT_ANALYZED;
				break;
			case NOT_ANALYZED:
			case NOT_ANALYZED_NO_NORMS:
				elasticsearchIndex = IndexType.NOT_ANALYZED;
				break;
			case NO:
				elasticsearchIndex = IndexType.NO;
				break;
			default:
				throw new AssertionFailure( "Unexpected index type: " + index );
		}
		propertyMapping.setIndex( elasticsearchIndex );

		if ( IndexType.NO.equals( elasticsearchIndex ) && FieldHelper.isSortableField( mappingBuilder.getMetadata(), sourceProperty, propertyPath ) ) {
			// We must use doc values in order to enable sorting on non-indexed fields
			propertyMapping.setDocValues( true );
		}

		if ( IndexType.ANALYZED.equals( elasticsearchIndex ) && analyzerReference != null ) {
			String analyzerName = mappingBuilder.getAnalyzerName( analyzerReference, propertyPath );
			propertyMapping.setAnalyzer( analyzerName );
		}
	}

	private boolean canTypeBeAnalyzed(DataType fieldType) {
		return DataType.STRING.equals( fieldType );
	}

	private void addTypeOptions(PropertyMapping propertyMapping, DocumentFieldMetadata fieldMetadata) {
		addTypeOptions( fieldMetadata.getFieldName(), propertyMapping, FieldHelper.getType( fieldMetadata ) );
	}

	private void addTypeOptions(PropertyMapping propertyMapping, BridgeDefinedField bridgeDefinedField) {
		ExtendedFieldType type = FieldHelper.getType( bridgeDefinedField );

		if ( ExtendedFieldType.UNKNOWN.equals( type ) ) {
			throw LOG.unexpectedFieldType( bridgeDefinedField.getType().name(), bridgeDefinedField.getName() );
		}

		addTypeOptions( bridgeDefinedField.getName(), propertyMapping, type );
	}

	private void addTypeOptions(PropertyMapping propertyMapping, FacetMetadata facetMetadata) {
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

		addTypeOptions( facetMetadata.getFacetName(), propertyMapping, type );
	}

	private DataType addTypeOptions(String fieldName, PropertyMapping propertyMapping, ExtendedFieldType extendedType) {
		DataType elasticsearchType;
		List<String> formats = new ArrayList<>();

		/* Note: for date formats, we use a 4-digit year format as the first format
		 * (which is the output format), so that Elasticsearch outputs are more
		 * human-readable.
		 */
		switch ( extendedType ) {
			case BOOLEAN:
				elasticsearchType = DataType.BOOLEAN;
				break;
			case CALENDAR:
			case DATE:
			case INSTANT:
				elasticsearchType = DataType.DATE;
				// Use default formats ("strict_date_optional_time||epoch_millis")
				break;
			case LOCAL_DATE:
				elasticsearchType = DataType.DATE;
				formats.add( "strict_date" );
				formats.add( "yyyyyyyyy-MM-dd" );
				break;
			case LOCAL_DATE_TIME:
				elasticsearchType = DataType.DATE;
				formats.add( "strict_date_hour_minute_second_fraction" );
				formats.add( "yyyyyyyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS" );
				break;
			case LOCAL_TIME:
				elasticsearchType = DataType.DATE;
				formats.add( "strict_hour_minute_second_fraction" );
				break;
			case OFFSET_DATE_TIME:
				elasticsearchType = DataType.DATE;
				formats.add( "strict_date_time" );
				formats.add( "yyyyyyyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSZ" );
				break;
			case OFFSET_TIME:
				elasticsearchType = DataType.DATE;
				formats.add( "strict_time" );
				break;
			case ZONED_DATE_TIME:
				elasticsearchType = DataType.DATE;
				formats.add( "yyyy-MM-dd'T'HH:mm:ss.SSSZZ'['ZZZ']'" );
				formats.add( "yyyyyyyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSZZ'['ZZZ']'" );
				break;
			case YEAR:
				elasticsearchType = DataType.DATE;
				formats.add( "strict_year" );
				formats.add( "yyyyyyyyy" );
				break;
			case YEAR_MONTH:
				elasticsearchType = DataType.DATE;
				formats.add( "strict_year_month" );
				formats.add( "yyyyyyyyy-MM" );
				break;
			case MONTH_DAY:
				elasticsearchType = DataType.DATE;
				/*
				 * This seems to be the ISO-8601 format for dates without year.
				 * It's also the default format for Java's MonthDay, see MonthDay.PARSER.
				 */
				formats.add( "--MM-dd" );
				break;
			case INTEGER:
				elasticsearchType = DataType.INTEGER;
				break;
			case LONG:
				elasticsearchType = DataType.LONG;
				break;
			case FLOAT:
				elasticsearchType = DataType.FLOAT;
				break;
			case DOUBLE:
				elasticsearchType = DataType.DOUBLE;
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
				elasticsearchType = DataType.STRING;
				break;
		}

		if ( elasticsearchType == null ) {
			throw new IncompleteDataException( "Field type could not be determined" );
		}

		propertyMapping.setType( elasticsearchType );

		if ( !formats.isEmpty() ) {
			propertyMapping.setFormat( formats );
		}

		return elasticsearchType;
	}

	private void addNullValue(PropertyMapping propertyMapping, DocumentFieldMetadata fieldMetadata) {
		String indexNullAs = fieldMetadata.indexNullAs();
		if ( indexNullAs != null ) {
			Object convertedValue = ElasticSearchIndexNullAsHelper.getNullValue(
					fieldMetadata.getName(), propertyMapping.getType(), indexNullAs
					);
			Gson gson = gsonService.getGson();
			propertyMapping.setNullValue( gson.toJsonTree( convertedValue ).getAsJsonPrimitive() );
		}
	}

	/**
	 * Collects all the bridge-defined fields for the given type, excluding its embedded types.
	 */
	private Set<BridgeDefinedField> getNonEmbeddedBridgeDefinedFields(TypeMetadata type) {
		Set<BridgeDefinedField> bridgeDefinedFields = new HashSet<>();
		for ( DocumentFieldMetadata documentFieldMetadata : type.getNonEmbeddedDocumentFieldMetadata() ) {
			bridgeDefinedFields.addAll( documentFieldMetadata.getBridgeDefinedFields().values() );
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
