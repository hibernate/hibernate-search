/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleDocValuesField;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexableField;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.FlushLuceneWork;
import org.hibernate.search.backend.IndexWorkVisitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.UpdateLuceneWork;
import org.hibernate.search.backend.spi.DeleteByQueryLuceneWork;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.elasticsearch.client.impl.BackendRequest;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
import org.hibernate.search.engine.metadata.impl.EmbeddedTypeMetadata;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.spatial.impl.SpatialHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.searchbox.action.Action;
import io.searchbox.core.Delete;
import io.searchbox.core.DeleteByQuery;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.Index;

/**
 * Converts {@link LuceneWork}s into corresponding {@link BackendRequest}s. Instances are specific
 * to one index.
 *
 * @author Gunnar Morling
 */
class ElasticsearchIndexWorkVisitor implements IndexWorkVisitor<Void, BackendRequest<?>> {

	private static final Log LOG = LoggerFactory.make();

	private static final Pattern DOT = Pattern.compile( "\\." );
	private static final Pattern NAME_AND_INDEX = Pattern.compile( "(.+?)(\\[([0-9])+\\])?" );

	private static final String DELETE_ALL_QUERY = "{ \"query\" : { \"constant_score\" : { \"filter\" : { \"match_all\" : { } } } } }";
	private static final String DELETE_ALL_FOR_TENANT_QUERY = "{ \"query\" : { \"constant_score\" : { \"filter\" : { \"term\" : { \"" + DocumentBuilderIndexedEntity.TENANT_ID_FIELDNAME + "\" : \"%s\" } } } } }";

	private final String indexName;
	private final boolean refreshAfterWrite;
	private final ExtendedSearchIntegrator searchIntegrator;

	public ElasticsearchIndexWorkVisitor(String indexName, boolean refreshAfterWrite, ExtendedSearchIntegrator searchIntegrator) {
		this.indexName = indexName;
		this.refreshAfterWrite = refreshAfterWrite;
		this.searchIntegrator = searchIntegrator;
	}

	@Override
	public BackendRequest<?> visitAddWork(AddLuceneWork work, Void p) {
		Action<?> index = indexDocument( DocumentIdHelper.getDocumentId( work ), work.getDocument(), work.getEntityClass() );
		return new BackendRequest<>( index, work, indexName, refreshAfterWrite );
	}

	@Override
	public BackendRequest<?> visitDeleteWork(DeleteLuceneWork work, Void p) {
		Delete delete = new Delete.Builder( DocumentIdHelper.getDocumentId( work ) )
			.index( indexName )
			.type( work.getEntityClass().getName() )
			.build();

		return new BackendRequest<DocumentResult>( delete, work, indexName, refreshAfterWrite, 404 );
	}

	@Override
	public BackendRequest<?> visitOptimizeWork(OptimizeLuceneWork work, Void p) {
		// TODO HSEARCH-2092 implement
		LOG.warn( "Optimize work is not yet supported for Elasticsearch backend, ignoring it" );
		return null;
	}

	@Override
	public BackendRequest<?> visitPurgeAllWork(PurgeAllLuceneWork work, Void p) {
		String query = work.getTenantId() != null ?
				String.format( Locale.ENGLISH, DELETE_ALL_FOR_TENANT_QUERY, work.getTenantId() ) :
				DELETE_ALL_QUERY;

		DeleteByQuery.Builder builder = new DeleteByQuery.Builder( query )
			.addIndex( indexName );

		Set<Class<?>> typesToDelete = searchIntegrator.getIndexedTypesPolymorphic( new Class<?>[] { work.getEntityClass() } );
		for ( Class<?> typeToDelete : typesToDelete ) {
			builder.addType( typeToDelete.getName() );
		}

		return new BackendRequest<>( builder.build(), work, indexName, refreshAfterWrite );
	}

	@Override
	public BackendRequest<?> visitUpdateWork(UpdateLuceneWork work, Void p) {
		Action<?> index = indexDocument( DocumentIdHelper.getDocumentId( work ), work.getDocument(), work.getEntityClass() );
		return new BackendRequest<>( index, work, indexName, refreshAfterWrite );
	}

	@Override
	public BackendRequest<?> visitFlushWork(FlushLuceneWork work, Void p) {
		// Nothing to do
		return null;
	}

	@Override
	public BackendRequest<?> visitDeleteByQueryWork(DeleteByQueryLuceneWork work, Void p) {
		JsonObject convertedQuery = ToElasticsearch.fromDeletionQuery(
				searchIntegrator.getIndexBinding( work.getEntityClass() ).getDocumentBuilder(),
				work.getDeletionQuery()
		);
		String type = work.getEntityClass().getName();

		JsonObject query;

		// Add filter on tenant id if needed
		if ( work.getTenantId() != null ) {
			query = JsonBuilder.object()
				.add( "query", JsonBuilder.object()
					.add( "filtered", JsonBuilder.object()
						.add( "filter", JsonBuilder.object()
							.add( "term", JsonBuilder.object()
								.addProperty( DocumentBuilderIndexedEntity.TENANT_ID_FIELDNAME, work.getTenantId() )
							)
						)
						.add( "query", convertedQuery )
					)
				)
				.build();
		}
		else {
			query = JsonBuilder.object()
				.add( "query", convertedQuery )
				.build();
		}

		DeleteByQuery deleteByQuery = new DeleteByQuery.Builder( query.toString() )
			.addIndex( indexName )
			.addType( type )
			.build();

		return new BackendRequest<>( deleteByQuery, work, indexName, refreshAfterWrite );
	}

