/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexableField;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.FlushLuceneWork;
import org.hibernate.search.backend.IndexWorkVisitor;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.UpdateLuceneWork;
import org.hibernate.search.backend.spi.DeleteByQueryLuceneWork;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.elasticsearch.client.impl.BackendRequest;
import org.hibernate.search.elasticsearch.impl.NestingMarker.NestingPathComponent;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.util.impl.FieldHelper;
import org.hibernate.search.elasticsearch.util.impl.FieldHelper.ExtendedFieldType;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
import org.hibernate.search.engine.metadata.impl.EmbeddedTypeMetadata;
import org.hibernate.search.engine.metadata.impl.EmbeddedTypeMetadata.Container;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.spatial.impl.SpatialHelper;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.searchbox.action.Action;
import io.searchbox.core.Delete;
import io.searchbox.core.DeleteByQuery;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.Index;
import io.searchbox.indices.Optimize;

/**
 * Converts {@link LuceneWork}s into corresponding {@link BackendRequest}s. Instances are specific
 * to one index.
 *
 * @author Gunnar Morling
 */
class ElasticsearchIndexWorkVisitor implements IndexWorkVisitor<IndexingMonitor, BackendRequest<?>> {

	private static final Log LOG = LoggerFactory.make( Log.class );

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
	public BackendRequest<?> visitAddWork(AddLuceneWork work, IndexingMonitor monitor) {
		Action<DocumentResult> index = indexDocument( getDocumentId( work ), work.getDocument(), work.getEntityClass() );
		return new BackendRequest<>( index, work, indexName,
				monitor, DocumentAddedBackendRequestSuccessReporter.INSTANCE, refreshAfterWrite );
	}

	@Override
	public BackendRequest<?> visitDeleteWork(DeleteLuceneWork work, IndexingMonitor monitor) {
		Delete delete = new Delete.Builder( getDocumentId( work ) )
			.index( indexName )
			.type( work.getEntityClass().getName() )
			.build();

		return new BackendRequest<>( delete, work, indexName,
				monitor, NoopBackendRequestSuccessHandler.INSTANCE, refreshAfterWrite, 404 );
	}

	@Override
	public BackendRequest<?> visitOptimizeWork(OptimizeLuceneWork work, IndexingMonitor monitor) {
		/*
		 * As of ES 2.1, the Optimize API has been renamed to ForceMerge,
		 * but Jest still does not provide commands for the ForceMerge API as of
		 * version 2.0.3
		 * See https://github.com/searchbox-io/Jest/issues/292
		 */
		Optimize optimize = new Optimize.Builder()
				.addIndex( indexName )
				.build();

		return new BackendRequest<>( optimize, work, indexName,
				monitor, NoopBackendRequestSuccessHandler.INSTANCE, refreshAfterWrite );
	}

	@Override
	public BackendRequest<?> visitPurgeAllWork(PurgeAllLuceneWork work, IndexingMonitor monitor) {
		String query = work.getTenantId() != null ?
				String.format( Locale.ENGLISH, DELETE_ALL_FOR_TENANT_QUERY, work.getTenantId() ) :
				DELETE_ALL_QUERY;

		DeleteByQuery.Builder builder = new DeleteByQuery.Builder( query )
			.addIndex( indexName );

		Set<Class<?>> typesToDelete = searchIntegrator.getIndexedTypesPolymorphic( new Class<?>[] { work.getEntityClass() } );
		for ( Class<?> typeToDelete : typesToDelete ) {
			builder.addType( typeToDelete.getName() );
		}

		return new BackendRequest<>( builder.build(), work, indexName,
				monitor, NoopBackendRequestSuccessHandler.INSTANCE, refreshAfterWrite );
	}

	@Override
	public BackendRequest<?> visitUpdateWork(UpdateLuceneWork work, IndexingMonitor monitor) {
		Action<DocumentResult> index = indexDocument( getDocumentId( work ), work.getDocument(), work.getEntityClass() );
		return new BackendRequest<>( index, work, indexName,
				monitor, DocumentAddedBackendRequestSuccessReporter.INSTANCE, refreshAfterWrite );
	}

	@Override
	public BackendRequest<?> visitFlushWork(FlushLuceneWork work, IndexingMonitor monitor) {
		// Nothing to do
		return null;
	}

