/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import java.util.List;

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
import org.hibernate.search.bridge.spi.NullMarker;
import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.elasticsearch.gson.impl.JsonElementType;
import org.hibernate.search.elasticsearch.gson.impl.UnexpectedJsonElementTypeException;
import org.hibernate.search.elasticsearch.gson.impl.UnknownTypeJsonAccessor;
import org.hibernate.search.elasticsearch.impl.NestingMarker.NestingPathComponent;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.util.impl.FieldHelper;
import org.hibernate.search.elasticsearch.util.impl.FieldHelper.ExtendedFieldType;
import org.hibernate.search.elasticsearch.util.impl.ParentPathMismatchException;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.elasticsearch.work.impl.builder.DeleteByQueryWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.builder.IndexWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.factory.ElasticsearchWorkFactory;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
import org.hibernate.search.engine.metadata.impl.EmbeddedTypeMetadata;
import org.hibernate.search.engine.metadata.impl.EmbeddedTypeMetadata.Container;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.spatial.impl.SpatialHelper;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Converts {@link LuceneWork}s into corresponding {@link ElasticsearchWork}s. Instances are specific
 * to one index.
 *
 * @author Gunnar Morling
 */
class ElasticsearchIndexWorkVisitor implements IndexWorkVisitor<IndexingMonitor, ElasticsearchWork<?>> {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private final URLEncodedString indexName;
	private final boolean refreshAfterWrite;
	private final ExtendedSearchIntegrator searchIntegrator;
	private final ElasticsearchWorkFactory workFactory;

	public ElasticsearchIndexWorkVisitor(URLEncodedString indexName, boolean refreshAfterWrite,
			ExtendedSearchIntegrator searchIntegrator, ElasticsearchWorkFactory workFactory) {
		this.indexName = indexName;
		this.refreshAfterWrite = refreshAfterWrite;
		this.searchIntegrator = searchIntegrator;
		this.workFactory = workFactory;
	}

	@Override
	public ElasticsearchWork<?> visitAddWork(AddLuceneWork work, IndexingMonitor monitor) {
		return indexDocument( getDocumentId( work ), work.getDocument(), work.getEntityType() )
				.monitor( monitor )
				.luceneWork( work )
				.markIndexDirty( refreshAfterWrite )
				.build();
	}

	@Override
	public ElasticsearchWork<?> visitDeleteWork(DeleteLuceneWork work, IndexingMonitor monitor) {
		return workFactory.delete( indexName, entityName( work ), getDocumentId( work ) )
				.luceneWork( work )
				.markIndexDirty( refreshAfterWrite )
				.build();
	}

	@Override
	public ElasticsearchWork<?> visitOptimizeWork(OptimizeLuceneWork work, IndexingMonitor monitor) {
		return workFactory.optimize().index( indexName )
				.luceneWork( work )
				.build();
	}

	@Override
	public ElasticsearchWork<?> visitPurgeAllWork(PurgeAllLuceneWork work, IndexingMonitor monitor) {
		JsonObject payload = createDeleteByQueryPayload(
				JsonBuilder.object().add( "match_all", new JsonObject() ).build(),
				work.getTenantId()
				);

		DeleteByQueryWorkBuilder builder = workFactory.deleteByQuery( indexName, payload )
				.luceneWork( work )
				.markIndexDirty( refreshAfterWrite );

		/*
		 * Deleting only the given type.
		 * Inheritance trees are handled at a higher level by creating multiple purge works.
		 */
		builder.type( URLEncodedString.fromString( work.getEntityType().getName() ) );
		return builder.build();
	}

	@Override
	public ElasticsearchWork<?> visitUpdateWork(UpdateLuceneWork work, IndexingMonitor monitor) {
		return indexDocument( getDocumentId( work ), work.getDocument(), work.getEntityType() )
				.monitor( monitor )
				.luceneWork( work )
				.markIndexDirty( refreshAfterWrite )
				.build();
	}

	@Override
	public ElasticsearchWork<?> visitFlushWork(FlushLuceneWork work, IndexingMonitor monitor) {
		return workFactory.flush()
				.index( indexName )
				.luceneWork( work )
				.build();
	}

