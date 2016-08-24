/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.elasticsearch.ElasticsearchProjectionConstants;
import org.hibernate.search.elasticsearch.client.impl.DistanceSort;
import org.hibernate.search.elasticsearch.client.impl.JestClient;
import org.hibernate.search.elasticsearch.filter.ElasticsearchFilter;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.impl.FilterDef;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.metadata.impl.BridgeDefinedField;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
import org.hibernate.search.engine.metadata.impl.PropertyMetadata;
import org.hibernate.search.engine.service.spi.ServiceReference;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.filter.impl.FullTextFilterImpl;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.metadata.NumericFieldSettingsDescriptor.NumericEncodingType;
import org.hibernate.search.query.dsl.impl.DiscreteFacetRequest;
import org.hibernate.search.query.dsl.impl.FacetRange;
import org.hibernate.search.query.dsl.impl.RangeFacetRequest;
import org.hibernate.search.query.engine.impl.AbstractHSQuery;
import org.hibernate.search.query.engine.impl.EntityInfoImpl;
import org.hibernate.search.query.engine.impl.FacetComparators;
import org.hibernate.search.query.engine.impl.FacetManagerImpl;
import org.hibernate.search.query.engine.impl.TimeoutManagerImpl;
import org.hibernate.search.query.engine.spi.DocumentExtractor;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetSortOrder;
import org.hibernate.search.query.facet.FacetingRequest;
import org.hibernate.search.spatial.DistanceSortField;
import org.hibernate.search.util.impl.CollectionHelper;
import org.hibernate.search.util.impl.ReflectionHelper;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import io.searchbox.core.DocumentResult;
import io.searchbox.core.Explain;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.sort.Sort;
import io.searchbox.core.search.sort.Sort.Sorting;

/**
 * Query implementation based on Elasticsearch.
 *
 * @author Gunnar Morling
 */
public class ElasticsearchHSQueryImpl extends AbstractHSQuery {

	private static final JsonParser JSON_PARSER = new JsonParser();

	private static final Log LOG = LoggerFactory.make( Log.class );

	private static final Pattern DOT = Pattern.compile( "\\." );

	private static final String SPATIAL_DISTANCE_FIELD = "_distance";

	/**
	 * ES default limit for (firstResult + maxResult)
	 */
	private static final int MAX_RESULT_WINDOW_SIZE = 10000;

	private static final Set<String> SUPPORTED_PROJECTION_CONSTANTS = Collections.unmodifiableSet(
			CollectionHelper.asSet(
					ElasticsearchProjectionConstants.ID,
					ElasticsearchProjectionConstants.OBJECT_CLASS,
					ElasticsearchProjectionConstants.SCORE,
					ElasticsearchProjectionConstants.SOURCE,
					ElasticsearchProjectionConstants.SPATIAL_DISTANCE,
					ElasticsearchProjectionConstants.THIS,
					ElasticsearchProjectionConstants.TOOK,
					ElasticsearchProjectionConstants.TIMED_OUT
			)
	);

	private final JsonObject jsonQuery;

	private Integer resultSize;
	private IndexSearcher searcher;
	private SearchResult searchResult;

	private int sortByDistanceIndex = -1;

	private transient FacetManagerImpl facetManager;

	public ElasticsearchHSQueryImpl(JsonObject jsonQuery, ExtendedSearchIntegrator extendedIntegrator) {
		super( extendedIntegrator );
		this.jsonQuery = jsonQuery;
	}

	@Override
	public HSQuery luceneQuery(Query query) {
		throw LOG.hsQueryLuceneQueryUnsupported();
	}

	@Override
	public FacetManagerImpl getFacetManager() {
		if ( facetManager == null ) {
			facetManager = new FacetManagerImpl( this );
		}
		return facetManager;
	}

	@Override
	public Query getLuceneQuery() {
		throw LOG.hsQueryLuceneQueryUnsupported();
	}

	@Override
	public DocumentExtractor queryDocumentExtractor() {
		return new ElasticsearchDocumentExtractor();
	}