	private Action<?> indexDocument(String id, Document document, Class<?> entityType) {
		JsonObject source = convertToJson( document, entityType );
		String type = entityType.getName();

		Index index = new Index.Builder( source )
			.index( indexName )
			.type( type )
			.id( id )
			.build();

		return index;
	}

	private JsonObject convertToJson(Document document, Class<?> entityType) {
		EntityIndexBinding indexBinding = searchIntegrator.getIndexBinding( entityType );
		JsonObject source = new JsonObject();

		String parentPath = null;
		for ( IndexableField field : document.getFields() ) {
			// marker field for denoting the parent element of the subsequent fields
			if ( "$nesting".equals( field.name() ) ) {
				parentPath = field.stringValue();
				continue;
			}

			if ( !field.name().equals( ProjectionConstants.OBJECT_CLASS ) &&
					!field.name().equals( indexBinding.getDocumentBuilder().getIdKeywordName() ) &&
					!FacetsConfig.DEFAULT_INDEX_FIELD_NAME.equals( field.name() ) &&
					!isDocValueField( field ) ) {

				JsonObject parent = getOrCreateDocumentTree( source, parentPath );
				String jsonPropertyName = FieldHelper.getEmbeddedFieldPropertyName( field.name() );

				DocumentFieldMetadata documentFieldMetadata = indexBinding.getDocumentBuilder().getTypeMetadata().getDocumentFieldMetadataFor( field.name() );

				if ( documentFieldMetadata == null ) {
					if ( SpatialHelper.isSpatialField( jsonPropertyName ) ) {
						if ( isNumeric( field ) && ( SpatialHelper.isSpatialFieldLatitude( jsonPropertyName ) ||
								SpatialHelper.isSpatialFieldLongitude( jsonPropertyName ) ) ) {
							// work on the latitude/longitude fields
							Number value = field.numericValue();
							String spatialJsonPropertyName = SpatialHelper.getSpatialFieldRootName( jsonPropertyName );
							JsonObject spatialParent;

							if ( parent.get( spatialJsonPropertyName ) != null ) {
								spatialParent = parent.get( spatialJsonPropertyName ).getAsJsonObject();
							}
							else {
								spatialParent = new JsonObject();
								parent.add( spatialJsonPropertyName, spatialParent );
							}

							if ( SpatialHelper.isSpatialFieldLatitude( jsonPropertyName ) ) {
								addPropertyOfPotentiallyMultipleCardinality( spatialParent, "lat",
										value != null ? new JsonPrimitive( value ) : null );
							}
							else if ( SpatialHelper.isSpatialFieldLongitude( jsonPropertyName ) ) {
								addPropertyOfPotentiallyMultipleCardinality( spatialParent, "lon",
										value != null ? new JsonPrimitive( value ) : null );
							}
						}
						else {
							// here, we have the hash fields used for spatial hash indexing
							String value = field.stringValue();
							addPropertyOfPotentiallyMultipleCardinality( parent, jsonPropertyName,
									value != null ? new JsonPrimitive( value ) : null );
						}
					}
					else {
						String[] fieldNameParts = FieldHelper.getFieldNameParts( field.name() );

						EmbeddedTypeMetadata embeddedType = getEmbeddedTypeMetadata( indexBinding.getDocumentBuilder().getTypeMetadata(), fieldNameParts );

						// Make sure this field does not represent an embeddable (not a field thereof)
						if ( embeddedType == null ) {
							// should only be the case for class-bridge fields; in that case we'd miss proper handling of boolean/Date for now
							String stringValue = field.stringValue();
							if ( stringValue != null ) {
								addPropertyOfPotentiallyMultipleCardinality( parent, jsonPropertyName, new JsonPrimitive( stringValue ) );
							}
							else {
								addPropertyOfPotentiallyMultipleCardinality( parent, jsonPropertyName,
										field.numericValue() != null ? new JsonPrimitive( field.numericValue() ) : null );
							}
						}
					}
				}
				else if ( FieldHelper.isBoolean( indexBinding, field.name() ) ) {
					FieldBridge fieldBridge = documentFieldMetadata.getFieldBridge();
					Boolean value = (Boolean) ( (TwoWayFieldBridge) fieldBridge ).get( field.name(), document );
					addPropertyOfPotentiallyMultipleCardinality( parent, jsonPropertyName,
							value != null ? new JsonPrimitive( value ) : null );
				}
				// TODO HSEARCH-2261 falling back for now to checking actual Field type to cover numeric fields created by custom
				// bridges
				else if ( FieldHelper.isNumeric( documentFieldMetadata ) || isNumeric( field ) ) {
					// Explicitly propagate null in case value is not given and let ES handle the default token set
					// in the meta-data
					Number value = field.numericValue();

					if ( value != null && value.toString().equals( documentFieldMetadata.indexNullAs() ) ) {
						value = null;
					}

					addPropertyOfPotentiallyMultipleCardinality( parent, jsonPropertyName,
							value != null ? new JsonPrimitive( value ) : null );
				}
				else {
					// Explicitly propagate null in case value is not given and let ES handle the default token set in the meta-data
					String value = field.stringValue();
					if ( value != null && value.equals( documentFieldMetadata.indexNullAs() ) ) {
						value = null;
					}

					addPropertyOfPotentiallyMultipleCardinality( parent, jsonPropertyName,
							value != null ? new JsonPrimitive( value ) : null );
				}
			}
			else if ( FacetsConfig.DEFAULT_INDEX_FIELD_NAME.equals( field.name() )
					&& field instanceof SortedSetDocValuesField ) {
				// String facet fields
				String[] facetParts = FacetsConfig.stringToPath( field.binaryValue().utf8ToString() );
				if ( facetParts == null || facetParts.length != 2 ) {
					continue;
				}
				String fieldName = facetParts[0];
				String value = facetParts[1];

				// if it's not just a facet field, we ignore it as the field is going to be created by the standard
				// mechanism
				if ( indexBinding.getDocumentBuilder().getTypeMetadata().getDocumentFieldMetadataFor( fieldName ) != null ) {
					continue;
				}

				JsonObject parent = getOrCreateDocumentTree( source, fieldName );
				String jsonPropertyName = FieldHelper.getEmbeddedFieldPropertyName( fieldName );
				addPropertyOfPotentiallyMultipleCardinality( parent, jsonPropertyName,
						value != null ? new JsonPrimitive( value ) : null );
			}
			else if ( isDocValueField( field ) && field instanceof NumericDocValuesField ) {
				// Numeric facet fields: we also get fields created for sorting so we need to exclude them.
				if ( indexBinding.getDocumentBuilder().getTypeMetadata().getDocumentFieldMetadataFor( field.name() ) != null ) {
					continue;
				}

				Number value;
				if ( field instanceof DoubleDocValuesField ) {
					// double values are encoded so we need to decode them
					value = Double.longBitsToDouble( field.numericValue().longValue() );
				}
				else {
					value = field.numericValue();
				}
				JsonObject parent = getOrCreateDocumentTree( source, parentPath );
				String jsonPropertyName = FieldHelper.getEmbeddedFieldPropertyName( field.name() );
				addPropertyOfPotentiallyMultipleCardinality( parent, jsonPropertyName,
						value != null ? new JsonPrimitive( value ) : null );
			}
		}

		return source;
	}

