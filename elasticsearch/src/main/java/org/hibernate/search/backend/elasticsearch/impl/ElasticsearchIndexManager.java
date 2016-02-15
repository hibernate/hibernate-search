/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.similarities.Similarity;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.backend.BackendFactory;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.backend.elasticsearch.cfg.IndexManagementStrategy;
import org.hibernate.search.backend.elasticsearch.client.impl.JestClient;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.metadata.impl.BridgeDefinedField;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
import org.hibernate.search.engine.metadata.impl.PropertyMetadata;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.spi.ServiceReference;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.serialization.impl.LuceneWorkSerializerImpl;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.indexes.serialization.spi.SerializationProvider;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.indexes.spi.ReaderProvider;
import org.hibernate.search.metadata.NumericFieldSettingsDescriptor.NumericEncodingType;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.DeleteIndex;
import io.searchbox.indices.IndicesExists;
import io.searchbox.indices.mapping.PutMapping;

/**
 * An {@link IndexManager} applying indexing work to an Elasticsearch server.
 *
 * @author Gunnar Morling
 */
public class ElasticsearchIndexManager implements IndexManager {

	private static final Log LOG = LoggerFactory.make();

	private String indexName;
	private String actualIndexName;
	private IndexManagementStrategy indexManagementStrategy;
	private Similarity similarity;

	ExtendedSearchIntegrator searchIntegrator;
	private final Set<Class<?>> containedEntityTypes = new HashSet<>();

	private ServiceReference<JestClient> clientReference;
	private BackendQueueProcessor backend;

	private LuceneWorkSerializer serializer;
	private SerializationProvider serializationProvider;
	private ServiceManager serviceManager;

	// Lifecycle

	@Override
	public void initialize(String indexName, Properties properties, Similarity similarity, WorkerBuildContext context) {
		this.serviceManager = context.getServiceManager();
		this.indexName = getIndexName( indexName, properties );
		try ( ServiceReference<ConfigurationPropertiesProvider> propertiesProvider = serviceManager.requestReference( ConfigurationPropertiesProvider.class ) ) {
			this.indexManagementStrategy = getIndexManagementStrategy( propertiesProvider.get().getProperties() );
		}
		this.actualIndexName = IndexNameNormalizer.getElasticsearchIndexName( this.indexName );
		this.similarity = similarity;
		this.backend = BackendFactory.createBackend( this, context, properties );
	}

	private String getIndexName(String indexName, Properties properties) {
		String name = properties.getProperty( Environment.INDEX_NAME_PROP_NAME );
		return name != null ? name : indexName;
	}

	private IndexManagementStrategy getIndexManagementStrategy(Properties properties) {
		String strategy = properties.getProperty( ElasticsearchEnvironment.INDEX_MANAGEMENT_STRATEGY );
		return strategy != null ? IndexManagementStrategy.valueOf( strategy ) : IndexManagementStrategy.NONE;
	}

	@Override
	public void destroy() {
		if ( indexManagementStrategy == IndexManagementStrategy.CREATE_DELETE ) {
			deleteIndexIfExisting();
		}

		clientReference.close();
	}

	@Override
	public void setSearchFactory(ExtendedSearchIntegrator boundSearchIntegrator) {
		this.searchIntegrator = boundSearchIntegrator;
		this.clientReference = searchIntegrator.getServiceManager().requestReference( JestClient.class );

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
		clientReference.get().executeRequest( new CreateIndex.Builder( actualIndexName ).build() );
	}

	private void createIndexIfNotYetExisting() {
		if ( clientReference.get().executeRequest( new IndicesExists.Builder( actualIndexName ).build(), false ).getResponseCode() == 200 ) {
			return;
		}

		clientReference.get().executeRequest( new CreateIndex.Builder( actualIndexName ).build() );
	}

	private void deleteIndexIfExisting() {
		// Not actually needed, but do it to avoid cluttering the ES log
		if ( clientReference.get().executeRequest( new IndicesExists.Builder( actualIndexName ).build(), false ).getResponseCode() == 404 ) {
			return;
		}

		try {
			clientReference.get().executeRequest( new DeleteIndex.Builder( actualIndexName ).build() );
		}
		catch (SearchException e) {
			// ignoring deletion of non-existing index
			if ( !e.getMessage().contains( "index_not_found_exception" ) ) {
				throw e;
			}
		}
	}

