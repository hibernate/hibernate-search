/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.impl;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexableField;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.FlushLuceneWork;
import org.hibernate.search.backend.IndexWorkVisitor;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.UpdateLuceneWork;
import org.hibernate.search.backend.elasticsearch.client.impl.JestClient;
import org.hibernate.search.backend.spi.DeleteByQueryLuceneWork;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
import org.hibernate.search.engine.metadata.impl.EmbeddedTypeMetadata;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.engine.service.spi.ServiceReference;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

import io.searchbox.core.Delete;
import io.searchbox.core.DeleteByQuery;
import io.searchbox.core.Index;
import io.searchbox.indices.Refresh;
import io.searchbox.params.Parameters;

/**
 * @author Gunnar Morling
 */
class ElasticsearchIndexWorkVisitor implements IndexWorkVisitor<Void, Void> {

	private static final Log LOG = LoggerFactory.make();

	private static final Pattern DOT = Pattern.compile( "\\." );
	private static final String DELETE_ALL_QUERY = "{ \"query\" : { \"constant_score\" : { \"filter\" : { \"match_all\" : { } } } } }";
	private static final String DELETE_ALL_FOR_TENANT_QUERY = "{ \"query\" : { \"constant_score\" : { \"filter\" : { \"term\" : { \"" + DocumentBuilderIndexedEntity.TENANT_ID_FIELDNAME + "\" : \"%s\" } } } } }";

	private final String indexName;
	private final ExtendedSearchIntegrator searchIntegrator;

	public ElasticsearchIndexWorkVisitor(String indexName, ExtendedSearchIntegrator searchIntegrator) {
		this.indexName = indexName;
		this.searchIntegrator = searchIntegrator;
	}

	@Override
	public Void visitAddWork(AddLuceneWork work, Void p) {
		indexDocument( DocumentIdHelper.getDocumentId( work ), work.getDocument(), work.getEntityClass() );
		return null;
	}

	@Override
	public Void visitDeleteWork(DeleteLuceneWork work, Void p) {
		Delete delete = new Delete.Builder( DocumentIdHelper.getDocumentId( work ) )
			.index( indexName )
			.type( work.getEntityClass().getName() )
			// TODO Make configurable?
			.setParameter( Parameters.REFRESH, true )
			.build();

		try ( ServiceReference<JestClient> client = searchIntegrator.getServiceManager().requestReference( JestClient.class ) ) {
			client.get().executeRequest( delete, false );
		}

		return null;
	}

	@Override
	public Void visitOptimizeWork(OptimizeLuceneWork work, Void p) {
		// TODO implement
		LOG.warn( "Optimize work is not yet supported for Elasticsearch backend, ignoring it" );
		return null;
	}

	@Override
	public Void visitPurgeAllWork(PurgeAllLuceneWork work, Void p) {
		// TODO This requires the delete-by-query plug-in on ES 2.0 and beyond; Alternatively
		// the type mappings could be deleted, think about implications for concurrent access
		String query = work.getTenantId() != null ?
				String.format( Locale.ENGLISH, DELETE_ALL_FOR_TENANT_QUERY, work.getTenantId() ) :
				DELETE_ALL_QUERY;

		DeleteByQuery.Builder builder = new DeleteByQuery.Builder( query )
			.addIndex( indexName );

		Set<Class<?>> typesToDelete = searchIntegrator.getIndexedTypesPolymorphic( new Class<?>[] { work.getEntityClass() } );
		for ( Class<?> typeToDelete : typesToDelete ) {
			builder.addType( typeToDelete.getName() );
		}

		DeleteByQuery delete = builder.build();
		Refresh refresh = new Refresh.Builder().addIndex( indexName ).build();

		try ( ServiceReference<JestClient> client = searchIntegrator.getServiceManager().requestReference( JestClient.class ) ) {
			client.get().executeRequest( delete );

			// TODO Refresh not needed on ES 1.x; Make it configurable?
			client.get().executeRequest( refresh );
		}

		return null;
	}

	@Override
	public Void visitUpdateWork(UpdateLuceneWork work, Void p) {
		indexDocument( DocumentIdHelper.getDocumentId( work ), work.getDocument(), work.getEntityClass() );
		return null;
	}

