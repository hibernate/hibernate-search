/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.query.impl;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.bridge.util.impl.ContextualExceptionBridgeHelper;
import org.hibernate.search.elasticsearch.ElasticsearchProjectionConstants;
import org.hibernate.search.elasticsearch.impl.JsonBuilder;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.util.impl.FieldHelper;
import org.hibernate.search.elasticsearch.util.impl.FieldHelper.ExtendedFieldType;
import org.hibernate.search.elasticsearch.work.impl.SearchResult;
import org.hibernate.search.engine.metadata.impl.BridgeDefinedField;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.query.engine.impl.EntityInfoImpl;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * @author Yoann Rodiere
 */
class QueryHitConverter {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private static final String SPATIAL_DISTANCE_FIELD = "_distance";

	public static Builder builder(ElasticsearchQueryFactory queryFactory,
			Map<String, EntityIndexBinding> targetedEntityBindingsByName) {
		return new Builder( queryFactory, targetedEntityBindingsByName );
	}

	private final Map<String, EntityIndexBinding> targetedEntityBindingsByName;
	private final Map<EntityIndexBinding, FieldProjection> idProjectionByEntityBinding;
	private final Map<EntityIndexBinding, FieldProjection[]> fieldProjectionsByEntityBinding;

	private final JsonElement sourceFilter;
	private final JsonElement scriptFields;
	private final boolean trackScore;

	private final String[] projectedFields;
	private final Integer sortByDistanceIndex;

	// Private constructor; use builder() instead
	private QueryHitConverter(Map<String, EntityIndexBinding> targetedEntityBindingsByName,
			Map<EntityIndexBinding, FieldProjection> idProjectionByEntityBinding,
			Map<EntityIndexBinding, FieldProjection[]> fieldProjectionsByEntityBinding,
			JsonElement sourceFilter, JsonElement scriptFields, boolean trackScore,
			String[] projectedFields, Integer sortByDistanceIndex) {
		this.targetedEntityBindingsByName = targetedEntityBindingsByName;
		this.idProjectionByEntityBinding = idProjectionByEntityBinding;
		this.fieldProjectionsByEntityBinding = fieldProjectionsByEntityBinding;
		this.sourceFilter = sourceFilter;
		this.trackScore = trackScore;
		this.projectedFields = projectedFields;
		this.scriptFields = scriptFields;
		this.sortByDistanceIndex = sortByDistanceIndex;
	}

	public void contributeToPayload(JsonBuilder.Object payloadBuilder) {
		if ( trackScore ) {
			payloadBuilder.addProperty( "track_scores", true );
		}
		payloadBuilder.add( "_source", sourceFilter );
		if ( scriptFields != null ) {
			payloadBuilder.add( "script_fields", scriptFields );
		}
	}