	@Override
	public ElasticsearchWork<?> visitDeleteByQueryWork(DeleteByQueryLuceneWork work, IndexingMonitor monitor) {
		JsonObject convertedQuery = ToElasticsearch.fromDeletionQuery(
				searchIntegrator.getIndexBinding( work.getEntityType() ).getDocumentBuilder(),
				work.getDeletionQuery()
		);
		URLEncodedString typeName = URLEncodedString.fromString( work.getEntityType().getName() );

		JsonObject payload = createDeleteByQueryPayload( convertedQuery, work.getTenantId() );

		return workFactory.deleteByQuery( indexName, payload )
				.luceneWork( work )
				.type( typeName )
				.markIndexDirty( refreshAfterWrite )
				.build();
	}

	private JsonObject createDeleteByQueryPayload(JsonObject query, String tenantId) {
		// Add filter on tenant id if needed
		if ( tenantId != null ) {
			return JsonBuilder.object()
					.add( "query", JsonBuilder.object()
							.add( "bool", JsonBuilder.object()
									.add( "filter", JsonBuilder.object()
											.add( "term", JsonBuilder.object()
													.addProperty( DocumentBuilderIndexedEntity.TENANT_ID_FIELDNAME, tenantId )
											)
									)
									.add( "must", query )
							)
					)
					.build();
		}
		else {
			return JsonBuilder.object()
					.add( "query", query )
					.build();
		}
	}


	private IndexWorkBuilder indexDocument(URLEncodedString id, Document document, IndexedTypeIdentifier entityType) {
		JsonObject source = convertDocumentToJson( document, entityType );
		URLEncodedString typeName = URLEncodedString.fromString( entityType.getName() );
		return workFactory.index( indexName, typeName, id, source );
	}

	private JsonObject convertDocumentToJson(Document document, IndexedTypeIdentifier entityType) {
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
						UnknownTypeJsonAccessor accessor = accessorBuilder.buildForPath( fieldPath );
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
					UnknownTypeJsonAccessor accessor = accessorBuilder.buildForPath( fieldPath );

					// If the value was initially null, explicitly propagate null and let ES handle the default token.
					if ( field instanceof NullMarker ) {
						accessor.add( root, null );
						return;
					}

					ExtendedFieldType type = FieldHelper.getType( documentFieldMetadata );
					if ( ExtendedFieldType.BOOLEAN.equals( type ) ) {
						FieldBridge fieldBridge = documentFieldMetadata.getFieldBridge();
						Boolean value = (Boolean) ( (TwoWayFieldBridge) fieldBridge ).get( field.name(), document );

						accessor.add( root, value != null ? new JsonPrimitive( value ) : null );
					}
					else {
						Number numericValue = field.numericValue();

						if ( numericValue != null ) {
							accessor.add( root, numericValue != null ? new JsonPrimitive( numericValue ) : null );
						}
						else {
							String stringValue = field.stringValue();
							accessor.add( root, stringValue != null ? new JsonPrimitive( stringValue ) : null );
						}
					}
				}
			}
		}
		catch (ParentPathMismatchException e) {
			throw LOG.indexedEmbeddedPrefixBypass( indexBinding.getDocumentBuilder().getTypeIdentifier(),
					e.getMismatchingPath(), e.getExpectedParentPath() );
		}
		catch (UnexpectedJsonElementTypeException e) {
			List<JsonElementType<?>> expectedTypes = e.getExpectedTypes();
			JsonAccessor<?> accessor = e.getAccessor();
			JsonElement actualValue = e.getActualElement();

			if ( expectedTypes.contains( JsonElementType.OBJECT ) || JsonElementType.OBJECT.isInstance( actualValue ) ) {
				throw LOG.fieldIsBothCompositeAndConcrete( indexBinding.getDocumentBuilder().getTypeIdentifier(), accessor.getStaticAbsolutePath() );
			}
			else {
				throw new AssertionFailure( "Unexpected field naming conflict when indexing;"
						+ " this kind of issue should have been detected when building the metadata.", e );
			}
		}
	}

	private URLEncodedString getDocumentId(LuceneWork work) {
		return URLEncodedString.fromString( work.getTenantId() == null ? work.getIdInString() : work.getTenantId() + "_" + work.getIdInString() );
	}

	private boolean isDocValueField(IndexableField field) {
		return field.fieldType().docValuesType() != DocValuesType.NONE;
	}

	private static URLEncodedString entityName(LuceneWork work) {
		return URLEncodedString.fromString( work.getEntityType().getName() );
	}

}