	private void addPropertyOfPotentiallyMultipleCardinality(JsonObject parent, String propertyName, JsonPrimitive value) {
		JsonElement currentValue = parent.get( propertyName );
		if ( currentValue == null ) {
			JsonBuilder.object( parent ).add( propertyName, value );
		}
		else if ( !currentValue.isJsonArray() ) {
			parent.remove( propertyName );
			parent.add( propertyName, JsonBuilder.array().add( currentValue ).add( value ).build() );
		}
		else {
			currentValue.getAsJsonArray().add( value );
		}
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

	private JsonObject getOrCreateDocumentTree(JsonObject source, String path) {
		if ( path == null ) {
			return source;
		}

		// embedded property Create JSON hierarchy as needed
		String[] parts = DOT.split( path );
		JsonObject parent = source;

		for ( int i = 0; i < parts.length; i++ ) {
			Matcher nameAndIndex = NAME_AND_INDEX.matcher( parts[i] );
			nameAndIndex.matches();

			String name = nameAndIndex.group( 1 );
			String idx = nameAndIndex.group( 3 );
			Integer index = null;

			if ( idx != null ) {
				index = Integer.valueOf( idx );
				JsonArray array = parent.getAsJsonArray( name );
				if ( array == null ) {
					array = new JsonArray();
					parent.add( name, array );
				}

				JsonObject newParent = index < array.size() ? array.get( index ).getAsJsonObject() : null;
				if ( newParent == null ) {
					newParent = new JsonObject();

					if ( index >= array.size() ) {
						for ( int j = array.size(); j <= index; j++ ) {
							array.add( JsonNull.INSTANCE );
						}
					}
					array.set( index, newParent );
				}

				parent = newParent;
			}
			else {
				JsonObject newParent = parent.getAsJsonObject( name );
				if ( newParent == null ) {
					newParent = new JsonObject();
					parent.add( name, newParent );
				}
				parent = newParent;
			}
		}

		return parent;
	}

	private boolean isDocValueField(IndexableField field) {
		return field.fieldType().docValuesType() != DocValuesType.NONE;
	}
}