	public EntityInfo convert(SearchResult searchResult, JsonObject hit) {
		String type = hit.get( "_type" ).getAsString();
		EntityIndexBinding binding = targetedEntityBindingsByName.get( type );

		if ( binding == null ) {
			LOG.warnf( "Found unknown type in Elasticsearch index: " + type );
			return null;
		}

		DocumentBuilderIndexedEntity documentBuilder = binding.getDocumentBuilder();
		IndexedTypeIdentifier typeId = documentBuilder.getTypeIdentifier();

		ConversionContext conversionContext = new ContextualExceptionBridgeHelper();
		conversionContext.setConvertedTypeId( typeId );
		FieldProjection idProjection = idProjectionByEntityBinding.get( binding );
		Object id = idProjection.convertHit( hit, conversionContext );
		Object[] projections = null;

		if ( projectedFields != null ) {
			projections = new Object[projectedFields.length];

			for ( int i = 0; i < projections.length; i++ ) {
				String field = projectedFields[i];
				if ( field == null ) {
					continue;
				}
				switch ( field ) {
					case ElasticsearchProjectionConstants.SOURCE:
						projections[i] = hit.getAsJsonObject().get( "_source" ).toString();
						break;
					case ElasticsearchProjectionConstants.ID:
						projections[i] = id;
						break;
					case ElasticsearchProjectionConstants.OBJECT_CLASS:
						projections[i] = typeId.getPojoType();
						break;
					case ElasticsearchProjectionConstants.SCORE:
						projections[i] = hit.getAsJsonObject().get( "_score" ).getAsFloat();
						break;
					case ElasticsearchProjectionConstants.SPATIAL_DISTANCE:
						JsonElement distance = null;
						// if we sort by distance, we need to find the index of the DistanceSortField and use it
						// to extract the values from the sort array
						// if we don't sort by distance, we use the field generated by the script_field added earlier
						if ( sortByDistanceIndex != null ) {
							distance = hit.getAsJsonObject().get( "sort" ).getAsJsonArray().get( sortByDistanceIndex );
						}
						else {
							JsonElement fields = hit.getAsJsonObject().get( "fields" );
							if ( fields != null ) { // "fields" seems to be missing if there are only null results in script fields
								distance = hit.getAsJsonObject().get( "fields" ).getAsJsonObject()
										.get( SPATIAL_DISTANCE_FIELD );
							}
						}
						if ( distance != null && distance.isJsonArray() ) {
							JsonArray array = distance.getAsJsonArray();
							distance = array.size() >= 1 ? array.get( 0 ) : null;
						}
						if ( distance == null || distance.isJsonNull() ) {
							projections[i] = null;
						}
						else {
							Double distanceAsDouble = distance.getAsDouble();

							if ( distanceAsDouble == Double.MAX_VALUE || distanceAsDouble.isInfinite() ) {
								/*
								 * When we extract the distance from the sort, its default value is:
								 *  - Double.MAX_VALUE on older ES versions (5.0 and lower)
								 *  - Double.POSITIVE_INFINITY on newer ES versions (from somewhere around 5.2 onwards)
								 */
								projections[i] = null;
							}
							else {
								projections[i] = distance.getAsDouble();
							}
						}
						break;
					case ElasticsearchProjectionConstants.TOOK:
						projections[i] = searchResult.getTook();
						break;
					case ElasticsearchProjectionConstants.TIMED_OUT:
						projections[i] = searchResult.getTimedOut();
						break;
					case ElasticsearchProjectionConstants.THIS:
						// Use EntityInfo.ENTITY_PLACEHOLDER as placeholder.
						// It will be replaced when we populate
						// the EntityInfo with the real entity.
						projections[i] = EntityInfo.ENTITY_PLACEHOLDER;
						break;
					default:
						FieldProjection projection = fieldProjectionsByEntityBinding.get( binding )[i];
						projections[i] = projection.convertHit( hit, conversionContext );
				}
			}
		}

		return new EntityInfoImpl( typeId, documentBuilder.getIdPropertyName(), (Serializable) id, projections );
	}

	public static class Builder {
		private final ElasticsearchQueryFactory queryFactory;
		private final Map<String, EntityIndexBinding> targetedEntityBindingsByName;

		private final Map<EntityIndexBinding, FieldProjection> idProjectionByEntityBinding = new HashMap<>();
		private final Map<EntityIndexBinding, FieldProjection[]> fieldProjectionsByEntityBinding = new HashMap<>();
		private boolean trackScore = false;
		private boolean includeAllSource = false;

		private boolean hasSpatialDistanceProjection = false;
		private Integer sortByDistanceIndex = null;
		private Coordinates spatialSearchCenter;
		private String spatialFieldName;

		private final JsonBuilder.Array sourceFilterCollector = JsonBuilder.array();
		private String[] projectedFields;