	SearchResult getSearchResult() {
		if ( searchResult == null ) {
			execute();
		}
		return searchResult;
	}

	@Override
	public int queryResultSize() {
		if ( searchResult == null ) {
			execute();
		}
		return resultSize;
	}

	@Override
	public Explanation explain(int documentId) {
		if ( searchResult == null ) {
			execute();
		}

		JsonObject hit = searchResult.getJsonObject()
				.get( "hits" )
				.getAsJsonObject()
				.get( "hits" )
				.getAsJsonArray()
				// TODO Is it right to use the document id that way? I am not quite clear about its semantics
				.get( documentId )
				.getAsJsonObject();

		try ( ServiceReference<JestClient> client = getExtendedSearchIntegrator().getServiceManager().requestReference( JestClient.class ) ) {
			Explain request = new Explain.Builder(
					hit.get( "_index" ).getAsString(),
					hit.get( "_type" ).getAsString(),
					hit.get( "_id" ).getAsString(),
					searcher.executedQuery
				)
				.build();

			DocumentResult response = client.get().executeRequest( request );
			JsonObject explanation = response.getJsonObject().get( "explanation" ).getAsJsonObject();

			return convertExplanation( explanation );
		}
	}

	private Explanation convertExplanation(JsonObject explanation) {
		float value = explanation.get( "value" ).getAsFloat();
		String description = explanation.get( "description" ).getAsString();
		JsonElement explanationDetails = explanation.get( "details" );

		List<Explanation> details;

		if ( explanationDetails != null ) {
			details = new ArrayList<>( explanationDetails.getAsJsonArray().size() );

			for ( JsonElement detail : explanationDetails.getAsJsonArray() ) {
				details.add( convertExplanation( detail.getAsJsonObject() ) );
			}
		}
		else {
			details = Collections.emptyList();
		}

		return Explanation.match( value, description, details );
	}

	@Override
	protected void clearCachedResults() {
		searchResult = null;
		resultSize = null;
	}

	@Override
	protected TimeoutManagerImpl buildTimeoutManager() {
		return new TimeoutManagerImpl(
				jsonQuery,
				timeoutExceptionFactory,
				this.extendedIntegrator.getTimingSource()
		);
	}

	@Override
	public List<EntityInfo> queryEntityInfos() {
		if ( searchResult == null ) {
			execute();
		}

		List<EntityInfo> results = new ArrayList<>( searchResult.getTotal() );
		JsonObject searchResultJsonObject = searchResult.getJsonObject();
		JsonArray hits = searchResultJsonObject.get( "hits" ).getAsJsonObject().get( "hits" ).getAsJsonArray();

		for ( JsonElement hit : hits ) {
			EntityInfo entityInfo = searcher.convertQueryHit( searchResultJsonObject, hit.getAsJsonObject() );
			if ( entityInfo != null ) {
				results.add( entityInfo );
			}
		}

		return results;
	}

	@Override
	protected Set<String> getSupportedProjectionConstants() {
		return SUPPORTED_PROJECTION_CONSTANTS;
	}

	private void execute() {
		searcher = new IndexSearcher();

		searchResult = searcher.runSearch();
		resultSize = searchResult.getTotal();
	}

	/**
	 * Determines the affected indexes and runs the given query against them.
	 */
	private class IndexSearcher {

		private final Search search;
		private final Map<String, Class<?>> entityTypesByName;
		private final String executedQuery;

