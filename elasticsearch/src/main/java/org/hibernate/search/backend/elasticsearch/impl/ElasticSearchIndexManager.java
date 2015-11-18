/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.impl;

import static org.hibernate.search.backend.elasticsearch.client.impl.RequestHelper.executeRequest;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.IndicesExists;
import io.searchbox.indices.mapping.PutMapping;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.similarities.Similarity;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.indexes.spi.ReaderProvider;
import org.hibernate.search.spi.WorkerBuildContext;

import com.google.gson.JsonObject;

/**
 * An {@link IndexManager} applying indexing work to an ElasticSearch server.
 *
 * @author Gunnar Morling
 */
public class ElasticSearchIndexManager implements IndexManager {

	private String indexName;
	String actualIndexName;
	private Similarity similarity;

	ExtendedSearchIntegrator searchIntegrator;
	private final Set<Class<?>> containedEntityTypes = new HashSet<>();

	// Lifecycle

	@Override
	public void initialize(String indexName, Properties properties, Similarity similarity, WorkerBuildContext context) {
		this.indexName = indexName;

		this.actualIndexName = indexName.toLowerCase( Locale.ENGLISH );
		if ( !actualIndexName.equals( indexName ) ) {
			// TODO LOG
		}

		this.similarity = similarity;
	}

	@Override
	public void destroy() {
	}

	@Override
	public void setSearchFactory(ExtendedSearchIntegrator boundSearchIntegrator) {
		this.searchIntegrator = boundSearchIntegrator;
		createIndexIfNotYetExisting();
		createIndexMappings();
	}

	@Override
	public void addContainedEntity(Class<?> entity) {
		containedEntityTypes.add( entity );
	}

	private void createIndexIfNotYetExisting() {
		if ( Boolean.TRUE.equals( executeRequest( new IndicesExists.Builder( actualIndexName ).build(), false ).getValue( "found" ) ) ) {
			return;
		}

		executeRequest( new CreateIndex.Builder( actualIndexName ).build() );
	}

	private void createIndexMappings() {
		for ( Class<?> entityType : containedEntityTypes ) {
			EntityIndexBinding descriptor = searchIntegrator.getIndexBinding( entityType );

			JsonObject payload = new JsonObject();
			payload.addProperty( "dynamic", "strict" );
			JsonObject properties = new JsonObject();
			payload.add( "properties", properties );

			for ( DocumentFieldMetadata fieldMetadata : descriptor.getDocumentBuilder().getTypeMetadata().getAllDocumentFieldMetadata() ) {
				if ( fieldMetadata.isId() ) {
					continue;
				}

				JsonObject field = new JsonObject();
				field.addProperty( "type", fieldMetadata.isNumeric() ? "long" : "string" );
				field.addProperty( "store", fieldMetadata.getStore() == Store.NO ? false : true );
				field.addProperty( "index", getIndex( descriptor, fieldMetadata ) );

				getOrCreateProperties( payload, fieldMetadata.getName() ).add( fieldMetadata.getName().substring( fieldMetadata.getName().lastIndexOf( "." ) + 1 ), field );
			}

			PutMapping putMapping = new PutMapping.Builder(
					actualIndexName,
					entityType.getName(),
					payload
			)
			.build();

			try {
				executeRequest( putMapping );
			}
			catch (Exception e) {
				throw new SearchException( "Could not create mapping for entity type " + entityType.getName(), e );
			}
		}
	}

	@SuppressWarnings("deprecation")
	private String getIndex(EntityIndexBinding binding, DocumentFieldMetadata fieldMetadata) {
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
		return null;
	}

	public String getActualIndexName() {
		return actualIndexName;
	}

	// Runtime ops

	@Override
	public void performOperations(List<LuceneWork> queue, IndexingMonitor monitor) {
		for ( LuceneWork luceneWork : queue ) {
			luceneWork.acceptIndexWorkVisitor( new ElasticSearchIndexWorkVisitor( actualIndexName, searchIntegrator ), null );
		}
	}

	@Override
	public void performStreamOperation(LuceneWork singleOperation, IndexingMonitor monitor, boolean forceAsync) {

	}

	@Override
	public void optimize() {
	}
}