	@Override
	public BackendRequest<?> visitDeleteByQueryWork(DeleteByQueryLuceneWork work, IndexingMonitor monitor) {
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
					.add( "bool", JsonBuilder.object()
						.add( "filter", JsonBuilder.object()
							.add( "term", JsonBuilder.object()
								.addProperty( DocumentBuilderIndexedEntity.TENANT_ID_FIELDNAME, work.getTenantId() )
							)
						)
						.add( "must", convertedQuery )
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

		return new BackendRequest<>( deleteByQuery, work, indexName,
				monitor, NoopBackendRequestSuccessHandler.INSTANCE, refreshAfterWrite );
	}

	private Action<DocumentResult> indexDocument(String id, Document document, Class<?> entityType) {
		JsonObject source = convertDocumentToJson( document, entityType );
		String type = entityType.getName();

		Index index = new Index.Builder( source )
			.index( indexName )
			.type( type )
			.id( id )
			.build();

		return index;
	}

	private JsonObject convertDocumentToJson(Document document, Class<?> entityType) {
		EntityIndexBinding indexBinding = searchIntegrator.getIndexBinding( entityType );
		JsonObject root = new JsonObject();

		NestingMarker nestingMarker = null;
		JsonAccessorBuilder accessorBuilder = new JsonAccessorBuilder();
		for ( IndexableField field : document.getFields() ) {
			if ( field instanceof NestingMarker ) {
				nestingMarker = (NestingMarker) field;
				accessorBuilder.reset();
				accessorBuilder.append( ((NestingMarker) field).getPath() );
				continue; // Inspect the next field taking into account this metadata
			}

			convertFieldToJson( root, accessorBuilder, indexBinding, nestingMarker, document, field );
		}

		return root;
	}

	private void convertFieldToJson(
			JsonObject root, JsonAccessorBuilder accessorBuilder,
			EntityIndexBinding indexBinding, NestingMarker nestingMarker, Document document, IndexableField field
			) {
		try {
			String fieldPath = field.name();
			List<NestingPathComponent> nestingPath = nestingMarker == null ? null : nestingMarker.getPath();
			NestingPathComponent lastPathComponent = nestingPath == null ? null : nestingPath.get( nestingPath.size() - 1 );
			EmbeddedTypeMetadata embeddedType = lastPathComponent == null ? null : lastPathComponent.getEmbeddedTypeMetadata();

			if ( embeddedType != null && fieldPath.equals( embeddedType.getEmbeddedNullFieldName() ) ) {
				// Case of a null indexed embedded

				// Exclude the last path component: it represents the null embedded
				nestingPath = nestingPath.subList( 0, nestingPath.size() - 1 );
				accessorBuilder.reset();
				accessorBuilder.append( nestingPath );

				Container containerType = embeddedType.getEmbeddedContainer();

				switch ( containerType ) {
					case ARRAY:
					case COLLECTION:
					case MAP:
						/*
						 * When a indexNullAs is set for array/collection/map embeddeds, and we get a null replacement
						 * token from the engine, just keep the token, and don't replace it back with null (which is
						 * what we do for other fields, see below).
						 * This behavior is necessary because Elasticsearch treats null arrays exactly as arrays
						 * containing only null, so propagating null for an array/collection/map as a whole would
						 * lead to conflicts when querying.
						 */
						String value = field.stringValue();

						accessorBuilder.buildForPath( fieldPath ).set( root, value != null ? new JsonPrimitive( value ) : null );
						break;
					case OBJECT:
						// TODO HSEARCH-2389 Support indexNullAs for @IndexedEmbedded applied on objects with Elasticsearch
						break;
					default:
						throw new AssertionFailure( "Unexpected container type: " + containerType );
				}
			}
			else if ( FacetsConfig.DEFAULT_INDEX_FIELD_NAME.equals( field.name() ) ) {
				/*
				 * Lucene-specific fields for facets.
				 * Just ignore such fields: Elasticsearch handles that internally.
				 */
				return;
			}
			else if ( isDocValueField( field ) ) {
				/*
				 * Doc value fields for facets or sorts.
				 * Just ignore such fields: Elasticsearch handles that internally.
				 */
				return;
			}
			else if ( fieldPath.equals( ProjectionConstants.OBJECT_CLASS ) ) {
				// Object class: no need to index that in Elasticsearch, because documents are typed.
				return;
			}
			else {
				DocumentFieldMetadata documentFieldMetadata = indexBinding.getDocumentBuilder().getTypeMetadata()
						.getDocumentFieldMetadataFor( field.name() );

				if ( documentFieldMetadata == null ) {
					if ( SpatialHelper.isSpatialField( fieldPath ) ) {
						if ( SpatialHelper.isSpatialFieldLatitude( fieldPath ) ) {
							Number value = field.numericValue();
							String spatialPropertyPath = SpatialHelper.stripSpatialFieldSuffix( fieldPath );
							accessorBuilder.buildForPath( spatialPropertyPath + ".lat" )
									.add( root, value != null ? new JsonPrimitive( value ) : null );
						}
						else if ( SpatialHelper.isSpatialFieldLongitude( fieldPath ) ) {
							Number value = field.numericValue();
							String spatialPropertyPath = SpatialHelper.stripSpatialFieldSuffix( fieldPath );
							accessorBuilder.buildForPath( spatialPropertyPath + ".lon" )
									.add( root, value != null ? new JsonPrimitive( value ) : null );
						}
						else {
							// here, we have the hash fields used for spatial hash indexing
							String value = field.stringValue();
							accessorBuilder.buildForPath( fieldPath )
									.add( root, value != null ? new JsonPrimitive( value ) : null );
						}
					}
					else {
						// should only be the case for class-bridge fields; in that case we'd miss proper handling of boolean/Date for now
						JsonAccessor accessor = accessorBuilder.buildForPath( fieldPath );
						String stringValue = field.stringValue();
						Number numericValue = field.numericValue();
						if ( stringValue != null ) {
							accessor.add( root, new JsonPrimitive( stringValue ) );
						}
						else if ( numericValue != null ) {
							accessor.add( root, new JsonPrimitive( numericValue ) );
						}
						else {
							accessor.add( root, null );
						}
					}
				}
				else {
					JsonAccessor accessor = accessorBuilder.buildForPath( fieldPath );
					ExtendedFieldType type = FieldHelper.getType( documentFieldMetadata );
					if ( ExtendedFieldType.BOOLEAN.equals( type ) ) {
						FieldBridge fieldBridge = documentFieldMetadata.getFieldBridge();
						Boolean value = (Boolean) ( (TwoWayFieldBridge) fieldBridge ).get( field.name(), document );

						accessor.add( root, value != null ? new JsonPrimitive( value ) : null );
					}
					else {
						Number numericValue = field.numericValue();

						if ( numericValue != null ) {
							// If the value was initially null, explicitly propagate null and let ES handle the default token.
							if ( documentFieldMetadata.getNullMarkerCodec().representsNullValue( field ) ) {
								numericValue = null;
							}
							accessor.add( root, numericValue != null ? new JsonPrimitive( numericValue ) : null );
						}
						else {
							String stringValue = field.stringValue();
							// If the value was initially null, explicitly propagate null and let ES handle the default token.
							if ( documentFieldMetadata.getNullMarkerCodec().representsNullValue( field ) ) {
								stringValue = null;
							}

							accessor.add( root, stringValue != null ? new JsonPrimitive( stringValue ) : null );
						}
					}
				}
			}
		}
		catch (UnexpectedJsonElementTypeException e) {
			List<JsonElementType<?>> expectedTypes = e.getExpectedTypes();
			JsonAccessor accessor = e.getAccessor();
			JsonElement actualValue = e.getActualElement();

			if ( expectedTypes.contains( JsonElementType.OBJECT ) || JsonElementType.OBJECT.isInstance( actualValue ) ) {
				throw LOG.fieldIsBothCompositeAndConcrete( indexBinding.getDocumentBuilder().getBeanClass(), accessor.getStaticAbsolutePath() );
			}
			else {
				throw new AssertionFailure( "Unexpected field naming conflict when indexing;"
						+ " this kind of issue should have been detected when building the metadata.", e );
			}
		}
	}

	private String getDocumentId(LuceneWork work) {
		return work.getTenantId() == null ? work.getIdInString() : work.getTenantId() + "_" + work.getIdInString();
	}

	private boolean isDocValueField(IndexableField field) {
		return field.fieldType().docValuesType() != DocValuesType.NONE;
	}
}