		private IndexSearcher() {
			entityTypesByName = new HashMap<>();
			String idFieldName = null;
			JsonArray typeFilters = new JsonArray();
			Set<String> indexNames = new HashSet<>();
			Iterable<Class<?>> queriedEntityTypes = getQueriedEntityTypes();

			for ( Class<?> queriedEntityType : queriedEntityTypes ) {
				entityTypesByName.put( queriedEntityType.getName(), queriedEntityType );

				EntityIndexBinding binding = extendedIntegrator.getIndexBinding( queriedEntityType );
				IndexManager[] indexManagers = binding.getIndexManagers();

				for ( IndexManager indexManager : indexManagers ) {
					if ( !( indexManager instanceof ElasticsearchIndexManager ) ) {
						throw LOG.cannotRunEsQueryTargetingEntityIndexedWithNonEsIndexManager(
							queriedEntityType,
							jsonQuery.toString()
						);
					}

					// TODO HSEARCH-2253 the id field name is used to detect if we should sort using the internal Elasticsearch id field
					// as it's currently the only field in which we store the id of the entity.
					// Thus the id fields must be consistent accross all the entity types when querying multiple ones.
					idFieldName = binding.getDocumentBuilder().getIdentifierName();

					ElasticsearchIndexManager esIndexManager = (ElasticsearchIndexManager) indexManager;
					indexNames.add( esIndexManager.getActualIndexName() );
				}

				typeFilters.add( getEntityTypeFilter( queriedEntityType ) );
			}

			// Query filters; always a type filter, possibly a tenant id filter;
			JsonObject effectiveFilter = getEffectiveFilter( typeFilters );

			JsonBuilder.Object completeQuery = JsonBuilder.object();

			completeQuery.add( "query",
					JsonBuilder.object()
							.add( "filtered", JsonBuilder.object( jsonQuery ).add( "filter", effectiveFilter ) ) );

			if ( !getFacetManager().getFacetRequests().isEmpty() ) {
				JsonBuilder.Object facets = JsonBuilder.object();

				for ( Entry<String, FacetingRequest> facetRequestEntry : getFacetManager().getFacetRequests().entrySet() ) {
					ToElasticsearch.addFacetingRequest( facets, facetRequestEntry.getValue() );
				}

				completeQuery.add( "aggregations", facets );
			}

			// Initialize the sortByDistanceIndex to detect if the results are sorted by distance and the position
			// of the sort
			sortByDistanceIndex = getSortByDistanceIndex();
			addScriptFields( completeQuery );

			executedQuery = completeQuery.build().toString();

			Search.Builder search = new Search.Builder( executedQuery );
			search.addIndex( indexNames );
			search.setParameter( "from", firstResult );

			// If the user has given a value, take it as is, let ES itself complain if it's too high; if no value is
			// given, I take as much as possible, as by default only 10 rows would be returned
			search.setParameter( "size", maxResults != null ? maxResults : MAX_RESULT_WINDOW_SIZE - firstResult );

			// TODO: HSEARCH-2254 embedded fields (see https://github.com/searchbox-io/Jest/issues/304)
			if ( sort != null ) {
				validateSortFields( extendedIntegrator, queriedEntityTypes );
				for ( SortField sortField : sort.getSort() ) {
					search.addSort( getSort( sortField, idFieldName ) );
				}
			}
			this.search = search.build();
		}

		private JsonObject getEffectiveFilter(JsonArray typeFilters) {
			JsonArray filters = new JsonArray();

			JsonObject tenantFilter = getTenantIdFilter();
			if ( tenantFilter != null ) {
				filters.add( tenantFilter );
			}

			// wrap type filters into should if there is more than one
			filters.add( ToElasticsearch.condition( "should", typeFilters ) );

			// facet filters
			for ( Query query : getFacetManager().getFacetFilters().getFilterQueries() ) {
				filters.add( ToElasticsearch.fromLuceneQuery( query ) );
			}

			// user filter
			if ( userFilter != null ) {
				filters.add( ToElasticsearch.fromLuceneFilter( userFilter ) );
			}

			if ( !filterDefinitions.isEmpty() ) {
				for ( FullTextFilterImpl fullTextFilter : filterDefinitions.values() ) {
					JsonObject filter = buildFullTextFilter( fullTextFilter );
					if ( filter != null ) {
						filters.add( filter );
					}
				}
			}

			// wrap filters into must if there is more than one
			return ToElasticsearch.condition( "must", filters );
		}