		private Builder(ElasticsearchQueryFactory queryFactory, Map<String, EntityIndexBinding> targetedEntityBindingsByName) {
			this.queryFactory = queryFactory;
			this.targetedEntityBindingsByName = targetedEntityBindingsByName;

			/*
			 * IDs are always projected: always initialize their projections regardless of the
			 * "projectedFields" attribute.
			 */
			for ( EntityIndexBinding binding : targetedEntityBindingsByName.values() ) {
				DocumentBuilderIndexedEntity documentBuilder = binding.getDocumentBuilder();
				String idFieldName = documentBuilder.getIdFieldName();
				TypeMetadata typeMetadata = documentBuilder.getTypeMetadata();
				FieldProjection projection = createProjection( typeMetadata, idFieldName );
				idProjectionByEntityBinding.put( binding, projection );
			}
		}

		public Builder setSortByDistance(Integer sortIndex, Coordinates spatialSearchCenter, String spatialFieldName) {
			this.sortByDistanceIndex = sortIndex;
			this.spatialSearchCenter = spatialSearchCenter;
			this.spatialFieldName = spatialFieldName;
			return this;
		}

		public Builder setProjectedFields(String[] projectedFields) {
			if ( this.projectedFields != null ) {
				throw new AssertionFailure( "Projected fields set twice for a single query hit extractor" );
			}
			this.projectedFields = projectedFields;
			if ( projectedFields == null ) {
				return this;
			}
			for ( int i = 0 ; i < projectedFields.length ; ++i ) {
				String projectedField = projectedFields[i];
				if ( projectedField == null ) {
					continue;
				}
				switch ( projectedField ) {
					case ElasticsearchProjectionConstants.SOURCE:
						includeAllSource = true;
						break;
					case ElasticsearchProjectionConstants.SCORE:
						// Make sure to compute scores even if we don't sort by relevance
						trackScore = true;
						break;
					case ElasticsearchProjectionConstants.ID:
					case ElasticsearchProjectionConstants.THIS:
					case ElasticsearchProjectionConstants.OBJECT_CLASS:
					case ElasticsearchProjectionConstants.TOOK:
					case ElasticsearchProjectionConstants.TIMED_OUT:
						// Ignore: no impact on source filtering
						break;
					case ElasticsearchProjectionConstants.SPATIAL_DISTANCE:
						hasSpatialDistanceProjection = true;
						break;
					default:
						for ( EntityIndexBinding binding : targetedEntityBindingsByName.values() ) {
							TypeMetadata typeMetadata = binding.getDocumentBuilder().getTypeMetadata();
							FieldProjection projection = createProjection( typeMetadata, projectedField );
							FieldProjection[] projectionsForType = fieldProjectionsByEntityBinding.get( binding );
							if ( projectionsForType == null ) {
								projectionsForType = new FieldProjection[projectedFields.length];
								fieldProjectionsByEntityBinding.put( binding, projectionsForType );
							}
							projectionsForType[i] = projection;
						}
						break;
				}
			}
			return this;
		}

		public QueryHitConverter build() {
			JsonElement sourceFilter;
			if ( includeAllSource ) {
				sourceFilter = new JsonPrimitive( "*" );
			}
			else {
				JsonArray array = sourceFilterCollector.build();
				if ( array.size() > 0 ) {
					sourceFilter = array;
				}
				else {
					// Projecting only on score or other document-independent values
					sourceFilter = new JsonPrimitive( false );
				}
			}

			JsonElement scriptFields = null;
			if ( hasSpatialDistanceProjection && sortByDistanceIndex == null ) {
				// when the results are sorted by distance, Elasticsearch returns the distance in a "sort" field in
				// the results. If we don't sort by distance, we need to request for the distance using a script_field.
				scriptFields = JsonBuilder.object().add( SPATIAL_DISTANCE_FIELD, JsonBuilder.object()
							.add(
									"script",
									queryFactory.createSpatialDistanceScript( spatialSearchCenter, spatialFieldName )
							)
						)
						.build();
			}

			return new QueryHitConverter( targetedEntityBindingsByName,
					idProjectionByEntityBinding, fieldProjectionsByEntityBinding,
					sourceFilter, scriptFields, trackScore, projectedFields, sortByDistanceIndex );
		}

