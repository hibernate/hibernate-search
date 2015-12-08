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
import org.hibernate.search.backend.elasticsearch.cfg.ElasticSearchEnvironment;
import org.hibernate.search.backend.elasticsearch.cfg.IndexManagementStrategy;
import org.hibernate.search.backend.elasticsearch.client.impl.JestClientReference;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
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
 * An {@link IndexManager} applying indexing work to an ElasticSearch server.
 *
 * @author Gunnar Morling
 */
public class ElasticSearchIndexManager implements IndexManager {

	private static final Log LOG = LoggerFactory.make();

	private String indexName;
	private String actualIndexName;
	private IndexManagementStrategy indexManagementStrategy;
	private Similarity similarity;

	ExtendedSearchIntegrator searchIntegrator;
	private final Set<Class<?>> containedEntityTypes = new HashSet<>();

	private JestClientReference clientReference;
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
		this.actualIndexName = IndexNameNormalizer.getElasticSearchIndexName( this.indexName );
		this.similarity = similarity;
		this.backend = BackendFactory.createBackend( this, context, properties );
	}

	private String getIndexName(String indexName, Properties properties) {
		String name = properties.getProperty( Environment.INDEX_NAME_PROP_NAME );
		return name != null ? name : indexName;
	}

	private IndexManagementStrategy getIndexManagementStrategy(Properties properties) {
		String strategy = properties.getProperty( ElasticSearchEnvironment.INDEX_MANAGEMENT_STRATEGY );
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
		this.clientReference = new JestClientReference( searchIntegrator.getServiceManager() );

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
		clientReference.executeRequest( new CreateIndex.Builder( actualIndexName ).build() );
	}

	private void createIndexIfNotYetExisting() {
		if ( clientReference.executeRequest( new IndicesExists.Builder( actualIndexName ).build(), false ).getResponseCode() == 200 ) {
			return;
		}

		clientReference.executeRequest( new CreateIndex.Builder( actualIndexName ).build() );
	}

	private void deleteIndexIfExisting() {
		// Not actually needed, but do it to avoid cluttering the ES log
		if ( clientReference.executeRequest( new IndicesExists.Builder( actualIndexName ).build(), false ).getResponseCode() == 404 ) {
			return;
		}

		try {
			clientReference.executeRequest( new DeleteIndex.Builder( actualIndexName ).build() );
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
				if ( fieldMetadata.isId() ) {
					continue;
				}

				addFieldMapping( payload, descriptor, fieldMetadata );
			}

			// fields contributed by class bridges; only a single named field per class bridge is supported atm. as we
			// lack the meta-data on all fields potentially created by a class bridge
			for ( DocumentFieldMetadata fieldMetadata : descriptor.getDocumentBuilder().getTypeMetadata().getClassBridgeMetadata() ) {
				// TODO should we support dynamically added fields?
				if ( fieldMetadata.getName() == null || fieldMetadata.getName().isEmpty() ) {
					throw new SearchException(
							"Unnamed class-level fields are not supported with the ElasticSearch backend: " + descriptor.getDocumentBuilder().getTypeMetadata().getType()
					);
				}

				addFieldMapping( payload, descriptor, fieldMetadata );
			}

			PutMapping putMapping = new PutMapping.Builder(
					actualIndexName,
					entityType.getName(),
					payload
			)
			.build();

			try {
				clientReference.executeRequest( putMapping );
			}
			catch (Exception e) {
				throw new SearchException( "Could not create mapping for entity type " + entityType.getName(), e );
			}
		}
	}

	/**
	 * Adds a type mapping for the given field to the given request payload.
	 */
	private JsonObject addFieldMapping(JsonObject payload, EntityIndexBinding descriptor, DocumentFieldMetadata fieldMetadata) {
		String simpleFieldName = fieldMetadata.getName().substring( fieldMetadata.getName().lastIndexOf( "." ) + 1 );
		JsonObject field = new JsonObject();

		field.addProperty( "type", getFieldType( descriptor, fieldMetadata ) );
		field.addProperty( "store", fieldMetadata.getStore() == Store.NO ? false : true );
		field.addProperty( "index", getIndex( descriptor, fieldMetadata ) );
		field.addProperty( "boost", fieldMetadata.getBoost() );

		if ( fieldMetadata.indexNullAs() != null ) {
			// TODO Validate the type; Supported types are converted transparently by ES
			field.addProperty( "null_value", fieldMetadata.indexNullAs() );
		}

		getOrCreateProperties( payload, fieldMetadata.getName() ).add( simpleFieldName, field );
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
			NumericEncodingType numericEncodingType = FieldHelper.getNumericEncodingType( fieldMetadata );

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
					throw new SearchException( "Unexpected numeric field type: " + descriptor.getDocumentBuilder().getMetadata().getType() + " "
						+ fieldMetadata.getName() );
			}
		}
		else {
			type = "string";
		}

		return type;
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