		private JsonObject getEntityTypeFilter(Class<?> queriedEntityType) {
			JsonObject value = new JsonObject();
			value.addProperty( "value", queriedEntityType.getName() );

			JsonObject type = new JsonObject();
			type.add( "type", value );

			return type;
		}

		private JsonObject getTenantIdFilter() {
			if ( tenantId == null ) {
				return null;
			}

			JsonObject value = new JsonObject();
			value.addProperty( DocumentBuilderIndexedEntity.TENANT_ID_FIELDNAME, tenantId );

			JsonObject tenantFilter = new JsonObject();
			tenantFilter.add( "term", value );

			return tenantFilter;
		}

		private Iterable<Class<?>> getQueriedEntityTypes() {
			if ( indexedTargetedEntities == null || indexedTargetedEntities.isEmpty() ) {
				return extendedIntegrator.getIndexBindings().keySet();
			}
			else {
				return indexedTargetedEntities;
			}
		}

		private Sort getSort(SortField sortField, String idFieldName) {
			if ( sortField instanceof DistanceSortField ) {
				DistanceSortField distanceSortField = (DistanceSortField) sortField;
				return new DistanceSort( distanceSortField.getField(),
						distanceSortField.getCenter(),
						distanceSortField.getReverse() ? Sorting.DESC : Sorting.ASC );
			}
			else {
				String sortFieldName;
				if ( sortField.getField() == null ) {
					switch (sortField.getType()) {
						case DOC:
							sortFieldName = "_uid";
							break;
						case SCORE:
							sortFieldName = "_score";
							break;
						default:
							throw LOG.cannotUseThisSortTypeWithNullSortFieldName( sortField.getType() );
					}
				}
				else {
					if ( sortField.getField().equals( idFieldName ) ) {
						// It is not possible to sort on the _id field as this field is not indexed by Elasticsearch and there
						// is no way to make it indexed in recent Elasticsearch versions.
						// Thus, we use _uid which is stored as type#id even if it is not very satisfactory when
						// we query multiple types.
						// It is not recommended to sort by the @DocumentId field anyway.
						sortFieldName = "_uid";
					}
					else {
						sortFieldName = sortField.getField();
					}
				}
				return new Sort( sortFieldName, sortField.getReverse() ? Sorting.DESC : Sorting.ASC );
			}
		}

		/**
		 * Returns the index of the DistanceSortField in the Sort array.
		 *
		 * @return the index, -1 if no DistanceSortField has been found
		 */
		private int getSortByDistanceIndex() {
			int i = 0;
			if ( sort != null ) {
				for ( SortField sortField : sort.getSort() ) {
					if ( sortField instanceof DistanceSortField ) {
						return i;
					}
					i++;
				}
			}
			return -1;
		}

		/**
		 * Indicates if the results are sorted by distance (note that it might be a secondary order).
		 */
		private boolean isSortedByDistance() {
			return sortByDistanceIndex >= 0;
		}

		private void addScriptFields(JsonBuilder.Object query) {
			if ( isPartOfProjectedFields( ElasticsearchProjectionConstants.SPATIAL_DISTANCE ) && !isSortedByDistance() ) {
				// when the results are sorted by distance, Elasticsearch returns the distance in a "sort" field in
				// the results. If we don't sort by distance, we need to request for the distance using a script_field.
				query.add( "script_fields",
						JsonBuilder.object().add( SPATIAL_DISTANCE_FIELD, JsonBuilder.object()
								.add( "params",
										JsonBuilder.object()
												.addProperty( "lat", spatialSearchCenter.getLatitude() )
												.addProperty( "lon", spatialSearchCenter.getLongitude() )
								)
								.addProperty( "script", "doc[\u0027" + spatialFieldName + "\u0027].arcDistanceInKm(lat,lon)" )
						)
				);
				// in this case, the _source field is not present in the Elasticsearch results
				// we need to ask for it explicitely
				query.add( "fields", JsonBuilder.array().add( new JsonPrimitive( "_source" ) ) );
			}
		}