		private FieldProjection createProjection(TypeMetadata rootTypeMetadata, String projectedField) {
			DocumentFieldMetadata fieldMetadata = rootTypeMetadata.getDocumentFieldMetadataFor( projectedField );
			if ( fieldMetadata != null ) {
				return createProjection( rootTypeMetadata, fieldMetadata );
			}
			else {
				// We check if it is a field created by a field bridge
				BridgeDefinedField bridgeDefinedField = rootTypeMetadata.getBridgeDefinedFieldMetadataFor( projectedField );
				if ( bridgeDefinedField != null ) {
					String absoluteName = bridgeDefinedField.getAbsoluteName();
					ExtendedFieldType type = FieldHelper.getType( bridgeDefinedField );
					sourceFilterCollector.add( new JsonPrimitive( absoluteName ) );
					return new PrimitiveProjection( rootTypeMetadata, absoluteName, type );
				}
				else {
					/*
					 * No metadata: fall back to dynamically converting the resulting
					 * JSON to the most appropriate Java type.
					 */
					sourceFilterCollector.add( new JsonPrimitive( projectedField ) );
					return new JsonDrivenProjection( projectedField );
				}
			}
		}

		private FieldProjection createProjection(TypeMetadata rootTypeMetadata,
				DocumentFieldMetadata fieldMetadata) {
			String absoluteName = fieldMetadata.getAbsoluteName();
			FieldBridge fieldBridge = fieldMetadata.getFieldBridge();
			ExtendedFieldType type = FieldHelper.getType( fieldMetadata );

			if ( ExtendedFieldType.BOOLEAN.equals( type ) ) {
				sourceFilterCollector.add( new JsonPrimitive( absoluteName ) );

				return new PrimitiveProjection( rootTypeMetadata, absoluteName, type );
			}
			else if ( fieldBridge instanceof TwoWayFieldBridge ) {
				Collection<BridgeDefinedField> bridgeDefinedFields = fieldMetadata.getBridgeDefinedFields().values();

				Set<String> objectFieldNames = new HashSet<>();
				Map<String, PrimitiveProjection> primitiveProjections = new HashMap<>();

				for ( BridgeDefinedField bridgeDefinedField : bridgeDefinedFields ) {
					String nestedAbsoluteName = bridgeDefinedField.getAbsoluteName();
					ExtendedFieldType nestedType = FieldHelper.getType( bridgeDefinedField );
					if ( ExtendedFieldType.OBJECT.equals( nestedType ) ) {
						objectFieldNames.add( nestedAbsoluteName );
					}
					else {
						PrimitiveProjection projection =
								new PrimitiveProjection( rootTypeMetadata, nestedAbsoluteName, type );
						primitiveProjections.put( nestedAbsoluteName, projection );
					}
					sourceFilterCollector.add( new JsonPrimitive( nestedAbsoluteName ) );
				}

				if ( !objectFieldNames.contains( absoluteName )
						&& !primitiveProjections.containsKey( absoluteName ) ) {
					/*
					 * The default field was not overridden: add it to the projection
					 * just in case we're not dealing with a MetadataProvidingFieldBridge.
					 */
					PrimitiveProjection defaultFieldProjection = new PrimitiveProjection( rootTypeMetadata, absoluteName, type );
					primitiveProjections.put( absoluteName, defaultFieldProjection );
					sourceFilterCollector.add( new JsonPrimitive( absoluteName ) );
				}

				return new TwoWayFieldBridgeProjection(
						absoluteName, (TwoWayFieldBridge) fieldBridge, objectFieldNames, primitiveProjections
						);
			}
			else {
				/*
				 * Don't fail immediately: this entity type may not be present in the results, in which case
				 * we don't need to be able to project on this field for this exact entity type.
				 * Just make sure we *will* ultimately fail if we encounter this entity type.
				 */
				return new FailingOneWayFieldBridgeProjection( absoluteName, fieldBridge.getClass() );
			}
		}
	}

}