	@Override
	public Void visitFlushWork(FlushLuceneWork work, Void p) {
		// TODO implement
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public Void visitDeleteByQueryWork(
			DeleteByQueryLuceneWork work, Void p) {
		// TODO implement
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	private void indexDocument(String id, Document document, Class<?> entityType) {
		JsonObject source = convertToJson( document, entityType );
		String type = entityType.getName();

		Index index = new Index.Builder( source )
			.index( indexName )
			.type( type )
			.id( id )
			// TODO Make configurable?
			.setParameter( Parameters.REFRESH, true )
			.build();

		try ( ServiceReference<JestClient> client = searchIntegrator.getServiceManager().requestReference( JestClient.class ) ) {
			client.get().executeRequest( index );
		}
	}

	private JsonObject convertToJson(Document document, Class<?> entityType) {
		EntityIndexBinding indexBinding = searchIntegrator.getIndexBinding( entityType );
		JsonObject source = new JsonObject();

		for ( IndexableField field : document.getFields() ) {
			if ( !field.name().equals( ProjectionConstants.OBJECT_CLASS ) &&
					!field.name().equals( indexBinding.getDocumentBuilder().getIdKeywordName() ) &&
					! isDocValueField( field) ) {

				JsonObject parent = getOrCreateDocumentTree( source, field );
				String jsonPropertyName = field.name().substring( field.name().lastIndexOf( "." ) + 1 );

				DocumentFieldMetadata documentFieldMetadata = indexBinding.getDocumentBuilder().getTypeMetadata().getDocumentFieldMetadataFor( field.name() );

				if ( documentFieldMetadata == null ) {
					String[] fieldNameParts = FieldHelper.getFieldNameParts( field.name() );

					EmbeddedTypeMetadata embeddedType = getEmbeddedTypeMetadata( indexBinding.getDocumentBuilder().getTypeMetadata(), fieldNameParts );

					// Make sure this field does not represent an embeddable (not a field thereof)
					if ( embeddedType == null ) {
						// should only be the case for class-bridge fields; in that case we'd miss proper handling of boolean/Date for now
						String stringValue = field.stringValue();
						if ( stringValue != null ) {
							parent.addProperty( jsonPropertyName, stringValue );
						}
						else {
							parent.addProperty( jsonPropertyName, field.numericValue() );
						}
					}
				}
				else if ( FieldHelper.isBoolean( indexBinding, field.name() ) ) {
					FieldBridge fieldBridge = documentFieldMetadata.getFieldBridge();
					Boolean value = (Boolean) ( (TwoWayFieldBridge) fieldBridge ).get( field.name(), document );
					parent.addProperty( jsonPropertyName, value );
				}
				// TODO falling back for now to checking actual Field type to cover numeric fields created by custom
				// bridges
				else if ( FieldHelper.isNumeric( documentFieldMetadata ) || isNumeric( field ) ) {
					// Explicitly propagate null in case value is not given and let ES handle the default token set in the meta-data
					Number value = field.numericValue();

					if ( value != null && value.toString().equals( documentFieldMetadata.indexNullAs() ) ) {
						value = null;
					}

					parent.addProperty( jsonPropertyName, value );
				}
				else {
					// Explicitly propagate null in case value is not given and let ES handle the default token set in the meta-data
					String value = field.stringValue();
					if ( value != null && value.equals( documentFieldMetadata.indexNullAs() ) ) {
						value = null;
					}

					parent.addProperty( jsonPropertyName, value );
				}
			}
		}

		return source;
	}

	private boolean isNumeric(IndexableField field) {
		return field instanceof IntField || field instanceof LongField || field instanceof FloatField || field instanceof DoubleField;
	}

	private EmbeddedTypeMetadata getEmbeddedTypeMetadata(TypeMetadata type, String[] fieldNameParts) {
		TypeMetadata parent = type;

		for ( String namePart : fieldNameParts ) {
			EmbeddedTypeMetadata embeddedType = getDirectEmbeddedTypeMetadata( parent, namePart );
			if ( embeddedType == null ) {
				return null;
			}

			parent = embeddedType;
		}

		return (EmbeddedTypeMetadata) parent;
	}

	private EmbeddedTypeMetadata getDirectEmbeddedTypeMetadata(TypeMetadata type, String fieldName) {
		for ( EmbeddedTypeMetadata embeddedType : type.getEmbeddedTypeMetadata() ) {
			if ( embeddedType.getEmbeddedFieldName().equals( fieldName ) ) {
				return embeddedType;
			}
		}

		return null;
	}

	private JsonObject getOrCreateDocumentTree(JsonObject source, IndexableField field) {
		// top-level property
		if ( !field.name().contains( "." ) ) {
			return source;
		}

		// embedded property Create JSON hierarchy as needed
		String[] parts = DOT.split( field.name() );
		JsonObject parent = source;

		for ( int i = 0; i < parts.length - 1; i++ ) {
			JsonObject newParent = parent.getAsJsonObject( parts[i] );
			if ( newParent == null ) {
				newParent = new JsonObject();
				parent.add( parts[i], newParent );
			}
			parent = newParent;
		}

		return parent;
	}

	private boolean isDocValueField(IndexableField field) {
		return field.fieldType().docValuesType() != DocValuesType.NONE;
	}
}