		SearchResult runSearch() {
			try ( ServiceReference<JestClient> client = getExtendedSearchIntegrator().getServiceManager().requestReference( JestClient.class ) ) {
				return client.get().executeRequest( search );
			}
		}

		EntityInfo convertQueryHit(JsonObject searchResult, JsonObject hit) {
			String type = hit.get( "_type" ).getAsString();
			Class<?> clazz = entityTypesByName.get( type );

			if ( clazz == null ) {
				LOG.warnf( "Found unknown type in Elasticsearch index: " + type );
				return null;
			}

			EntityIndexBinding binding = extendedIntegrator.getIndexBinding( clazz );
			Object id = getId( hit, binding );
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
							projections[i] = clazz;
							break;
						case ElasticsearchProjectionConstants.SCORE:
							projections[i] = hit.getAsJsonObject().get( "_score" ).getAsFloat();
							break;
						case ElasticsearchProjectionConstants.SPATIAL_DISTANCE:
							// if we sort by distance, we need to find the index of the DistanceSortField and use it
							// to extract the values from the sort array
							// if we don't sort by distance, we use the field generated by the script_field added earlier
							if ( isSortedByDistance() ) {
								projections[i] = hit.getAsJsonObject().get( "sort" ).getAsJsonArray().get( sortByDistanceIndex ).getAsDouble();
							}
							else {
								projections[i] = hit.getAsJsonObject().get( "fields" ).getAsJsonObject().get( SPATIAL_DISTANCE_FIELD ).getAsDouble();
							}
							break;
						case ElasticsearchProjectionConstants.TOOK:
							projections[i] = searchResult.get( "took" ).getAsInt();
							break;
						case ElasticsearchProjectionConstants.TIMED_OUT:
							projections[i] = searchResult.get( "timed_out" ).getAsBoolean();
							break;
						case ElasticsearchProjectionConstants.THIS:
							// Use EntityInfo.ENTITY_PLACEHOLDER as placeholder.
							// It will be replaced when we populate
							// the EntityInfo with the real entity.
							projections[i] = EntityInfo.ENTITY_PLACEHOLDER;
							break;
						default:
							projections[i] = getFieldValue( binding, hit, field );
					}
				}
			}

			return new EntityInfoImpl( clazz, binding.getDocumentBuilder().getIdentifierName(), (Serializable) id, projections );
		}

		private Object getId(JsonObject hit, EntityIndexBinding binding) {
			Document tmp = new Document();
			tmp.add( new StringField( "id", DocumentIdHelper.getEntityId( hit.get( "_id" ).getAsString() ), Store.NO) );

			addIdBridgeDefinedFields( hit, binding, tmp );

			return binding.getDocumentBuilder().getIdBridge().get( "id", tmp );
		}

		// Add to the document the additional fields created when indexing the id
		private void addIdBridgeDefinedFields(JsonObject hit, EntityIndexBinding binding, Document tmp) {
			Set<BridgeDefinedField> allBridgeDefinedFields = new HashSet<>();
			allBridgeDefinedFields.addAll( binding.getDocumentBuilder().getMetadata().getIdPropertyMetadata().getBridgeDefinedFields().values() );
			for ( BridgeDefinedField bridgeDefinedField : allBridgeDefinedFields ) {
				Object fieldValue = getFieldValue( binding, hit, bridgeDefinedField.getName() );
				tmp.add( new StringField( bridgeDefinedField.getName(), String.valueOf( fieldValue ), Store.NO ) );
			}
		}

		/**
		 * Returns the value of the given field as retrieved from the ES result and converted using the corresponding
		 * field bridge. In case this bridge is not a 2-way bridge, the unconverted value will be returned.
		 */
		private Object getFieldValue(EntityIndexBinding binding, JsonObject hit, String projectedField) {
			DocumentFieldMetadata field = FieldHelper.getFieldMetadata( binding, projectedField );

			if ( field == null ) {
				// We check if it is a field created by a field bridge
				if ( !isBridgeDefinedField( binding, projectedField ) ) {
					throw LOG.unknownFieldForProjection(
							binding.getDocumentBuilder().getMetadata().getType().getName(),
							projectedField );
				}
			}

			JsonElement value;

			if ( field != null && field.isId() ) {
				value = hit.get( "_id" );
			}
			else {
				value = getFieldValue( hit.get( "_source" ).getAsJsonObject(), projectedField );
			}

			if ( value == null || value.isJsonNull() ) {
				return null;
			}

			if ( field != null ) {
				return convertFieldValue( binding, field, value );
			}
			else {
				return convertPrimitiveValue( value );
			}
		}

		private boolean isBridgeDefinedField(EntityIndexBinding binding, String projectedField) {
			BridgeDefinedField bridgeDefinedField = binding.getDocumentBuilder().getMetadata().getIdPropertyMetadata().getBridgeDefinedFields().get( projectedField );
			if ( bridgeDefinedField != null ) {
				return true;
			}
			Set<PropertyMetadata> allPropertyMetadata = binding.getDocumentBuilder().getMetadata().getAllPropertyMetadata();
			for ( PropertyMetadata propertyMetadata : allPropertyMetadata ) {
				bridgeDefinedField = propertyMetadata.getBridgeDefinedFields().get( projectedField );
				if ( bridgeDefinedField != null && bridgeDefinedField.getName().equals( projectedField ) ) {
					return true;
				}
			}
			return false;
		}

		private Object convertFieldValue(EntityIndexBinding binding, DocumentFieldMetadata field, JsonElement value) {
			FieldBridge fieldBridge = field.getFieldBridge();

			if ( FieldHelper.isBoolean( binding, field.getName() ) ) {
				return value.getAsBoolean();
			}
			else if ( fieldBridge instanceof TwoWayFieldBridge ) {
				Document tmp = new Document();

				if ( FieldHelper.isNumeric( field ) ) {
					NumericEncodingType numericEncodingType = FieldHelper.getNumericEncodingType( binding, field );

					switch ( numericEncodingType ) {
						case INTEGER:
							tmp.add( new IntField( field.getName(), value.getAsInt(), Store.NO ) );
							break;
						case LONG:
							tmp.add( new LongField( field.getName(), value.getAsLong(), Store.NO ) );
							break;
						case FLOAT:
							tmp.add( new FloatField( field.getName(), value.getAsFloat(), Store.NO ) );
							break;
						case DOUBLE:
							tmp.add( new DoubleField( field.getName(), value.getAsDouble(), Store.NO ) );
							break;
						case UNKNOWN:
						default:
							throw LOG.unexpectedNumericEncodingType(
									binding.getDocumentBuilder().getMetadata().getType().getName(),
									field.getName() );
					}
				}
				else {
					tmp.add( new StringField( field.getName(), value.getAsString(), Store.NO ) );
				}

				return ( (TwoWayFieldBridge) fieldBridge ).get( field.getName(), tmp );
			}
			// Should only be the case for custom bridges
			else {
				return convertPrimitiveValue( value );
			}
		}

		private Object convertPrimitiveValue(JsonElement value) {
			// TODO: HSEARCH-2255 should we do it?
			if ( !value.isJsonPrimitive() ) {
				throw LOG.unsupportedProjectionOfNonJsonPrimitiveFields( value );
			}

			JsonPrimitive primitive = value.getAsJsonPrimitive();

			if ( primitive.isBoolean() ) {
				return primitive.getAsBoolean();
			}
			else if ( primitive.isNumber() ) {
				// TODO HSEARCH-2255 this will expose a Gson-specific Number implementation; Can we somehow return an Integer,
				// Long... etc. instead?
				return primitive.getAsNumber();
			}
			else if ( primitive.isString() ) {
				return primitive.getAsString();
			}
			else {
				// TODO HSEARCH-2255 Better raise an exception?
				return primitive.toString();
			}
		}

		private JsonElement getFieldValue(JsonObject parent, String projectedField) {
			String field = projectedField;

			if ( FieldHelper.isEmbeddedField( projectedField ) ) {
				String[] parts = DOT.split( projectedField );
				field = parts[parts.length - 1];

				for ( int i = 0; i < parts.length - 1; i++ ) {
					JsonElement newParent = parent.get( parts[i] );
					if ( newParent == null ) {
						return null;
					}

					parent = newParent.getAsJsonObject();
				}
			}

			return parent.getAsJsonObject().get( field );
		}
	}

	@Override
	protected void extractFacetResults() {
		SearchResult searchResult = getSearchResult();
		JsonElement aggregationsElement = searchResult.getJsonObject().get( "aggregations" );
		if ( aggregationsElement == null ) {
			return;
		}
		JsonObject aggregations = aggregationsElement.getAsJsonObject();

		Map<String, List<Facet>> results = new HashMap<>();
		for ( FacetingRequest facetRequest : getFacetManager().getFacetRequests().values() ) {
			List<Facet> facets;
			if ( facetRequest instanceof DiscreteFacetRequest ) {
				facets = updateStringFacets( aggregations, (DiscreteFacetRequest) facetRequest );
				// Discrete facets are sorted by Elasticsearch
			}
			else {
				facets = updateRangeFacets( aggregations, (RangeFacetRequest<?>) facetRequest );
				if ( !FacetSortOrder.RANGE_DEFINITION_ORDER.equals( facetRequest.getSort() ) ) {
					Collections.sort( facets, FacetComparators.get( facetRequest.getSort() ) );
				}
			}

			results.put( facetRequest.getFacetingName(), facets );
		}
		getFacetManager().setFacetResults( results );
	}

	private List<Facet> updateRangeFacets(JsonObject aggregations, RangeFacetRequest<?> facetRequest) {
		if ( !ReflectionHelper.isIntegerType( facetRequest.getFacetValueType() )
				&& !Date.class.isAssignableFrom( facetRequest.getFacetValueType() )
				&& !ReflectionHelper.isFloatingPointType( facetRequest.getFacetValueType() ) ) {
			throw LOG.unsupportedFacetRangeParameter( facetRequest.getFacetValueType().getName() );
		}

		ArrayList<Facet> facets = new ArrayList<>();
		for ( FacetRange<?> facetRange : facetRequest.getFacetRangeList() ) {
			JsonElement aggregation = aggregations.get( facetRequest.getFacetingName() + "-" + facetRange.getIdentifier() );
			if ( aggregation == null ) {
				continue;
			}
			int docCount = aggregation.getAsJsonObject().get( "doc_count" ).getAsInt();
			if ( docCount == 0 && !facetRequest.hasZeroCountsIncluded() ) {
				continue;
			}
			facets.add( facetRequest.createFacet( facetRange.getRangeString(), docCount ) );
		}
		return facets;
	}

	private List<Facet> updateStringFacets(JsonObject aggregations, DiscreteFacetRequest facetRequest) {
		JsonElement aggregation = aggregations.get( facetRequest.getFacetingName() );
		if ( aggregation == null ) {
			return Collections.emptyList();
		}

		// deal with nested aggregation for nested documents
		if ( isNested( facetRequest ) ) {
			aggregation = aggregation.getAsJsonObject().get( facetRequest.getFacetingName() );
		}
		if ( aggregation == null ) {
			return Collections.emptyList();
		}

		ArrayList<Facet> facets = new ArrayList<>();
		for ( JsonElement bucket : aggregation.getAsJsonObject().get( "buckets" ).getAsJsonArray() ) {
			facets.add( facetRequest.createFacet(
					bucket.getAsJsonObject().get( "key" ).getAsString(),
					bucket.getAsJsonObject().get( "doc_count" ).getAsInt() ) );
		}
		return facets;
	}

	private JsonObject buildFullTextFilter(FullTextFilterImpl fullTextFilter) {

		/*
		 * FilterKey implementations and Filter(Factory) do not have to be threadsafe wrt their parameter injection
		 * as FilterCachingStrategy ensure a memory barrier between concurrent thread calls
		 */
		FilterDef def = extendedIntegrator.getFilterDefinition( fullTextFilter.getName() );
		//def can never be null, it's guarded by enableFullTextFilter(String)

		if ( isPreQueryFilterOnly( def ) ) {
			return null;
		}

		Object filterOrFactory = createFilterInstance( fullTextFilter, def );
		return createFullTextFilter( def, filterOrFactory );
	}

	protected JsonObject createFullTextFilter(FilterDef def, Object filterOrFactory) {
		JsonObject jsonFilter;
		if ( def.getFactoryMethod() != null ) {
			try {
				Object candidateFilter = def.getFactoryMethod().invoke( filterOrFactory );
				if ( candidateFilter instanceof Filter ) {
					jsonFilter = ToElasticsearch.fromLuceneFilter( (Filter) candidateFilter );
				}
				else if ( candidateFilter instanceof ElasticsearchFilter ) {
					jsonFilter = JSON_PARSER.parse( ( (ElasticsearchFilter) candidateFilter ).getJsonFilter() )
							.getAsJsonObject();
				}
				else {
					throw LOG.filterFactoryMethodReturnsUnsupportedType( def.getImpl().getName(), def.getFactoryMethod().getName() );
				}
			}
			catch (IllegalAccessException | InvocationTargetException e) {
				throw LOG.filterFactoryMethodInaccessible( def.getImpl().getName(), def.getFactoryMethod().getName(), e );
			}
		}
		else {
			if ( filterOrFactory instanceof Filter ) {
				jsonFilter = ToElasticsearch.fromLuceneFilter( (Filter) filterOrFactory );
			}
			else if ( filterOrFactory instanceof ElasticsearchFilter ) {
				jsonFilter = JSON_PARSER.parse( ( (ElasticsearchFilter) filterOrFactory ).getJsonFilter() ).getAsJsonObject();
			}
			else {
				throw LOG.filterHasUnsupportedType( filterOrFactory == null ? null : filterOrFactory.getClass().getName() );
			}
		}

		return jsonFilter;
	}

	private boolean isNested(DiscreteFacetRequest facetRequest) {
		//TODO HSEARCH-2097 Drive through meta-data
//		return FieldHelper.isEmbeddedField( facetRequest.getFieldName() );
		return false;
	}

	private boolean isPartOfProjectedFields(String projectionName) {
		if ( projectedFields == null ) {
			return false;
		}
		for ( String projectedField : projectedFields ) {
			if ( projectionName.equals( projectedField ) ) {
				return true;
			}
		}
		return false;
	}

	// TODO: HSEARCH-2189  Investigate scrolling API:
	// https://www.elastic.co/guide/en/elasticsearch/reference/current/search-request-scroll.html
	private class ElasticsearchDocumentExtractor implements DocumentExtractor {

		private final IndexSearcher searcher;
		private List<EntityInfo> results;

		private ElasticsearchDocumentExtractor() {
			searcher = new IndexSearcher();
		}

		@Override
		public EntityInfo extract(int index) throws IOException {
			if ( results == null ) {
				runSearch();
			}

			return results.get( index );
		}

		@Override
		public int getFirstIndex() {
			return 0;
		}

		@Override
		public int getMaxIndex() {
			if ( results == null ) {
				runSearch();
			}

			return results.size() - 1;
		}

		@Override
		public void close() {
		}

		@Override
		public TopDocs getTopDocs() {
			throw LOG.documentExtractorTopDocsUnsupported();
		}

		private void runSearch() {
			SearchResult searchResult = searcher.runSearch();
			JsonObject searchResultJsonObject = searchResult.getJsonObject();
			JsonArray hits = searchResultJsonObject.get( "hits" ).getAsJsonObject().get( "hits" ).getAsJsonArray();
			results = new ArrayList<>( searchResult.getTotal() );

			for ( JsonElement hit : hits ) {
				EntityInfo converted = searcher.convertQueryHit( searchResultJsonObject, hit.getAsJsonObject() );
				if ( converted != null ) {
					results.add( converted );
				}
			}
		}
	}
}