	// TODO
	// What happens if mappings already exist? We need an option similar to hbm2ddl
	// What happens if several nodes in a cluster try to create the mappings?
	private void createIndexMappings() {
		for ( Class<?> entityType : containedEntityTypes ) {
			EntityIndexBinding descriptor = searchIntegrator.getIndexBinding( entityType );

			JsonObject payload = new JsonObject();
			payload.addProperty( "dynamic", "strict" );
			JsonObject properties = new JsonObject();
			payload.add( "properties", properties );

			// Add field for tenant id
			// TODO At this point we don't know yet whether it's actually going to be needed
			// Should we make this configurable?
			JsonObject field = new JsonObject();
			field.addProperty( "type", "string" );
			properties.add( DocumentBuilderIndexedEntity.TENANT_ID_FIELDNAME, field );

			// normal document fields
			for ( DocumentFieldMetadata fieldMetadata : descriptor.getDocumentBuilder().getTypeMetadata().getAllDocumentFieldMetadata() ) {
				if ( fieldMetadata.isId() || fieldMetadata.getFieldName().isEmpty() || fieldMetadata.getFieldName().endsWith( "." ) ) {
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
				clientReference.get().executeRequest( putMapping );
			}
			catch (Exception e) {
				throw new SearchException( "Could not create mapping for entity type " + entityType.getName(), e );
			}
		}
	}

	/**
	 * Adds a type mapping for the given field to the given request payload.
	 */
	private void addFieldMapping(JsonObject payload, EntityIndexBinding descriptor, DocumentFieldMetadata fieldMetadata) {
		String simpleFieldName = fieldMetadata.getName().substring( fieldMetadata.getName().lastIndexOf( "." ) + 1 );
		JsonObject field = new JsonObject();

		String fieldType = getFieldType( descriptor, fieldMetadata );
		if ( fieldType == null ) {
			LOG.debug( "Not adding a mapping for field " + fieldMetadata.getFieldName() + " as its type could not be determined" );
			return;
		}

		field.addProperty( "type", fieldType );
		field.addProperty( "store", fieldMetadata.getStore() == Store.NO ? false : true );
		field.addProperty( "index", getIndex( descriptor, fieldMetadata ) );

		if ( fieldMetadata.getBoost() != null ) {
			field.addProperty( "boost", fieldMetadata.getBoost() );
		}

		if ( fieldMetadata.indexNullAs() != null ) {
			// TODO Validate the type; Supported types are converted transparently by ES
			field.addProperty( "null_value", fieldMetadata.indexNullAs() );
		}

		getOrCreateProperties( payload, fieldMetadata.getName() ).add( simpleFieldName, field );
	}

	/**
	 * Adds a type mapping for the given field to the given request payload.
	 */
	private JsonObject addFieldMapping(JsonObject payload, BridgeDefinedField bridgeDefinedField) {
		String simpleFieldName = bridgeDefinedField.getName().substring( bridgeDefinedField.getName().lastIndexOf( "." ) + 1 );
		JsonObject field = new JsonObject();

		field.addProperty( "type", getFieldType( bridgeDefinedField ) );
		field.addProperty( "index", "analyzed" );

		getOrCreateProperties( payload, bridgeDefinedField.getName() ).add( simpleFieldName, field );
		return field;
	}

	@SuppressWarnings("deprecation")
	private String getIndex(EntityIndexBinding binding, DocumentFieldMetadata fieldMetadata) {
		// Never analyze boolean
		if ( FieldHelper.isBoolean( binding, fieldMetadata.getName() ) ) {
			return "not_analyzed";
		}

		switch ( fieldMetadata.getIndex() ) {
			case ANALYZED:
			case ANALYZED_NO_NORMS:
				return "analyzed";
			case NOT_ANALYZED:
			case NOT_ANALYZED_NO_NORMS:
				return "not_analyzed";
			case NO:
				return "no";
			default:
				throw new IllegalArgumentException( "Unexpected index type: " + fieldMetadata.getIndex() );
		}
	}

	private String getFieldType(EntityIndexBinding descriptor, DocumentFieldMetadata fieldMetadata) {
		String type;

		if ( FieldHelper.isBoolean( descriptor, fieldMetadata.getName() ) ) {
			type = "boolean";
		}
		// TODO Calendar
		else if ( FieldHelper.isDate( descriptor, fieldMetadata.getName() ) ) {
			type = "date";
		}
		else if ( FieldHelper.isNumeric( fieldMetadata ) ) {

			NumericEncodingType numericEncodingType = FieldHelper.getNumericEncodingType( descriptor, fieldMetadata );

			switch( numericEncodingType ) {
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

	private JsonObject getOrCreateProperties(JsonObject mapping, String fieldName) {
		if ( !fieldName.contains( "." ) ) {
			return mapping.getAsJsonObject( "properties" );
		}

		JsonObject parentProperties = mapping.getAsJsonObject( "properties" );


		String[] parts = fieldName.split( "\\." );
		for ( int i = 0; i < parts.length - 1; i++ ) {
			String part = parts[i];
			JsonObject property = parentProperties.getAsJsonObject( part );
			if ( property == null ) {
				property = new JsonObject();
				property.addProperty( "type", "nested" );

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
	public void closeIndexWriter() {
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
	public void performOperations(List<LuceneWork> queue, IndexingMonitor monitor) {
		backend.applyWork( queue, monitor );
	}

	@Override
	public void performStreamOperation(LuceneWork singleOperation, IndexingMonitor monitor, boolean forceAsync) {
		backend.applyStreamWork( singleOperation, monitor );
	}

	@Override
	public void optimize() {
		// TODO Is there such thing for ES?
	}
}
